package com.flashsale.repository;

import com.flashsale.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_existingUser_returnsUser() {
        User persisted = em.persistAndFlush(user("alice@example.com"));

        Optional<User> found = userRepository.findByEmail("alice@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(persisted.getId());
    }

    @Test
    void findByEmail_unknownEmail_returnsEmpty() {
        assertThat(userRepository.findByEmail("ghost@example.com")).isEmpty();
    }

    @Test
    void save_persistsAllFields() {
        User saved = userRepository.save(user("bob@example.com"));

        User found = em.find(User.class, saved.getId());
        assertThat(found.getEmail()).isEqualTo("bob@example.com");
        assertThat(found.getPhone()).isEqualTo("555-0000");
        assertThat(found.getBalance()).isEqualByComparingTo("100.00");
    }

    private User user(String email) {
        return User.builder()
                .email(email)
                .phone("555-0000")
                .passwordHash("hashed")
                .balance(new BigDecimal("100.00"))
                .build();
    }
}
