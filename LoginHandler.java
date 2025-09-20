import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * LoginHandler - Java backend class for handling user authentication
 * This class provides methods for user login validation, password hashing,
 * and session management that would typically run on a server.
 */
public class LoginHandler {
    
    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    
    // Minimum password length
    private static final int MIN_PASSWORD_LENGTH = 6;
    
    // In-memory user database (in production, this would be a real database)
    private static final Map<String, User> userDatabase = new HashMap<>();
    
    // Session storage (in production, this would be Redis or similar)
    private static final Map<String, Session> activeSessions = new HashMap<>();
    
    // Static block to initialize demo users
    static {
        // Add demo users with hashed passwords
        userDatabase.put("admin@example.com", 
            new User("admin@example.com", hashPassword("password123"), "Admin User", true));
        userDatabase.put("user@example.com", 
            new User("user@example.com", hashPassword("userpass"), "Regular User", true));
        userDatabase.put("demo@example.com", 
            new User("demo@example.com", hashPassword("demo123"), "Demo User", true));
    }
    
    /**
     * Main method for testing the LoginHandler
     */
    public static void main(String[] args) {
        LoginHandler handler = new LoginHandler();
        
        // Test cases
        System.out.println("=== Login Handler Test Cases ===\n");
        
        // Test 1: Valid login
        LoginResult result1 = handler.authenticateUser("admin@example.com", "password123", true);
        System.out.println("Test 1 - Valid login: " + result1);
        
        // Test 2: Invalid password
        LoginResult result2 = handler.authenticateUser("admin@example.com", "wrongpass", false);
        System.out.println("Test 2 - Invalid password: " + result2);
        
        // Test 3: Invalid email format
        LoginResult result3 = handler.authenticateUser("invalid-email", "password123", false);
        System.out.println("Test 3 - Invalid email: " + result3);
        
        // Test 4: Non-existent user
        LoginResult result4 = handler.authenticateUser("notfound@example.com", "password123", false);
        System.out.println("Test 4 - Non-existent user: " + result4);
        
        // Test 5: Session validation
        if (result1.isSuccess()) {
            boolean isValid = handler.validateSession(result1.getSessionToken());
            System.out.println("Test 5 - Session validation: " + isValid);
        }
    }
    
    /**
     * Authenticate user with email and password
     * @param email User's email address
     * @param password User's password (plain text)
     * @param rememberMe Whether to create a long-lasting session
     * @return LoginResult containing success status and session information
     */
    public LoginResult authenticateUser(String email, String password, boolean rememberMe) {
        try {
            // Input validation
            ValidationResult validation = validateLoginInput(email, password);
            if (!validation.isValid()) {
                return new LoginResult(false, validation.getErrorMessage(), null, null);
            }
            
            // Check if user exists
            User user = userDatabase.get(email.toLowerCase());
            if (user == null) {
                logLoginAttempt(email, false, "User not found");
                return new LoginResult(false, "Invalid email or password", null, null);
            }
            
            // Check if user is active
            if (!user.isActive()) {
                logLoginAttempt(email, false, "Account disabled");
                return new LoginResult(false, "Account is disabled", null, null);
            }
            
            // Verify password
            String hashedPassword = hashPassword(password);
            if (!user.getPasswordHash().equals(hashedPassword)) {
                logLoginAttempt(email, false, "Invalid password");
                return new LoginResult(false, "Invalid email or password", null, null);
            }
            
            // Create session
            String sessionToken = generateSessionToken();
            Session session = new Session(sessionToken, user.getEmail(), rememberMe);
            activeSessions.put(sessionToken, session);
            
            // Update user's last login
            user.setLastLogin(LocalDateTime.now());
            
            logLoginAttempt(email, true, "Login successful");
            
            return new LoginResult(true, "Login successful", sessionToken, user);
            
        } catch (Exception e) {
            System.err.println("Error during authentication: " + e.getMessage());
            return new LoginResult(false, "Internal server error", null, null);
        }
    }
    
    /**
     * Validate login input data
     * @param email Email to validate
     * @param password Password to validate
     * @return ValidationResult with validation status and error message
     */
    private ValidationResult validateLoginInput(String email, String password) {
        // Check for null or empty values
        if (email == null || email.trim().isEmpty()) {
            return new ValidationResult(false, "Email is required");
        }
        
        if (password == null || password.isEmpty()) {
            return new ValidationResult(false, "Password is required");
        }
        
        // Validate email format
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return new ValidationResult(false, "Invalid email format");
        }
        
        // Validate password length
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return new ValidationResult(false, 
                "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Validate session token
     * @param sessionToken Token to validate
     * @return true if session is valid and not expired
     */
    public boolean validateSession(String sessionToken) {
        if (sessionToken == null || sessionToken.isEmpty()) {
            return false;
        }
        
        Session session = activeSessions.get(sessionToken);
        if (session == null) {
            return false;
        }
        
        // Check if session is expired
        if (session.isExpired()) {
            activeSessions.remove(sessionToken);
            return false;
        }
        
        // Update last accessed time
        session.updateLastAccessed();
        return true;
    }
    
    /**
     * Logout user by invalidating session
     * @param sessionToken Session token to invalidate
     * @return true if logout was successful
     */
    public boolean logoutUser(String sessionToken) {
        if (sessionToken == null) {
            return false;
        }
        
        Session session = activeSessions.remove(sessionToken);
        if (session != null) {
            logLoginAttempt(session.getUserEmail(), true, "Logout successful");
            return true;
        }
        
        return false;
    }
    
    /**
     * Hash password using SHA-256
     * @param password Plain text password
     * @return Hashed password
     */
    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Generate a unique session token
     * @return Random session token
     */
    private String generateSessionToken() {
        return "session_" + System.currentTimeMillis() + "_" + 
               (int)(Math.random() * 10000);
    }
    
    /**
     * Log login attempts for security monitoring
     * @param email User email
     * @param success Whether login was successful
     * @param message Additional message
     */
    private void logLoginAttempt(String email, boolean success, String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String status = success ? "SUCCESS" : "FAILURE";
        System.out.println(String.format("[%s] LOGIN %s: %s - %s", 
                          timestamp, status, email, message));
    }
    
    /**
     * Get user information by session token
     * @param sessionToken Valid session token
     * @return User object or null if session is invalid
     */
    public User getUserBySession(String sessionToken) {
        if (!validateSession(sessionToken)) {
            return null;
        }
        
        Session session = activeSessions.get(sessionToken);
        return userDatabase.get(session.getUserEmail());
    }
    
    // Inner classes for data structures
    
    /**
     * User data class
     */
    public static class User {
        private String email;
        private String passwordHash;
        private String fullName;
        private boolean active;
        private LocalDateTime lastLogin;
        private LocalDateTime createdAt;
        
        public User(String email, String passwordHash, String fullName, boolean active) {
            this.email = email;
            this.passwordHash = passwordHash;
            this.fullName = fullName;
            this.active = active;
            this.createdAt = LocalDateTime.now();
        }
        
        // Getters and setters
        public String getEmail() { return email; }
        public String getPasswordHash() { return passwordHash; }
        public String getFullName() { return fullName; }
        public boolean isActive() { return active; }
        public LocalDateTime getLastLogin() { return lastLogin; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        
        public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
        public void setActive(boolean active) { this.active = active; }
        
        @Override
        public String toString() {
            return String.format("User{email='%s', fullName='%s', active=%s, lastLogin=%s}", 
                               email, fullName, active, lastLogin);
        }
    }
    
    /**
     * Session data class
     */
    public static class Session {
        private String token;
        private String userEmail;
        private LocalDateTime createdAt;
        private LocalDateTime lastAccessed;
        private boolean rememberMe;
        private static final long SESSION_TIMEOUT_MINUTES = 30;
        private static final long REMEMBER_ME_TIMEOUT_DAYS = 30;
        
        public Session(String token, String userEmail, boolean rememberMe) {
            this.token = token;
            this.userEmail = userEmail;
            this.rememberMe = rememberMe;
            this.createdAt = LocalDateTime.now();
            this.lastAccessed = LocalDateTime.now();
        }
        
        public boolean isExpired() {
            LocalDateTime now = LocalDateTime.now();
            if (rememberMe) {
                return createdAt.plusDays(REMEMBER_ME_TIMEOUT_DAYS).isBefore(now);
            } else {
                return lastAccessed.plusMinutes(SESSION_TIMEOUT_MINUTES).isBefore(now);
            }
        }
        
        public void updateLastAccessed() {
            this.lastAccessed = LocalDateTime.now();
        }
        
        // Getters
        public String getToken() { return token; }
        public String getUserEmail() { return userEmail; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastAccessed() { return lastAccessed; }
        public boolean isRememberMe() { return rememberMe; }
    }
    
    /**
     * Login result class
     */
    public static class LoginResult {
        private boolean success;
        private String message;
        private String sessionToken;
        private User user;
        
        public LoginResult(boolean success, String message, String sessionToken, User user) {
            this.success = success;
            this.message = message;
            this.sessionToken = sessionToken;
            this.user = user;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getSessionToken() { return sessionToken; }
        public User getUser() { return user; }
        
        @Override
        public String toString() {
            return String.format("LoginResult{success=%s, message='%s', sessionToken='%s'}", 
                               success, message, sessionToken != null ? sessionToken.substring(0, 10) + "..." : null);
        }
    }
    
    /**
     * Validation result class
     */
    public static class ValidationResult {
        private boolean valid;
        private String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}
