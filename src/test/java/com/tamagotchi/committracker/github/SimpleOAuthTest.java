package com.tamagotchi.committracker.github;

import java.io.*;
import java.net.*;

/**
 * Simple test to debug OAuth 404 issue
 */
public class SimpleOAuthTest {
    public static void main(String[] args) throws Exception {
        String clientId = System.getenv("GITHUB_CLIENT_ID");
        if (clientId == null) {
            clientId = GitHubConfig.getClientId();
        }
        if (clientId == null) {
            System.err.println("ERROR: No client ID found. Set GITHUB_CLIENT_ID env var or add to config/github.properties");
            return;
        }
        
        // Trim any whitespace!
        clientId = clientId.trim();
        
        System.out.println("Testing GitHub Device Flow...");
        System.out.println("Client ID: [" + clientId + "]");
        System.out.println("Client ID length: " + clientId.length());
        
        URL url = new URL("https://github.com/login/device/code");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "Pokemon-Commit-Tracker/1.0");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        
        String body = "client_id=" + clientId + "&scope=" + URLEncoder.encode("repo read:user", "UTF-8");
        System.out.println("Request body: [" + body + "]");
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes("UTF-8"));
        }
        
        int responseCode = conn.getResponseCode();
        System.out.println("Response code: " + responseCode);
        
        InputStream is = responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        }
    }
}
