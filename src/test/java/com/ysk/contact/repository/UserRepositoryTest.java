package com.ysk.contact.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ysk.contact.entity.User;
import com.ysk.contact.support.IntegrationTest;

class UserRepositoryTest extends IntegrationTest {

    @Autowired
    UserRepository userRepository;

    @Test
    void savesAndFindsByUsername() {
        userRepository.save(User.builder().username("alice").password("hash").build());

        assertThat(userRepository.findByUsername("alice")).isPresent();
        assertThat(userRepository.existsByUsername("alice")).isTrue();
        assertThat(userRepository.existsByUsername("bob")).isFalse();
    }
}
