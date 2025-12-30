package com.tamagotchi.committracker.github;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Secure token storage implementation using AES-256-GCM encryption.
 * 
 * Security features:
 * - AES-256-GCM authenticated encryption
 * - PBKDF2 key derivation with 100,000 iterations
 * - Machine-specific salt derived from hardware identifiers
 * - Secure random IV generation for each encryption
 * - File permissions restricted to user-only access
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6
 */
public class SecureTokenStorageImpl implements SecureTokenStorage {
    
    private static final Logger LOGGER = Logger.getLogger(SecureTokenStorageImpl.class.getName());
    
    // Encryption constants
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final int SALT_LENGTH = 32;
    
    // Storage file names
    private static final String TOKENS_FILE = "tokens.enc";
    private static final String SALT_FILE = "salt.bin";
    
    // Token keys in storage
    private static final String ACCESS_TOKEN_KEY = "access_token";
    private static final String REFRESH_TOKEN_KEY = "refresh_token";
    
    private final Path storageDir;
    private final SecureRandom secureRandom;
    
    // Cached encryption key (derived on first use)
    private SecretKey cachedKey;
    private byte[] cachedSalt;
    
    /**
     * Creates a SecureTokenStorageImpl with the default storage directory.
     */
    public SecureTokenStorageImpl() {
        this(Paths.get(System.getProperty("user.home"), ".tamagotchi-commit-tracker", "secure"));
    }
    
    /**
     * Creates a SecureTokenStorageImpl with a custom storage directory.
     * @param storageDir the directory to store encrypted tokens
     */
    public SecureTokenStorageImpl(Path storageDir) {
        this.storageDir = storageDir;
        this.secureRandom = new SecureRandom();
    }

    @Override
    public void storeAccessToken(String accessToken) {
        Objects.requireNonNull(accessToken, "Access token cannot be null");
        storeToken(ACCESS_TOKEN_KEY, accessToken);
    }
    
    @Override
    public void storeRefreshToken(String refreshToken) {
        Objects.requireNonNull(refreshToken, "Refresh token cannot be null");
        storeToken(REFRESH_TOKEN_KEY, refreshToken);
    }
    
    @Override
    public Optional<String> getAccessToken() {
        return getToken(ACCESS_TOKEN_KEY);
    }
    
    @Override
    public Optional<String> getRefreshToken() {
        return getToken(REFRESH_TOKEN_KEY);
    }
    
    @Override
    public void clearAllTokens() {
        try {
            Path tokensPath = storageDir.resolve(TOKENS_FILE);
            if (Files.exists(tokensPath)) {
                // Overwrite with random data before deletion for security
                byte[] randomData = new byte[(int) Files.size(tokensPath)];
                secureRandom.nextBytes(randomData);
                Files.write(tokensPath, randomData);
                Files.delete(tokensPath);
            }
            
            // Clear cached key from memory
            cachedKey = null;
            
            LOGGER.info("All tokens cleared securely");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to clear tokens securely", e);
            throw new SecurityException("Failed to clear tokens", e);
        }
    }
    
    @Override
    public void rotateEncryptionKey() {
        try {
            // Read existing tokens with current key
            Optional<String> accessToken = getAccessToken();
            Optional<String> refreshToken = getRefreshToken();
            
            // Generate new salt (which will derive a new key)
            cachedSalt = generateSalt();
            cachedKey = null; // Force key re-derivation
            
            // Save new salt
            ensureStorageDirectory();
            Files.write(storageDir.resolve(SALT_FILE), cachedSalt);
            setFilePermissions(storageDir.resolve(SALT_FILE));
            
            // Clear existing tokens file
            Path tokensPath = storageDir.resolve(TOKENS_FILE);
            if (Files.exists(tokensPath)) {
                Files.delete(tokensPath);
            }
            
            // Re-encrypt tokens with new key
            accessToken.ifPresent(this::storeAccessToken);
            refreshToken.ifPresent(this::storeRefreshToken);
            
            LOGGER.info("Encryption key rotated successfully");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to rotate encryption key", e);
            throw new SecurityException("Failed to rotate encryption key", e);
        }
    }
    
    @Override
    public boolean hasStoredTokens() {
        return getAccessToken().isPresent() || getRefreshToken().isPresent();
    }

    // ==================== Private Helper Methods ====================
    
    private void storeToken(String key, String value) {
        try {
            ensureStorageDirectory();
            
            // Load existing tokens
            Map<String, String> tokens = loadTokens();
            tokens.put(key, value);
            
            // Encrypt and save
            byte[] plaintext = serializeTokens(tokens);
            byte[] encrypted = encrypt(plaintext);
            
            Path tokensPath = storageDir.resolve(TOKENS_FILE);
            Files.write(tokensPath, encrypted);
            setFilePermissions(tokensPath);
            
            // Clear plaintext from memory
            Arrays.fill(plaintext, (byte) 0);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to store token", e);
            throw new SecurityException("Failed to store token securely", e);
        }
    }
    
    private Optional<String> getToken(String key) {
        try {
            Path tokensPath = storageDir.resolve(TOKENS_FILE);
            if (!Files.exists(tokensPath)) {
                return Optional.empty();
            }
            
            byte[] encrypted = Files.readAllBytes(tokensPath);
            byte[] plaintext = decrypt(encrypted);
            
            Map<String, String> tokens = deserializeTokens(plaintext);
            
            // Clear plaintext from memory
            Arrays.fill(plaintext, (byte) 0);
            
            return Optional.ofNullable(tokens.get(key));
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to retrieve token", e);
            return Optional.empty();
        }
    }
    
    private Map<String, String> loadTokens() {
        try {
            Path tokensPath = storageDir.resolve(TOKENS_FILE);
            if (!Files.exists(tokensPath)) {
                return new HashMap<>();
            }
            
            byte[] encrypted = Files.readAllBytes(tokensPath);
            byte[] plaintext = decrypt(encrypted);
            Map<String, String> tokens = deserializeTokens(plaintext);
            
            // Clear plaintext from memory
            Arrays.fill(plaintext, (byte) 0);
            
            return tokens;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load existing tokens, starting fresh", e);
            return new HashMap<>();
        }
    }

    // ==================== Encryption Methods ====================
    
    /**
     * Encrypts data using AES-256-GCM.
     * Format: [IV (12 bytes)][Ciphertext + Auth Tag]
     */
    private byte[] encrypt(byte[] plaintext) throws Exception {
        SecretKey key = getOrDeriveKey();
        
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
        
        byte[] ciphertext = cipher.doFinal(plaintext);
        
        // Combine IV and ciphertext
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        buffer.put(iv);
        buffer.put(ciphertext);
        
        return buffer.array();
    }
    
    /**
     * Decrypts data using AES-256-GCM.
     */
    private byte[] decrypt(byte[] encrypted) throws Exception {
        SecretKey key = getOrDeriveKey();
        
        // Extract IV and ciphertext
        ByteBuffer buffer = ByteBuffer.wrap(encrypted);
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
        
        return cipher.doFinal(ciphertext);
    }
    
    /**
     * Gets the cached key or derives a new one using PBKDF2.
     */
    private SecretKey getOrDeriveKey() throws Exception {
        if (cachedKey != null) {
            return cachedKey;
        }
        
        byte[] salt = getOrCreateSalt();
        char[] password = getMachineIdentifier();
        
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
            PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH);
            SecretKey tmp = factory.generateSecret(spec);
            cachedKey = new SecretKeySpec(tmp.getEncoded(), KEY_ALGORITHM);
            
            // Clear password from memory
            spec.clearPassword();
            Arrays.fill(password, '\0');
            
            return cachedKey;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    // ==================== Salt and Key Management ====================
    
    /**
     * Gets existing salt or creates a new one.
     */
    private byte[] getOrCreateSalt() throws IOException {
        if (cachedSalt != null) {
            return cachedSalt;
        }
        
        Path saltPath = storageDir.resolve(SALT_FILE);
        
        if (Files.exists(saltPath)) {
            cachedSalt = Files.readAllBytes(saltPath);
        } else {
            ensureStorageDirectory();
            cachedSalt = generateSalt();
            Files.write(saltPath, cachedSalt);
            setFilePermissions(saltPath);
        }
        
        return cachedSalt;
    }
    
    /**
     * Generates a cryptographically secure random salt.
     */
    private byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return salt;
    }
    
    /**
     * Gets a machine-specific identifier for key derivation.
     * Combines multiple hardware identifiers for uniqueness.
     */
    private char[] getMachineIdentifier() {
        StringBuilder identifier = new StringBuilder();
        
        // Add user name
        identifier.append(System.getProperty("user.name", "default"));
        
        // Add OS info
        identifier.append(System.getProperty("os.name", ""));
        identifier.append(System.getProperty("os.arch", ""));
        
        // Try to add MAC address
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    for (byte b : mac) {
                        identifier.append(String.format("%02X", b));
                    }
                    break; // Use first available MAC
                }
            }
        } catch (Exception e) {
            // Fallback: use user home directory
            identifier.append(System.getProperty("user.home", ""));
        }
        
        // Add a constant application identifier
        identifier.append("TamagotchiCommitTracker-v1");
        
        return identifier.toString().toCharArray();
    }

    // ==================== File and Serialization Utilities ====================
    
    /**
     * Ensures the storage directory exists with proper permissions.
     */
    private void ensureStorageDirectory() throws IOException {
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
            setFilePermissions(storageDir);
        }
    }
    
    /**
     * Sets restrictive file permissions (user-only read/write).
     * Requirement 2.6: Set appropriate file permissions
     */
    private void setFilePermissions(Path path) {
        try {
            // Try POSIX permissions first (Unix/Linux/Mac)
            Set<PosixFilePermission> permissions = EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            );
            if (Files.isDirectory(path)) {
                permissions.add(PosixFilePermission.OWNER_EXECUTE);
            }
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException e) {
            // Windows: Use File.setReadable/setWritable
            File file = path.toFile();
            file.setReadable(false, false);
            file.setWritable(false, false);
            file.setExecutable(false, false);
            file.setReadable(true, true);
            file.setWritable(true, true);
            if (file.isDirectory()) {
                file.setExecutable(true, true);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to set file permissions", e);
        }
    }
    
    /**
     * Serializes token map to bytes.
     */
    private byte[] serializeTokens(Map<String, String> tokens) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(tokens);
        }
        return baos.toByteArray();
    }
    
    /**
     * Deserializes bytes to token map.
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> deserializeTokens(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (Map<String, String>) ois.readObject();
        }
    }
    
    /**
     * Gets the storage directory path (for testing purposes).
     */
    Path getStorageDir() {
        return storageDir;
    }
}
