package com.example.athleteresults.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Integer id;

    @Column(name = "athlete_id", nullable = false)
    private Integer athleteId;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Column(name = "time_min", nullable = false, precision = 6, scale = 2)
    private BigDecimal timeMin;

    @Column(name = "distance_km", precision = 6, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "heart_avg")
    private Integer heartAvg;

    @Column(name = "heart_max")
    private Integer heartMax;

    @Column(name = "calories", precision = 8, scale = 2)
    private BigDecimal calories;

    @Column(length = 50)
    private String surface;

    @Column(length = 100)
    private String weather;

    @Column(columnDefinition = "TEXT")
    private String notes;

    //Konstruktoret
    public Session() {}

    public Session(Integer athleteId, LocalDate runDate, BigDecimal timeMin, BigDecimal distanceKm,
                   Integer heartAvg, Integer heartMax, BigDecimal calories, String surface,
                   String weather, String notes) {
        this.athleteId = athleteId;
        this.runDate = runDate;
        this.timeMin = timeMin;
        this.distanceKm = distanceKm;
        this.heartAvg = heartAvg;
        this.heartMax = heartMax;
        this.calories = calories;
        this.surface = surface;
        this.weather = weather;
        this.notes = notes;
    }

    //Getters dhe Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getAthleteId() { return athleteId; }
    public void setAthleteId(Integer athleteId) { this.athleteId = athleteId; }

    public LocalDate getRunDate() { return runDate; }
    public void setRunDate(LocalDate runDate) { this.runDate = runDate; }

    public BigDecimal getTimeMin() { return timeMin; }
    public void setTimeMin(BigDecimal timeMin) { this.timeMin = timeMin; }

    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }

    public Integer getHeartAvg() { return heartAvg; }
    public void setHeartAvg(Integer heartAvg) { this.heartAvg = heartAvg; }

    public Integer getHeartMax() { return heartMax; }
    public void setHeartMax(Integer heartMax) { this.heartMax = heartMax; }

    public BigDecimal getCalories() { return calories; }
    public void setCalories(BigDecimal calories) { this.calories = calories; }

    public String getSurface() { return surface; }
    public void setSurface(String surface) { this.surface = surface; }

    public String getWeather() { return weather; }
    public void setWeather(String weather) { this.weather = weather; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
