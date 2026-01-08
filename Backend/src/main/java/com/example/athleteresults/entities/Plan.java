package com.example.athleteresults.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "plan")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "coach_id", nullable = true)
    private Coach coach;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(name = "prediction_plan")
    private String predictionPlan;

    @Column(name = "actual_plan")
    private String actualPlan;

    @Column(name = "notes")
    private String notes;

    //Konstruktoret
    public Plan() {}

    public Plan(Athlete athlete, Coach coach, LocalDate planDate, String predictionPlan, String actualPlan, String notes) {
        this.athlete = athlete;
        this.coach = coach;
        this.planDate = planDate;
        this.predictionPlan = predictionPlan;
        this.actualPlan = actualPlan;
        this.notes = notes;
    }

    //Getters dhe Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Athlete getAthlete() { return athlete; }
    public void setAthlete(Athlete athlete) { this.athlete = athlete; }

    public Coach getCoach() { return coach; }
    public void setCoach(Coach coach) { this.coach = coach; }

    public LocalDate getPlanDate() { return planDate; }
    public void setPlanDate(LocalDate planDate) { this.planDate = planDate; }

    public String getPredictionPlan() { return predictionPlan; }
    public void setPredictionPlan(String predictionPlan) { this.predictionPlan = predictionPlan; }

    public String getActualPlan() { return actualPlan; }
    public void setActualPlan(String actualPlan) { this.actualPlan = actualPlan; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
