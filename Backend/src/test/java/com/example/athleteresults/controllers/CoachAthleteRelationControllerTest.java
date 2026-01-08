package com.example.athleteresults.controllers;

import com.example.athleteresults.entities.*;
import com.example.athleteresults.repositories.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CoachAthleteRelationControllerTest {

    private CoachRepository coachRepo;
    private AthleteRepository athleteRepo;
    private CoachAthleteRelationRepository relationRepo;
    private StatusRepository statusRepo;
    private UserRepository userRepo;

    private CoachAthleteRelationController controller;

    @BeforeEach
    void setUp() {
        coachRepo = mock(CoachRepository.class);
        athleteRepo = mock(AthleteRepository.class);
        relationRepo = mock(CoachAthleteRelationRepository.class);
        statusRepo = mock(StatusRepository.class);
        userRepo = mock(UserRepository.class);

        controller = new CoachAthleteRelationController(
                coachRepo, athleteRepo, relationRepo, statusRepo, userRepo
        );
    }

    /* =========================================================
       SEND REQUEST
    ========================================================= */

    @Test
    void sendRequest_unauthorized_whenUserDetailsNull() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.sendRequest(1, 2, null)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void sendRequest_userNotFound() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("kent");
        when(userRepo.findByUsername("kent")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.sendRequest(1, 2, ud)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("User not found"));
    }

    @Test
    void sendRequest_coachNotFound() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("kent");

        User user = new User();
        user.setId(99);

        when(userRepo.findByUsername("kent")).thenReturn(Optional.of(user));
        when(coachRepo.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.sendRequest(1, 2, ud)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Coach not found"));
    }

    @Test
    void sendRequest_forbidden_whenCoachNotOwnedByLoggedUser() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("kent");

        User user = new User();
        user.setId(99);

        Coach coach = new Coach();
        coach.setId(1);
        coach.setUserId(123); // different

        when(userRepo.findByUsername("kent")).thenReturn(Optional.of(user));
        when(coachRepo.findById(1)).thenReturn(Optional.of(coach));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.sendRequest(1, 2, ud)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().toLowerCase().contains("doesn't match"));
    }

    @Test
    void sendRequest_athleteNotFound() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("kent");

        User user = new User();
        user.setId(99);

        Coach coach = new Coach();
        coach.setId(1);
        coach.setUserId(99); // owned

        when(userRepo.findByUsername("kent")).thenReturn(Optional.of(user));
        when(coachRepo.findById(1)).thenReturn(Optional.of(coach));
        when(athleteRepo.findById(2)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.sendRequest(1, 2, ud)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Athlete not found"));
    }

    @Test
    void sendRequest_duplicateRelation_returnsBadRequestResponse() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("kent");

        User user = new User();
        user.setId(99);

        Coach coach = new Coach();
        coach.setId(1);
        coach.setUserId(99);

        Athlete athlete = new Athlete();
        athlete.setId(2);

        when(userRepo.findByUsername("kent")).thenReturn(Optional.of(user));
        when(coachRepo.findById(1)).thenReturn(Optional.of(coach));
        when(athleteRepo.findById(2)).thenReturn(Optional.of(athlete));

        when(relationRepo.findByCoachAndAthlete(coach, athlete))
                .thenReturn(Optional.of(new CoachAthleteRelation()));

        ResponseEntity<String> res = controller.sendRequest(1, 2, ud);

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertTrue(res.getBody().toLowerCase().contains("already exists"));
        verify(relationRepo, never()).save(any());
    }

    @Test
    void sendRequest_success_createsPendingStatusAndSavesRelation() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("kent");

        User user = new User();
        user.setId(99);

        Coach coach = new Coach();
        coach.setId(1);
        coach.setUserId(99);

        Athlete athlete = new Athlete();
        athlete.setId(2);

        Status pending = new Status("pending");

        when(userRepo.findByUsername("kent")).thenReturn(Optional.of(user));
        when(coachRepo.findById(1)).thenReturn(Optional.of(coach));
        when(athleteRepo.findById(2)).thenReturn(Optional.of(athlete));
        when(relationRepo.findByCoachAndAthlete(coach, athlete)).thenReturn(Optional.empty());

        when(statusRepo.findByStatusName("pending")).thenReturn(Optional.of(pending));

        ResponseEntity<String> res = controller.sendRequest(1, 2, ud);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertTrue(res.getBody().toLowerCase().contains("sent"));

        verify(relationRepo).save(any(CoachAthleteRelation.class));
        verify(statusRepo, never()).save(any(Status.class)); // already existed
    }

    @Test
    void sendRequest_success_createsPendingStatusIfMissing() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("kent");

        User user = new User();
        user.setId(99);

        Coach coach = new Coach();
        coach.setId(1);
        coach.setUserId(99);

        Athlete athlete = new Athlete();
        athlete.setId(2);

        when(userRepo.findByUsername("kent")).thenReturn(Optional.of(user));
        when(coachRepo.findById(1)).thenReturn(Optional.of(coach));
        when(athleteRepo.findById(2)).thenReturn(Optional.of(athlete));
        when(relationRepo.findByCoachAndAthlete(coach, athlete)).thenReturn(Optional.empty());

        when(statusRepo.findByStatusName("pending")).thenReturn(Optional.empty());
        when(statusRepo.save(any(Status.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<String> res = controller.sendRequest(1, 2, ud);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(statusRepo).save(any(Status.class));
        verify(relationRepo).save(any(CoachAthleteRelation.class));
    }

    /* =========================================================
       ACCEPT
    ========================================================= */

    @Test
    void acceptRequest_coachNotFound() {
        when(coachRepo.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.acceptRequest(2, 1)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void acceptRequest_athleteNotFound() {
        Coach coach = new Coach();
        coach.setId(1);

        when(coachRepo.findById(1)).thenReturn(Optional.of(coach));
        when(athleteRepo.findById(2)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.acceptRequest(2, 1)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void acceptRequest_relationNotFound() {
        Coach coach = new Coach(); coach.setId(1);
        Athlete athlete = new Athlete(); athlete.setId(2);

        when(coachRepo.findById(1)).thenReturn(Optional.of(coach));
        when(athleteRepo.findById(2)).thenReturn(Optional.of(athlete));
        when(relationRepo.findByCoachAndAthlete(coach, athlete)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.acceptRequest(2, 1)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void acceptRequest_success_setsAcceptStatus() {
        Coach coach = new Coach(); coach.setId(1);
        Athlete athlete = new Athlete(); athlete.setId(2);
        CoachAthleteRelation rel = new CoachAthleteRelation();
        Status accept = new Status("accept");

        when(coachRepo.findById(1)).thenReturn(Optional.of(coach));
        when(athleteRepo.findById(2)).thenReturn(Optional.of(athlete));
        when(relationRepo.findByCoachAndAthlete(coach, athlete)).thenReturn(Optional.of(rel));

        when(statusRepo.findByStatusName("accept")).thenReturn(Optional.of(accept));

        ResponseEntity<String> res = controller.acceptRequest(2, 1);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertTrue(res.getBody().toLowerCase().contains("accepted"));
        verify(relationRepo).save(rel);
    }

    @Test
    void acceptRequest_success_createsAcceptStatusIfMissing() {
        Coach coach = new Coach(); coach.setId(1);
        Athlete athlete = new Athlete(); athlete.setId(2);
        CoachAthleteRelation rel = new CoachAthleteRelation();

        when(coachRepo.findById(1)).thenReturn(Optional.of(coach));
        when(athleteRepo.findById(2)).thenReturn(Optional.of(athlete));
        when(relationRepo.findByCoachAndAthlete(coach, athlete)).thenReturn(Optional.of(rel));

        when(statusRepo.findByStatusName("accept")).thenReturn(Optional.empty());
        when(statusRepo.save(any(Status.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<String> res = controller.acceptRequest(2, 1);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(statusRepo).save(any(Status.class));
        verify(relationRepo).save(rel);
    }

    /* =========================================================
       REFUSE
    ========================================================= */

    @Test
    void refuseRequest_success_setsRefuseStatus() {
        Coach coach = new Coach(); coach.setId(1);
        Athlete athlete = new Athlete(); athlete.setId(2);
        CoachAthleteRelation rel = new CoachAthleteRelation();
        Status refuse = new Status("refuse");

        when(coachRepo.findById(1)).thenReturn(Optional.of(coach));
        when(athleteRepo.findById(2)).thenReturn(Optional.of(athlete));
        when(relationRepo.findByCoachAndAthlete(coach, athlete)).thenReturn(Optional.of(rel));

        when(statusRepo.findByStatusName("refuse")).thenReturn(Optional.of(refuse));

        ResponseEntity<String> res = controller.refuseRequest(2, 1);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertTrue(res.getBody().toLowerCase().contains("refused"));
        verify(relationRepo).save(rel);
    }

    @Test
    void refuseRequest_success_createsRefuseStatusIfMissing() {
        Coach coach = new Coach(); coach.setId(1);
        Athlete athlete = new Athlete(); athlete.setId(2);
        CoachAthleteRelation rel = new CoachAthleteRelation();

        when(coachRepo.findById(1)).thenReturn(Optional.of(coach));
        when(athleteRepo.findById(2)).thenReturn(Optional.of(athlete));
        when(relationRepo.findByCoachAndAthlete(coach, athlete)).thenReturn(Optional.of(rel));

        when(statusRepo.findByStatusName("refuse")).thenReturn(Optional.empty());
        when(statusRepo.save(any(Status.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<String> res = controller.refuseRequest(2, 1);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(statusRepo).save(any(Status.class));
        verify(relationRepo).save(rel);
    }

    /* =========================================================
       UNLINK
    ========================================================= */

    @Test
    void unlink_unauthorized_whenUserDetailsNull() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.unlinkCoachFromAthlete(1, 2, null)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void unlink_userNotFound() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("kent");
        when(userRepo.findByUsername("kent")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.unlinkCoachFromAthlete(1, 2, ud)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void unlink_athleteNotFound() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("kent");

        User user = new User(); user.setId(99);
        when(userRepo.findByUsername("kent")).thenReturn(Optional.of(user));
        when(athleteRepo.findById(2)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.unlinkCoachFromAthlete(1, 2, ud)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void unlink_forbidden_whenAthleteNotOwnedByLoggedUser() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("kent");

        User user = new User(); user.setId(99);
        Athlete athlete = new Athlete(); athlete.setId(2); athlete.setUserId(123); // different

        when(userRepo.findByUsername("kent")).thenReturn(Optional.of(user));
        when(athleteRepo.findById(2)).thenReturn(Optional.of(athlete));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.unlinkCoachFromAthlete(1, 2, ud)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void unlink_coachNotFound() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("kent");

        User user = new User(); user.setId(99);
        Athlete athlete = new Athlete(); athlete.setId(2); athlete.setUserId(99);

        when(userRepo.findByUsername("kent")).thenReturn(Optional.of(user));
        when(athleteRepo.findById(2)).thenReturn(Optional.of(athlete));
        when(coachRepo.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.unlinkCoachFromAthlete(1, 2, ud)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void unlink_success_deletesRelationIfPresent() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("kent");

        User user = new User(); user.setId(99);
        Athlete athlete = new Athlete(); athlete.setId(2); athlete.setUserId(99);
        Coach coach = new Coach(); coach.setId(1);

        CoachAthleteRelation rel = new CoachAthleteRelation();

        when(userRepo.findByUsername("kent")).thenReturn(Optional.of(user));
        when(athleteRepo.findById(2)).thenReturn(Optional.of(athlete));
        when(coachRepo.findById(1)).thenReturn(Optional.of(coach));
        when(relationRepo.findByCoachAndAthlete(coach, athlete)).thenReturn(Optional.of(rel));

        ResponseEntity<String> res = controller.unlinkCoachFromAthlete(1, 2, ud);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertTrue(res.getBody().toLowerCase().contains("removed"));

        verify(relationRepo).delete(rel);
    }

    @Test
    void unlink_success_okEvenIfRelationMissing() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("kent");

        User user = new User(); user.setId(99);
        Athlete athlete = new Athlete(); athlete.setId(2); athlete.setUserId(99);
        Coach coach = new Coach(); coach.setId(1);

        when(userRepo.findByUsername("kent")).thenReturn(Optional.of(user));
        when(athleteRepo.findById(2)).thenReturn(Optional.of(athlete));
        when(coachRepo.findById(1)).thenReturn(Optional.of(coach));
        when(relationRepo.findByCoachAndAthlete(coach, athlete)).thenReturn(Optional.empty());

        ResponseEntity<String> res = controller.unlinkCoachFromAthlete(1, 2, ud);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(relationRepo, never()).delete(any());
    }
}
