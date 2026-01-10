package com.example.athleteresults.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "coach_athlete_relation")
public class CoachAthleteRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== COACH =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id", nullable = false)
    private Coach coach;

    // ===== ATHLETE =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    // ===== STATUS =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    private Status status;

    // ===== Constructors =====
    public CoachAthleteRelation() {}

    public CoachAthleteRelation(Coach coach, Athlete athlete, Status status) {
        this.coach = coach;
        this.athlete = athlete;
        this.status = status;
    }

    // ===== Getters & Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Coach getCoach() { return coach; }
    public void setCoach(Coach coach) { this.coach = coach; }

    public Athlete getAthlete() { return athlete; }
    public void setAthlete(Athlete athlete) { this.athlete = athlete; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
