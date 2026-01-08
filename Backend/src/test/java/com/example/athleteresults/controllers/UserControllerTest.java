package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.*;
import com.example.athleteresults.repositories.*;
import com.example.athleteresults.security.JwtAuthFilter;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /* ===== MOCK REPOSITORIES ===== */
    @MockBean private UserRepository userRepo;
    @MockBean private RoleRepository roleRepo;
    @MockBean private AthleteRepository athleteRepo;
    @MockBean private CoachRepository coachRepo;

    /* =====================================================
       GET /api/users  (ADMIN)
    ===================================================== */
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAllUsers_admin_shouldReturnList() throws Exception {
        User u = sampleUser();

        Mockito.when(userRepo.findAll()).thenReturn(List.of(u));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("john"));
    }

    /* =====================================================
       GET /api/users/{id}  (SELF)
    ===================================================== */
    @Test
    @WithMockUser(username = "john", roles = "ATHLETE")
    void getUser_self_shouldReturnUser() throws Exception {
        User u = sampleUser();
        u.setId(1);

        Mockito.when(userRepo.findByUsername("john"))
                .thenReturn(Optional.of(u));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    /* =====================================================
       GET /api/users/{id}  (FORBIDDEN)
    ===================================================== */
    @Test
    @WithMockUser(username = "john", roles = "ATHLETE")
    void getUser_other_shouldBeForbidden() throws Exception {
        User u = sampleUser();
        u.setId(1);

        Mockito.when(userRepo.findByUsername("john"))
                .thenReturn(Optional.of(u));

        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isForbidden());
    }

    /* =====================================================
       POST /api/users  (ADMIN)
    ===================================================== */
    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_shouldReturnCreated() throws Exception {
        User u = sampleUser();
        u.setId(10);
        u.setRoles(Set.of());

        Mockito.when(userRepo.existsByUsername("john")).thenReturn(false);
        Mockito.when(userRepo.save(Mockito.any(User.class))).thenReturn(u);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(u)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    /* =====================================================
       PUT /api/users/{id}
    ===================================================== */
    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_shouldReturnUpdated() throws Exception {
        User u = sampleUser();
        u.setId(1);

        Mockito.when(userRepo.findById(1)).thenReturn(Optional.of(u));
        Mockito.when(userRepo.save(Mockito.any(User.class))).thenReturn(u);

        u.setUsername("updated");

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updated"));
    }

    /* =====================================================
       DELETE /api/users/{id}
    ===================================================== */
    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_shouldReturnOk() throws Exception {
        User u = sampleUser();
        u.setId(1);
        u.setRoles(Set.of(new Role("ATHLETE", u)));

        Mockito.when(userRepo.findById(1)).thenReturn(Optional.of(u));
        Mockito.when(roleRepo.findByUserId(1)).thenReturn(List.of());

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("deleted")));
    }

    /* =====================================================
       GET /api/users/by-username/{username}
    ===================================================== */
    @Test
    @WithMockUser
    void getByUsername_shouldReturnUser() throws Exception {
        User u = sampleUser();

        Mockito.when(userRepo.findByUsername("john"))
                .thenReturn(Optional.of(u));

        mockMvc.perform(get("/api/users/by-username/john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    /* =====================================================
       PUT /api/users/{id}/activate
    ===================================================== */
    @Test
    @WithMockUser(roles = "ADMIN")
    void activateUser_shouldReturnOk() throws Exception {
        User u = sampleUser();
        u.setId(1);

        Mockito.when(userRepo.findById(1)).thenReturn(Optional.of(u));
        Mockito.when(userRepo.save(Mockito.any(User.class))).thenReturn(u);

        mockMvc.perform(put("/api/users/1/activate"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("activated")));
    }

    /* =====================================================
       ===== HELPER =====
    ===================================================== */
    private User sampleUser() {
        User u = new User();
        u.setId(1);
        u.setUsername("john");
        u.setEmail("john@test.com");
        u.setPassword("secret");
        u.setActive(true);
        return u;
    }
}
