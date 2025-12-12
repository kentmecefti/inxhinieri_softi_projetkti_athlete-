package com.example.athleteresults.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "gym_sessions")
public class GymSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gym_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_id", nullable = false, referencedColumnName = "athlete_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // <-- IMPORTANT
    private Athlete athlete;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(length = 20)
    private String category;

    @Column(name = "exercise_name", length = 100, nullable = false)
    private String exerciseName;

    @Column(columnDefinition = "TEXT")
    private String notes;

    //Lidhjet
    @OneToMany(mappedBy = "gymSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<WeightMetric> weightMetrics;

    @OneToMany(mappedBy = "gymSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PlyoMetric> plyoMetrics;

    @OneToMany(mappedBy = "gymSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ReflexMetric> reflexMetrics;

    public GymSession() {}

    //Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Athlete getAthlete() { return athlete; }
    public void setAthlete(Athlete athlete) { this.athlete = athlete; }

    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<WeightMetric> getWeightMetrics() { return weightMetrics; }
    public void setWeightMetrics(List<WeightMetric> weightMetrics) { this.weightMetrics = weightMetrics; }

    public List<PlyoMetric> getPlyoMetrics() { return plyoMetrics; }
    public void setPlyoMetrics(List<PlyoMetric> plyoMetrics) { this.plyoMetrics = plyoMetrics; }

    public List<ReflexMetric> getReflexMetrics() { return reflexMetrics; }
    public void setReflexMetrics(List<ReflexMetric> reflexMetrics) { this.reflexMetrics = reflexMetrics; }
}
