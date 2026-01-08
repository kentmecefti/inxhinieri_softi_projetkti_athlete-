package com.example.athleteresults.security;

import com.example.athleteresults.controllers.AuthController;
import com.example.athleteresults.entities.*;
import com.example.athleteresults.repositories.*;
import com.example.athleteresults.dto.ChangePasswordRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /* ===== MOCKED BEANS ===== */
    @MockBean private AuthenticationManager authManager;
    @MockBean private UserRepository userRepo;
    @MockBean private RoleRepository roleRepo;
    @MockBean private AthleteRepository athleteRepo;
    @MockBean private CoachRepository coachRepo;
    @MockBean private PasswordEncoder encoder;
    @MockBean private JwtService jwtService;

    /* =====================================================
       REGISTER
    ===================================================== */

    @Test
    void registerAthlete_success() throws Exception {
        AuthController.RegistrationRequest req = new AuthController.RegistrationRequest();
        req.setUsername("john");
        req.setEmail("john@test.com");
        req.setPassword("pass");
        req.setRole("ATHLETE");

        Mockito.when(userRepo.existsByUsername("john")).thenReturn(false);
        Mockito.when(encoder.encode("pass")).thenReturn("ENCODED");
        Mockito.when(userRepo.save(Mockito.any(User.class)))
                .thenAnswer(i -> {
                    User u = i.getArgument(0);
                    u.setId(1);
                    return u;
                });

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ATHLETE")));
    }

    @Test
    void registerDuplicateUsername_shouldFail() throws Exception {
        AuthController.RegistrationRequest req = new AuthController.RegistrationRequest();
        req.setUsername("john");

        Mockito.when(userRepo.existsByUsername("john")).thenReturn(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    /* =====================================================
       LOGIN
    ===================================================== */

    @Test
    void login_success() throws Exception {
        User u = activeUser();

        Mockito.when(authManager.authenticate(Mockito.any()))
                .thenReturn(Mockito.mock(Authentication.class));

        Mockito.when(userRepo.findByUsername("john"))
                .thenReturn(Optional.of(u));

        Mockito.when(jwtService.generateToken("john"))
                .thenReturn("JWT_TOKEN");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username":"john", "password":"pass" }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("JWT_TOKEN"));
    }

    @Test
    void login_inactiveUser_shouldFail() throws Exception {
        User u = activeUser();
        u.setActive(false);

        Mockito.when(authManager.authenticate(Mockito.any()))
                .thenReturn(Mockito.mock(Authentication.class));

        Mockito.when(userRepo.findByUsername("john"))
                .thenReturn(Optional.of(u));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username":"john", "password":"pass" }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_badCredentials_shouldFail() throws Exception {
        Mockito.when(authManager.authenticate(Mockito.any()))
                .thenThrow(new BadCredentialsException("Bad"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username":"john", "password":"wrong" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    /* =====================================================
       CHANGE PASSWORD
    ===================================================== */

    @Test
    @WithMockUser(username = "john")
    void changePassword_success() throws Exception {
        User u = activeUser();

        Mockito.when(userRepo.findByUsername("john"))
                .thenReturn(Optional.of(u));

        Mockito.when(encoder.matches("old", "ENCODED"))
                .thenReturn(true);

        Mockito.when(encoder.encode("new"))
                .thenReturn("NEW_ENCODED");

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("old");
        req.setNewPassword("new");

        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("successfully")));
    }

    @Test
    @WithMockUser(username = "john")
    void changePassword_wrongCurrent_shouldFail() throws Exception {
        User u = activeUser();

        Mockito.when(userRepo.findByUsername("john"))
                .thenReturn(Optional.of(u));

        Mockito.when(encoder.matches(Mockito.any(), Mockito.any()))
                .thenReturn(false);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("wrong");
        req.setNewPassword("new");

        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    /* =====================================================
       HELPER
    ===================================================== */
    private User activeUser() {
        User u = new User();
        u.setId(1);
        u.setUsername("john");
        u.setPassword("ENCODED");
        u.setActive(true);
        return u;
    }
}
