package com.flowforge.persistence;

import com.flowforge.exception.PersistenceException;
import com.flowforge.model.User;

import java.util.Optional;

/** Storage abstraction for FlowForge accounts. */
public interface UserRepository {

    boolean hasUsers() throws PersistenceException;

    Optional<UserRecord> findByUsername(String username) throws PersistenceException;

    User create(String username, String passwordHash, String salt) throws PersistenceException;

    /** Stored credential row for authentication. */
    class UserRecord {
        private final User user;
        private final String passwordHash;
        private final String salt;

        public UserRecord(User user, String passwordHash, String salt) {
            this.user = user;
            this.passwordHash = passwordHash;
            this.salt = salt;
        }

        public User getUser() {
            return user;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public String getSalt() {
            return salt;
        }
    }
}
