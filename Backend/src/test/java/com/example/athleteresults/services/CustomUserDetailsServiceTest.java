package com.example.athleteresults.services;

import com.example.athleteresults.entities.User;
import com.example.athleteresults.repositories.UserRepository;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {

    private final UserRepository userRepo = mock(UserRepository.class);
    private final CustomUserDetailsService service =
            new CustomUserDetailsService(userRepo);

    /* =====================================================
       USER EXISTS
    ===================================================== */
    @Test
    void loadUserByUsername_userExists_shouldReturnUser() {
        User user = new User();
        user.setUsername("john");
        user.setPassword("encoded");

        when(userRepo.findByUsername("john"))
                .thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("john");

        assertNotNull(result);
        assertEquals("john", result.getUsername());
        assertEquals("encoded", result.getPassword());
    }

    /* =====================================================
       USER DOES NOT EXIST
    ===================================================== */
    @Test
    void loadUserByUsername_userNotFound_shouldThrow() {
        when(userRepo.findByUsername("missing"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing")
        );
    }
}
