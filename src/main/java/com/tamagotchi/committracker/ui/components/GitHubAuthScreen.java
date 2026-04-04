package com.tamagotchi.committracker.ui.components;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import com.tamagotchi.committracker.github.DeviceCodeResponse;
import com.tamagotchi.committracker.github.GitHubOAuthService;
import com.tamagotchi.committracker.github.AccessTokenResponse;
import com.tamagotchi.committracker.ui.theme.PokedexTheme;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * GitHub authentication screen for the Pokedex UI.
 * Displays device code and verification URL for GitHub OAuth Device Flow.
 * Shows QR code for easy mobile access and authentication status/progress.
 * 
 * Requirements: 1.1, 1.5
 */
public class GitHubAuthScreen extends VBox {
    
    // Authentication state
    public enum AuthState {
        INITIALIZING,
        WAITING_FOR_USER,
        POLLING,
        SUCCESS,
        ERROR,
        CANCELLED
    }
    
    // UI Components
    private Label titleLabel;
    private Label instructionLabel;
    private Label userCodeLabel;
    private Label urlLabel;
    private ImageView qrCodeView;
    private ProgressIndicator progressIndicator;
    private Label statusLabel;
    private HBox skipButton;
    
    // State
    private AuthState currentState = AuthState.INITIALIZING;
    private DeviceCodeResponse deviceCodeResponse;
    private String errorMessage;
    
    // Callbacks
    private Consumer<AccessTokenResponse> onAuthSuccess;
    private Runnable onAuthSkipped;
    private Runnable onAuthError;
    
    // OAuth service reference
    private GitHubOAuthService oauthService;
    private CompletableFuture<?> currentOperation;
    
    // QR Code dimensions
    private static final int QR_CODE_SIZE = 80;
    private static final int QR_MODULE_SIZE = 2;
    
    /**
     * Creates a new GitHub authentication screen.
     * 
     * @param oauthService The OAuth service to use for authentication
     * @param onSuccess Callback when authentication succeeds
     * @param onSkipped Callback when user skips authentication
     */
    public GitHubAuthScreen(GitHubOAuthService oauthService, 
                           Consumer<AccessTokenResponse> onSuccess,
                           Runnable onSkipped) {
        this.oauthService = oauthService;
        this.onAuthSuccess = onSuccess;
        this.onAuthSkipped = onSkipped;
        
        initializeLayout();
        createComponents();
    }
    
    /**
     * Initializes the layout properties.
     */
    private void initializeLayout() {
        setAlignment(Pos.CENTER);
        setPadding(new Insets(8));
        setSpacing(6);
        setStyle("-fx-background-color: transparent;");
    }
    
    /**
     * Creates all UI components.
     */
    private void createComponents() {
        // Title
        titleLabel = new Label("GitHub Login");
        titleLabel.setStyle(PokedexTheme.combineStyles(
            PokedexTheme.getPixelFontStyle(10),
            PokedexTheme.getTextStyle(PokedexTheme.TEXT_WHITE)
        ));
        
        // Instruction label
        instructionLabel = new Label("Visit URL & enter code:");
        instructionLabel.setStyle(PokedexTheme.combineStyles(
            PokedexTheme.getPixelFontStyle(7),
            PokedexTheme.getTextStyle(PokedexTheme.TEXT_WHITE)
        ));
        instructionLabel.setWrapText(true);
        
        // User code display (large, prominent)
        userCodeLabel = new Label("--------");
        userCodeLabel.setStyle(PokedexTheme.combineStyles(
            PokedexTheme.getPixelFontStyle(14),
            PokedexTheme.getTextStyle("#4ADE80"), // Green for visibility
            "-fx-background-color: rgba(0,0,0,0.5); -fx-padding: 6 10; -fx-background-radius: 4;"
        ));
        
        // URL label
        urlLabel = new Label("github.com/login/device");
        urlLabel.setStyle(PokedexTheme.combineStyles(
            PokedexTheme.getPixelFontStyle(7),
            PokedexTheme.getTextStyle("#60A5FA") // Blue for link
        ));
        
        // QR Code placeholder
        qrCodeView = new ImageView();
        qrCodeView.setFitWidth(QR_CODE_SIZE);
        qrCodeView.setFitHeight(QR_CODE_SIZE);
        qrCodeView.setPreserveRatio(true);
        
        // Wrap QR code in a styled container
        StackPane qrContainer = new StackPane(qrCodeView);
        qrContainer.setStyle("-fx-background-color: white; -fx-padding: 4; -fx-background-radius: 4;");
        qrContainer.setMaxSize(QR_CODE_SIZE + 8, QR_CODE_SIZE + 8);
        
        // Progress indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(20, 20);
        progressIndicator.setStyle("-fx-progress-color: #4ADE80;");
        
        // Status label
        statusLabel = new Label("Initializing...");
        statusLabel.setStyle(PokedexTheme.combineStyles(
            PokedexTheme.getPixelFontStyle(7),
            PokedexTheme.getTextStyle(PokedexTheme.TEXT_WHITE)
        ));
        statusLabel.setWrapText(true);
        
        // Status row with progress indicator
        HBox statusRow = new HBox(6);
        statusRow.setAlignment(Pos.CENTER);
        statusRow.getChildren().addAll(progressIndicator, statusLabel);
        
        // Skip button
        skipButton = createSkipButton();
        
        // Add all components
        getChildren().addAll(
            titleLabel,
            instructionLabel,
            userCodeLabel,
            urlLabel,
            qrContainer,
            statusRow,
            skipButton
        );
    }
    
    /**
     * Creates the skip button for users who want to skip GitHub auth.
     */
    private HBox createSkipButton() {
        Label skipLabel = new Label("Use local Git only");
        skipLabel.setStyle(PokedexTheme.combineStyles(
            PokedexTheme.getPixelFontStyle(7),
            PokedexTheme.getTextStyle("#9CA3AF")
        ));
        
        HBox button = new HBox(skipLabel);
        button.setAlignment(Pos.CENTER);
        button.setStyle("-fx-cursor: hand; -fx-padding: 4 8; -fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 4;");
        
        button.setOnMouseEntered(e -> {
            skipLabel.setStyle(PokedexTheme.combineStyles(
                PokedexTheme.getPixelFontStyle(7),
                PokedexTheme.getTextStyle(PokedexTheme.TEXT_WHITE)
            ));
            button.setStyle("-fx-cursor: hand; -fx-padding: 4 8; -fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 4;");
        });
        
        button.setOnMouseExited(e -> {
            skipLabel.setStyle(PokedexTheme.combineStyles(
                PokedexTheme.getPixelFontStyle(7),
                PokedexTheme.getTextStyle("#9CA3AF")
            ));
            button.setStyle("-fx-cursor: hand; -fx-padding: 4 8; -fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 4;");
        });
        
        button.setOnMouseClicked(e -> {
            cancelAuthentication();
            if (onAuthSkipped != null) {
                onAuthSkipped.run();
            }
        });
        
        return button;
    }

    
    /**
     * Starts the authentication flow.
     * Initiates the device flow and begins polling for user authorization.
     */
    public void startAuthentication() {
        if (oauthService == null) {
            showError("OAuth service not configured");
            return;
        }
        
        updateState(AuthState.INITIALIZING);
        
        currentOperation = oauthService.initiateDeviceFlow()
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    this.deviceCodeResponse = response;
                    displayDeviceCode(response);
                    updateState(AuthState.WAITING_FOR_USER);
                    startPolling(response.deviceCode());
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    showError("Failed to start auth: " + getShortErrorMessage(ex));
                });
                return null;
            });
    }
    
    /**
     * Displays the device code and verification URL.
     */
    private void displayDeviceCode(DeviceCodeResponse response) {
        userCodeLabel.setText(response.userCode());
        urlLabel.setText(response.verificationUri());
        
        // Generate QR code for the verification URL
        generateQRCode(response.verificationUri() + "?user_code=" + response.userCode());
    }
    
    /**
     * Generates a simple QR code image for the given URL.
     * Uses a basic QR code generation algorithm.
     */
    private void generateQRCode(String url) {
        // Create a simple placeholder QR code pattern
        // In production, use a proper QR code library like ZXing
        WritableImage qrImage = createSimpleQRPlaceholder(url);
        qrCodeView.setImage(qrImage);
    }
    
    /**
     * Creates a simple QR-like placeholder image.
     * This is a visual placeholder - for production, use ZXing or similar.
     */
    private WritableImage createSimpleQRPlaceholder(String data) {
        int size = QR_CODE_SIZE;
        WritableImage image = new WritableImage(size, size);
        var writer = image.getPixelWriter();
        
        // Create a simple pattern based on data hash
        int hash = data.hashCode();
        
        // Fill with white background
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                writer.setColor(x, y, Color.WHITE);
            }
        }
        
        // Draw finder patterns (corners)
        drawFinderPattern(writer, 4, 4, 14);
        drawFinderPattern(writer, size - 18, 4, 14);
        drawFinderPattern(writer, 4, size - 18, 14);
        
        // Draw data pattern based on hash
        int moduleSize = 4;
        int dataArea = size - 36;
        int startX = 20;
        int startY = 20;
        
        for (int row = 0; row < dataArea / moduleSize; row++) {
            for (int col = 0; col < dataArea / moduleSize; col++) {
                int bit = (hash >> ((row * 7 + col) % 32)) & 1;
                if (bit == 1) {
                    fillModule(writer, startX + col * moduleSize, startY + row * moduleSize, moduleSize, Color.BLACK);
                }
            }
        }
        
        return image;
    }
    
    /**
     * Draws a QR code finder pattern at the specified position.
     */
    private void drawFinderPattern(javafx.scene.image.PixelWriter writer, int x, int y, int size) {
        // Outer black square
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                writer.setColor(x + i, y + j, Color.BLACK);
            }
        }
        // Inner white square
        int innerOffset = 2;
        int innerSize = size - 4;
        for (int i = 0; i < innerSize; i++) {
            for (int j = 0; j < innerSize; j++) {
                writer.setColor(x + innerOffset + i, y + innerOffset + j, Color.WHITE);
            }
        }
        // Center black square
        int centerOffset = 4;
        int centerSize = size - 8;
        for (int i = 0; i < centerSize; i++) {
            for (int j = 0; j < centerSize; j++) {
                writer.setColor(x + centerOffset + i, y + centerOffset + j, Color.BLACK);
            }
        }
    }
    
    /**
     * Fills a module (small square) in the QR code.
     */
    private void fillModule(javafx.scene.image.PixelWriter writer, int x, int y, int size, Color color) {
        for (int i = 0; i < size && x + i < QR_CODE_SIZE; i++) {
            for (int j = 0; j < size && y + j < QR_CODE_SIZE; j++) {
                writer.setColor(x + i, y + j, color);
            }
        }
    }
    
    /**
     * Starts polling for the access token.
     */
    private void startPolling(String deviceCode) {
        updateState(AuthState.POLLING);
        
        currentOperation = oauthService.pollForAccessToken(deviceCode)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    updateState(AuthState.SUCCESS);
                    if (onAuthSuccess != null) {
                        onAuthSuccess.accept(response);
                    }
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    if (currentState != AuthState.CANCELLED) {
                        showError(getShortErrorMessage(ex));
                    }
                });
                return null;
            });
    }
    
    /**
     * Updates the authentication state and UI.
     */
    private void updateState(AuthState newState) {
        this.currentState = newState;
        
        switch (newState) {
            case INITIALIZING:
                statusLabel.setText("Connecting to GitHub...");
                progressIndicator.setVisible(true);
                skipButton.setVisible(true);
                break;
                
            case WAITING_FOR_USER:
                statusLabel.setText("Enter code on GitHub");
                progressIndicator.setVisible(true);
                break;
                
            case POLLING:
                statusLabel.setText("Waiting for authorization...");
                progressIndicator.setVisible(true);
                break;
                
            case SUCCESS:
                statusLabel.setText("Success!");
                statusLabel.setStyle(PokedexTheme.combineStyles(
                    PokedexTheme.getPixelFontStyle(7),
                    PokedexTheme.getTextStyle("#4ADE80")
                ));
                progressIndicator.setVisible(false);
                skipButton.setVisible(false);
                break;
                
            case ERROR:
                statusLabel.setText(errorMessage != null ? errorMessage : "Authentication failed");
                statusLabel.setStyle(PokedexTheme.combineStyles(
                    PokedexTheme.getPixelFontStyle(7),
                    PokedexTheme.getTextStyle("#EF4444")
                ));
                progressIndicator.setVisible(false);
                break;
                
            case CANCELLED:
                statusLabel.setText("Skipped");
                progressIndicator.setVisible(false);
                break;
        }
    }
    
    /**
     * Shows an error message.
     */
    private void showError(String message) {
        this.errorMessage = message;
        updateState(AuthState.ERROR);
        if (onAuthError != null) {
            onAuthError.run();
        }
    }
    
    /**
     * Gets a short error message from an exception.
     */
    private String getShortErrorMessage(Throwable ex) {
        String msg = ex.getMessage();
        if (msg == null) {
            msg = ex.getClass().getSimpleName();
        }
        // Truncate long messages
        if (msg.length() > 30) {
            msg = msg.substring(0, 27) + "...";
        }
        return msg;
    }
    
    /**
     * Cancels the current authentication operation.
     */
    public void cancelAuthentication() {
        updateState(AuthState.CANCELLED);
        if (currentOperation != null && !currentOperation.isDone()) {
            currentOperation.cancel(true);
        }
    }
    
    /**
     * Sets the error callback.
     */
    public void setOnAuthError(Runnable callback) {
        this.onAuthError = callback;
    }
    
    /**
     * Gets the current authentication state.
     */
    public AuthState getCurrentState() {
        return currentState;
    }
    
    /**
     * Gets the device code response if available.
     */
    public DeviceCodeResponse getDeviceCodeResponse() {
        return deviceCodeResponse;
    }
    
    /**
     * Checks if authentication is in progress.
     */
    public boolean isAuthInProgress() {
        return currentState == AuthState.INITIALIZING || 
               currentState == AuthState.WAITING_FOR_USER || 
               currentState == AuthState.POLLING;
    }
    
    /**
     * Checks if authentication was successful.
     */
    public boolean isAuthSuccessful() {
        return currentState == AuthState.SUCCESS;
    }
    
    /**
     * Retries authentication after an error.
     */
    public void retryAuthentication() {
        startAuthentication();
    }
    
    /**
     * Cleans up resources.
     */
    public void cleanup() {
        cancelAuthentication();
    }
}
