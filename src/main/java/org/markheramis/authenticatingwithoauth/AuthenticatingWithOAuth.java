package org.markheramis.authenticatingwithoauth;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.net.InetSocketAddress;
import java.io.IOException;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

/**
 * A simple OAuth authentication example using Java. This application sets up a
 * local server to handle OAuth callbacks.
 *
 * Author: Mark Heramis <chumheramis@gmail.com>
 */
public class AuthenticatingWithOAuth {

    // OAuth configuration constants
    private static final String REDIRECT_URI = "http://localhost:3000/auth/callback";
    // The HTTP client to send requests to the OAuth provider
    private static final HttpClient client = HttpClient.newHttpClient();

    // The client ID for the OAuth provider
    private static String client_id = "";
    // The authorization URL for the OAuth provider
    private static String authorization_url = "";
    // The token URL for the OAuth provider
    private static String token_url = "";


    public static void main(String[] args) throws Exception {
        getInput();
        // Generate state, code verifier, and code challenge
        String state = generateRandomString(40);
        // Generate code verifier and code challenge
        String codeVerifier = generateCodeVerifier();
        // Generate code challenge
        String codeChallenge = generateCodeChallenge(codeVerifier);
        // Construct the authorization URL
        String authorizationUrl = authorization_url + "?client_id=" + client_id
                + "&redirect_uri=" + REDIRECT_URI
                + "&response_type=code"
                + "&scope="
                + "&state=" + state
                + "&code_challenge=" + codeChallenge
                + "&code_challenge_method=S256";

        // Open the authorization URL in the default browser
        java.awt.Desktop.getDesktop().browse(new URI(authorizationUrl));

        // Wait for the authorization code and state
        System.out.println("Waiting for the authorization code and state...");
        Map<String, String> queryParams = waitForCallback();
        String authorizationCode = queryParams.get("code");
        String returnedState = queryParams.get("state");

        // Validate state and exchange authorization code for access token
        if (!state.equals(returnedState)) {
            throw new IllegalArgumentException("Invalid state parameter.");
        }
        exchangeAuthorizationCodeForToken(client_id, authorizationCode, codeVerifier);
    }

    /**
     * Get the client ID from the user.
     *
     * @return the client ID
     */
    private static void getInput() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter OAuth Client ID: ");
            client_id = scanner.nextLine();
            System.out.println("Please provide Authorization URL (e.g. http://localhost:8000/oauth/authorize): ");
            System.out.print("Enter OAuth Authorization URL: ");
            authorization_url = scanner.nextLine();
            System.out.println("Please provide Token URL (e.g. http://localhost:8000/oauth/token): ");
            System.out.print("Enter OAuth Token URL: ");
            token_url = scanner.nextLine();
        }
    }

    /**
     * Generate a code verifier.
     *
     * This method generates a code verifier by creating a random sequence of bytes,
     * encoding them using URL-safe Base64 encoding, and removing any padding characters.
     *
     * @return a random URL-safe string
     */
    private static String generateCodeVerifier() {
        byte[] randomBytes = new byte[32];
        new Random().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Generate code challenge from code verifier.
     *
     * This method takes a code verifier string and generates a code challenge
     * using the following steps:
     * 1. Hash the code verifier using SHA-256.
     * 2. Encode the resulting hash using URL-safe Base64 encoding.
     * 3. Replace '+' with '-' and '/' with '_' to ensure the string is URL-safe.
     *
     * @param codeVerifier a code verifier String
     * @return URL-safe Base64 encoded String
     * @throws NoSuchAlgorithmException
     */
    private static String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
                .replace('+', '-').replace('/', '_');
    }

    /**
     * Generate a random string for state parameter.
     *
     * This method generates a random string of the specified length using
     * a combination of uppercase letters, lowercase letters, and digits.
     * It uses a `StringBuilder` to construct the string and a `Random` 
     * instance to select random characters from the defined character set.
     *
     * @param length the length of the String to generate
     * @return the generated String
     */
    private static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Exchange the authorization code for an access token.
     *
     * This method sends a POST request to the token endpoint with the required
     * parameters to exchange the authorization code for an access token.
     *
     * @param client_id the client ID of the application
     * @param code the authorization code received from the authorization server
     * @param codeVerifier the code verifier used in the PKCE flow
     * @throws Exception if an error occurs during the request
     */
    private static void exchangeAuthorizationCodeForToken(String client_id, String code, String codeVerifier) throws Exception {
        // Create a map to hold the form data parameters
        Map<Object, Object> formData = new HashMap<>();
        formData.put("grant_type", "authorization_code");
        formData.put("client_id", client_id);
        formData.put("redirect_uri", REDIRECT_URI);
        formData.put("code_verifier", codeVerifier);
        formData.put("code", code);

        // Build the HTTP request with the form data
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(token_url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(buildFormData(formData))
                .build();

        // Send the HTTP request and get the response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Print the access token response
        System.out.println("Access Token Response: " + response.body());
    }

    /**
     * Build form data for POST requests.
     *
     * @param data
     * @return
     */
    private static HttpRequest.BodyPublisher buildFormData(Map<Object, Object> data) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }

    /**
     * Start a simple HTTP server to capture the OAuth callback.
     * This server listens on port 3000 and waits for the OAuth authorization server
     * to redirect the user back to our application with the authorization code.
     *
     * @return a map containing the query parameters from the callback URL
     * @throws IOException if an error occurs while creating or starting the server
     */
    private static Map<String, String> waitForCallback() throws IOException {
        // Create an HTTP server that listens on port 3000
        HttpServer server = HttpServer.create(new InetSocketAddress(3000), 0);
        // Map to store the query parameters from the callback URL
        Map<String, String> queryParams = new HashMap<>();
        // CountDownLatch to wait for the callback request to be handled
        CountDownLatch latch = new CountDownLatch(1);
        
        /**
         * Handle the HTTP request and extract the query parameters.
         * This context handles requests to the /auth/callback endpoint.
         *
         * @param exchange the HTTP exchange object containing the request and response
         * @throws IOException if an error occurs while handling the request
         */
        server.createContext("/auth/callback", (HttpExchange exchange) -> {
            // Extract the query parameters from the request URL
            String query = exchange.getRequestURI().getQuery();
            if (query != null) {
                // Split the query string into individual parameters
                String[] params = query.split("&");
                for (String param : params) {
                    // Split each parameter into a key and value
                    String[] keyValue = param.split("=");
                    if (keyValue.length > 1) {
                        // Store the key and value in the queryParams map
                        queryParams.put(keyValue[0], keyValue[1]);
                    }
                }
            }
            // Send a response to the client indicating that the authorization was received
            String response = "<html><body>Authorization received. This window will close automatically.<script>window.close();</script></body></html>";
            exchange.sendResponseHeaders(200, response.length());
            // Signal that the request has been handled by counting down the latch
            latch.countDown();
        });
        
        // Start the server
        server.start();
        System.out.println("Server started at http://localhost:3000/auth/callback. Waiting for the callback...");

        // Wait for the callback request to be handled
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Stop the server after handling the callback request
        server.stop(0);
        System.out.println("Server has stopped.");
        
        // Return the query parameters from the callback URL
        return queryParams;
    }
}
