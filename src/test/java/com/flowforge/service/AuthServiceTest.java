package com.flowforge.service;

import com.flowforge.exception.AuthenticationException;
import com.flowforge.model.User;
import com.flowforge.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests registration/login rules and secure salted hashing in {@link AuthService}. */
class AuthServiceTest {

    private static class InMemoryUserRepository implements UserRepository {
        private final Map<String, UserRecord> users = new LinkedHashMap<>();

        @Override
        public boolean hasUsers() {
            return !users.isEmpty();
        }

        @Override
        public Optional<UserRecord> findByUsername(String username) {
            return Optional.ofNullable(users.get(username));
        }

        @Override
        public User create(String username, String passwordHash, String salt) {
            User user = new User(username, Instant.now());
            users.put(username, new UserRecord(user, passwordHash, salt));
            return user;
        }
    }

    private InMemoryUserRepository repository;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
        auth = new AuthService(repository);
    }

    @Test
    void registerCreatesUserAndNormalizesName() throws Exception {
        User user = auth.register("  Mahir_1  ", "secret1".toCharArray());
        assertEquals("mahir_1", user.getUsername());
        assertTrue(auth.hasUsers());
    }

    @Test
    void loginReturnsRegisteredUser() throws Exception {
        auth.register("mahir", "secret1".toCharArray());
        assertEquals("mahir", auth.login("MAHIR", "secret1".toCharArray()).getUsername());
    }

    @Test
    void wrongPasswordFails() throws Exception {
        auth.register("mahir", "secret1".toCharArray());
        assertThrows(AuthenticationException.class,
                () -> auth.login("mahir", "wronggg".toCharArray()));
    }

    @Test
    void duplicateUserRejected() throws Exception {
        auth.register("mahir", "secret1".toCharArray());
        assertThrows(AuthenticationException.class,
                () -> auth.register("MAHIR", "secret2".toCharArray()));
    }

    @Test
    void invalidUsernameAndShortPasswordRejected() {
        assertThrows(AuthenticationException.class,
                () -> auth.register("1bad", "secret1".toCharArray()));
        assertThrows(AuthenticationException.class,
                () -> auth.register("valid", "123".toCharArray()));
    }

    @Test
    void samePasswordGetsDifferentSaltedHashes() throws Exception {
        auth.register("alice", "secret1".toCharArray());
        auth.register("bob", "secret1".toCharArray());
        UserRepository.UserRecord alice = repository.findByUsername("alice").orElseThrow();
        UserRepository.UserRecord bob = repository.findByUsername("bob").orElseThrow();
        assertNotEquals(alice.getSalt(), bob.getSalt());
        assertNotEquals(alice.getPasswordHash(), bob.getPasswordHash());
        assertFalse(alice.getPasswordHash().contains("secret1"));
    }
}
