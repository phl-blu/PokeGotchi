package com.tamagotchi.committracker.ui.components;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import com.tamagotchi.committracker.github.AccessTokenResponse;
import com.tamagotchi.committracker.github.DeviceCodeResponse;
import com.tamagotchi.committracker.github.GitHubOAuthService;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Standalone GitHub login window — shown before the widget appears.
 * Uses GitHub's navy brand colour (#001C4D) with geometric sans-serif bold white text.
 */
public class GitHubLoginWindow {

    private static final int W = 440;
    private static final int H = 420;
    private static final String BG       = "#001C4D";
    private static final String SURFACE  = "#0D2B6B";
    private static final String ACCENT   = "#4ade80";
    private static final String MUTED    = "#8b9dc3";
    private static final String FONT     = "-fx-font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;";

    private Stage loginStage;
    private final GitHubOAuthService oauthService;
    private final Consumer<AccessTokenResponse> onSuccess;
    private final Runnable onSkipped;
    private CompletableFuture<?> currentOp;

    // UI refs
    private Label statusLabel;
    private Label userCodeLabel;
    private Label urlLabel;
    private ProgressIndicator spinner;
    private VBox codeSection;

    public GitHubLoginWindow(Stage owner, GitHubOAuthService oauthService,
                             Consumer<AccessTokenResponse> onSuccess,
                             Runnable onSkipped) {
        this.oauthService = oauthService;
        this.onSuccess    = onSuccess;
        this.onSkipped    = onSkipped;
        build(owner);
    }

    private void build(Stage owner) {
        loginStage = new Stage(StageStyle.TRANSPARENT);
        loginStage.setTitle("Sign in with GitHub");
        // Don't set owner — prevents JavaFX from shutting down when this window closes
        loginStage.setAlwaysOnTop(true);
        loginStage.setResizable(false);

        // ── Root ──────────────────────────────────────────────────────────
        StackPane root = new StackPane();
        root.setPrefSize(W, H);
        root.setStyle("-fx-background-color: transparent;");

        // ── Card ──────────────────────────────────────────────────────────
        VBox card = new VBox(10);
        card.setPadding(new Insets(0, 0, 20, 0));
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefSize(W, H);
        card.setStyle(
            "-fx-background-color: " + BG + ";" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #1a3a7a;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;"
        );

        // ── Windows-style title bar — buttons top-right, no label ─────────
        HBox titleBar = new HBox();
        titleBar.setAlignment(Pos.CENTER_RIGHT);
        titleBar.setPrefHeight(32);
        titleBar.setStyle("-fx-background-color: #001030; -fx-background-radius: 10 10 0 0;");

        // Minimize button
        Label minBtn = new Label("─");
        minBtn.setPrefSize(46, 32);
        minBtn.setAlignment(Pos.CENTER);
        minBtn.setStyle(FONT + "-fx-font-size: 14px; -fx-text-fill: white; -fx-cursor: hand; -fx-background-color: transparent;");
        minBtn.setOnMouseEntered(e -> minBtn.setStyle(FONT + "-fx-font-size: 14px; -fx-text-fill: white; -fx-cursor: hand; -fx-background-color: #2a3a5a;"));
        minBtn.setOnMouseExited(e -> minBtn.setStyle(FONT + "-fx-font-size: 14px; -fx-text-fill: white; -fx-cursor: hand; -fx-background-color: transparent;"));
        minBtn.setOnMouseClicked(e -> loginStage.setIconified(true));

        // Close button
        Label closeBtn = new Label("✕");
        closeBtn.setPrefSize(46, 32);
        closeBtn.setAlignment(Pos.CENTER);
        closeBtn.setStyle(FONT + "-fx-font-size: 13px; -fx-text-fill: white; -fx-cursor: hand; -fx-background-color: transparent; -fx-background-radius: 0 10 0 0;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(FONT + "-fx-font-size: 13px; -fx-text-fill: white; -fx-cursor: hand; -fx-background-color: #c42b1c; -fx-background-radius: 0 10 0 0;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(FONT + "-fx-font-size: 13px; -fx-text-fill: white; -fx-cursor: hand; -fx-background-color: transparent; -fx-background-radius: 0 10 0 0;"));
        closeBtn.setOnMouseClicked(e -> { cancel(); Platform.exit(); System.exit(0); });

        titleBar.getChildren().addAll(minBtn, closeBtn);

        // Draggable via title bar
        final double[] drag = {0, 0};
        titleBar.setOnMousePressed(e -> { drag[0] = loginStage.getX() - e.getScreenX(); drag[1] = loginStage.getY() - e.getScreenY(); });
        titleBar.setOnMouseDragged(e -> { loginStage.setX(e.getScreenX() + drag[0]); loginStage.setY(e.getScreenY() + drag[1]); });

        // ── Content area ──────────────────────────────────────────────────
        VBox content = new VBox(0); // spacing=0, we control margins manually
        content.setPadding(new Insets(16, 32, 0, 32));
        content.setAlignment(Pos.TOP_CENTER);

        // GitHub logo
        ImageView logo = loadLogo();
        if (logo != null) {
            content.getChildren().add(logo);
            VBox.setMargin(logo, new Insets(0, 0, 8, 0));
        }

        // Title
        Label title = new Label("Sign in with GitHub");
        title.setStyle(FONT + "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        content.getChildren().add(title);

        // Subtitle — 2 lines below title (16px gap)
        Label subtitle = new Label("Connect your account to track commits across all your repos");
        subtitle.setStyle(FONT + "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(W - 80);
        subtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        subtitle.setAlignment(Pos.CENTER);
        VBox.setMargin(subtitle, new Insets(16, 0, 0, 0));
        content.getChildren().add(subtitle);

        // ── Code section ──────────────────────────────────────────────────
        codeSection = new VBox(0);
        codeSection.setAlignment(Pos.CENTER);
        codeSection.setVisible(false);
        codeSection.setManaged(false);

        // "Visit URL" — 1 line below subtitle (12px gap)
        Label step1 = new Label("1. Visit this URL in your browser:");
        step1.setStyle(FONT + "-fx-font-size: 11px; -fx-text-fill: white;");
        VBox.setMargin(step1, new Insets(12, 0, 4, 0));

        urlLabel = new Label("github.com/login/device");
        urlLabel.setStyle(FONT + "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #60a5fa; -fx-cursor: hand; -fx-underline: true;");
        urlLabel.setOnMouseClicked(e -> openBrowser("https://github.com/login/device"));
        VBox.setMargin(urlLabel, new Insets(0, 0, 0, 0));

        // "Enter code" — 1 line below URL (12px gap)
        Label step2 = new Label("2. Enter this code:");
        step2.setStyle(FONT + "-fx-font-size: 11px; -fx-text-fill: white;");
        VBox.setMargin(step2, new Insets(12, 0, 4, 0));

        // Code box
        userCodeLabel = new Label("--------");
        userCodeLabel.setStyle(FONT +
            "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT + ";" +
            "-fx-background-color: rgba(74,222,128,0.08);" +
            "-fx-padding: 6 20; -fx-background-radius: 6;" +
            "-fx-border-color: rgba(74,222,128,0.3); -fx-border-radius: 6; -fx-border-width: 1;" +
            "-fx-cursor: hand; -fx-letter-spacing: 4;");
        userCodeLabel.setOnMouseClicked(e -> copyCode());
        userCodeLabel.setOnMouseEntered(e -> userCodeLabel.setStyle(FONT +
            "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT + ";" +
            "-fx-background-color: rgba(74,222,128,0.15);" +
            "-fx-padding: 6 20; -fx-background-radius: 6;" +
            "-fx-border-color: rgba(74,222,128,0.6); -fx-border-radius: 6; -fx-border-width: 1;" +
            "-fx-cursor: hand; -fx-letter-spacing: 4;"));
        userCodeLabel.setOnMouseExited(e -> userCodeLabel.setStyle(FONT +
            "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT + ";" +
            "-fx-background-color: rgba(74,222,128,0.08);" +
            "-fx-padding: 6 20; -fx-background-radius: 6;" +
            "-fx-border-color: rgba(74,222,128,0.3); -fx-border-radius: 6; -fx-border-width: 1;" +
            "-fx-cursor: hand; -fx-letter-spacing: 4;"));
        VBox.setMargin(userCodeLabel, new Insets(0, 0, 6, 0));

        // Click to copy button
        Label copyHint = new Label("click to copy");
        copyHint.setStyle(FONT +
            "-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white;" +
            "-fx-cursor: hand; -fx-padding: 4 10;" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: #1a3a7a; -fx-border-radius: 4; -fx-background-radius: 4;");
        copyHint.setOnMouseClicked(e -> copyCode());
        copyHint.setOnMouseEntered(e -> copyHint.setStyle(FONT +
            "-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white;" +
            "-fx-cursor: hand; -fx-padding: 4 10;" +
            "-fx-background-color: " + SURFACE + ";" +
            "-fx-border-color: #2a4a9a; -fx-border-radius: 4; -fx-background-radius: 4;"));
        copyHint.setOnMouseExited(e -> copyHint.setStyle(FONT +
            "-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white;" +
            "-fx-cursor: hand; -fx-padding: 4 10;" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: #1a3a7a; -fx-border-radius: 4; -fx-background-radius: 4;"));

        codeSection.getChildren().addAll(step1, urlLabel, step2, userCodeLabel, copyHint);
        VBox.setMargin(codeSection, new Insets(0, 0, 0, 0));
        content.getChildren().add(codeSection);

        // ── Status row — 1 line below click-to-copy (12px gap) ────────────
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER);

        spinner = new ProgressIndicator();
        spinner.setMaxSize(14, 14);
        spinner.setStyle("-fx-progress-color: " + ACCENT + ";");

        statusLabel = new Label("Connecting to GitHub...");
        statusLabel.setStyle(FONT + "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");

        statusRow.getChildren().addAll(spinner, statusLabel);
        VBox.setMargin(statusRow, new Insets(28, 0, 0, 0));
        content.getChildren().add(statusRow);

        // ── Skip button — pushed further down (24px gap) ──────────────────
        Label skipBtn = new Label("Use local Git only");
        skipBtn.setStyle(FONT +
            "-fx-font-size: 11px; -fx-text-fill: white;" +
            "-fx-cursor: hand; -fx-padding: 6 14;" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: #1a3a7a; -fx-border-radius: 6; -fx-background-radius: 6;");
        skipBtn.setOnMouseEntered(e -> skipBtn.setStyle(FONT +
            "-fx-font-size: 11px; -fx-text-fill: white;" +
            "-fx-cursor: hand; -fx-padding: 6 14;" +
            "-fx-background-color: " + SURFACE + ";" +
            "-fx-border-color: #2a4a9a; -fx-border-radius: 6; -fx-background-radius: 6;"));
        skipBtn.setOnMouseExited(e -> skipBtn.setStyle(FONT +
            "-fx-font-size: 11px; -fx-text-fill: white;" +
            "-fx-cursor: hand; -fx-padding: 6 14;" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: #1a3a7a; -fx-border-radius: 6; -fx-background-radius: 6;"));
        skipBtn.setOnMouseClicked(e -> skip());
        VBox.setMargin(skipBtn, new Insets(24, 0, 0, 0));
        content.getChildren().add(skipBtn);

        // ── Assemble ──────────────────────────────────────────────────────
        card.getChildren().addAll(titleBar, content);
        root.getChildren().add(card);

        Rectangle clip = new Rectangle(W, H);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        root.setClip(clip);

        Scene scene = new Scene(root, W, H);
        scene.setFill(Color.TRANSPARENT);
        loginStage.setScene(scene);
        loginStage.centerOnScreen();
    }

    private ImageView loadLogo() {
        try {
            var stream = getClass().getResourceAsStream("/ui/githublogo.png");
            if (stream == null) return null;
            ImageView iv = new ImageView(new Image(stream));
            iv.setFitWidth(48);
            iv.setFitHeight(48);
            iv.setPreserveRatio(true);
            return iv;
        } catch (Exception e) {
            return null;
        }
    }

    public void show() {
        Platform.setImplicitExit(false); // prevent app exit when login window closes
        loginStage.show();
        startAuth();
    }

    private void startAuth() {
        if (oauthService == null) {
            setStatus("GitHub not configured", "#ef4444");
            return;
        }
        setStatus("Connecting to GitHub...", "white");
        currentOp = oauthService.initiateDeviceFlow()
            .thenAccept(r -> Platform.runLater(() -> { showCode(r); poll(r.deviceCode()); }))
            .exceptionally(ex -> { Platform.runLater(() -> setStatus("Failed: " + short_(ex), "#ef4444")); return null; });
    }

    private void showCode(DeviceCodeResponse r) {
        userCodeLabel.setText(r.userCode());
        urlLabel.setText(r.verificationUri());
        codeSection.setVisible(true);
        codeSection.setManaged(true);
        setStatus("Waiting for authorization...", "white");
        openBrowser("https://github.com/login/device");
    }

    private void poll(String deviceCode) {
        currentOp = oauthService.pollForAccessToken(deviceCode)
            .thenAccept(token -> Platform.runLater(() -> {
                setStatus("Connected!", ACCENT);
                spinner.setVisible(false);
                loginStage.close();
                if (onSuccess != null) onSuccess.accept(token);
            }))
            .exceptionally(ex -> { Platform.runLater(() -> setStatus(short_(ex), "#ef4444")); return null; });
    }

    private void skip() {
        cancel();
        loginStage.close();
        if (onSkipped != null) onSkipped.run();
    }

    private void cancel() {
        if (currentOp != null && !currentOp.isDone()) currentOp.cancel(true);
    }

    private void copyCode() {
        String code = userCodeLabel.getText();
        if (code != null && !code.equals("--------")) {
            ClipboardContent c = new ClipboardContent();
            c.putString(code);
            Clipboard.getSystemClipboard().setContent(c);
            statusLabel.setText("Code copied!");
        }
    }

    private void openBrowser(String url) {
        try { java.awt.Desktop.getDesktop().browse(new java.net.URI(url)); } catch (Exception ignored) {}
    }

    private void setStatus(String text, String color) {
        statusLabel.setText(text);
        statusLabel.setStyle(FONT + "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    }

    private String short_(Throwable ex) {
        String m = ex.getMessage();
        if (m == null) m = ex.getClass().getSimpleName();
        return m.length() > 45 ? m.substring(0, 42) + "..." : m;
    }
}
