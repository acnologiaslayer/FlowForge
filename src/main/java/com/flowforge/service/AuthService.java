package com.flowforge.service;

import com.flowforge.exception.AuthenticationException;
import com.flowforge.exception.PersistenceException;
import com.flowforge.model.User;
import com.flowforge.persistence.UserRepository;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Application service for registering and authenticating users.
 * <p>
 * Passwords are never stored directly: each password is hashed with PBKDF2 and
 * a per-user random salt, then compared with a constant-time equality check.
 */
public class AuthService {

    private static final int SALT_BYTES = 16;
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;

    private final UserRepository repository;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository repository) {
        this.repository = repository;
    }

    public boolean hasUsers() throws PersistenceException {
        return repository.hasUsers();
    }

    public User register(String username, char[] password)
            throws AuthenticationException, PersistenceException {
        String normalized = normalizeAndValidateUsername(username);
        validatePassword(password);
        if (repository.findByUsername(normalized).isPresent()) {
            throw new AuthenticationException("Username '" + normalized + "' is already taken.");
        }
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        String saltText = Base64.getEncoder().encodeToString(salt);
        String hash = hash(password, saltText);
        return repository.create(normalized, hash, saltText);
    }

    public User login(String username, char[] password)
            throws AuthenticationException, PersistenceException {
        String normalized = normalizeAndValidateUsername(username);
        Optional<UserRepository.UserRecord> record = repository.findByUsername(normalized);
        if (record.isEmpty()) {
            throw new AuthenticationException("Unknown username or password.");
        }
        String attempted = hash(password, record.get().getSalt());
        if (!MessageDigest.isEqual(attempted.getBytes(), record.get().getPasswordHash().getBytes())) {
            throw new AuthenticationException("Unknown username or password.");
        }
        return record.get().getUser();
    }

    private static String normalizeAndValidateUsername(String username) throws AuthenticationException {
        String normalized = username == null ? "" : username.trim().toLowerCase();
        if (!normalized.matches("[a-z][a-z0-9_]{2,31}")) {
            throw new AuthenticationException(
                    "Username must be 3-32 characters: lowercase letters, digits and underscores, starting with a letter.");
        }
        return normalized;
    }

    private static void validatePassword(char[] password) throws AuthenticationException {
        if (password == null || password.length < 6) {
            throw new AuthenticationException("Password must be at least 6 characters.");
        }
    }

    private static String hash(char[] password, String saltText) throws AuthenticationException {
        try {
            byte[] salt = Base64.getDecoder().decode(saltText);
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return Base64.getEncoder().encodeToString(factory.generateSecret(spec).getEncoded());
        } catch (Exception e) {
            throw new AuthenticationException("Could not hash password.", e);
        }
    }
}
