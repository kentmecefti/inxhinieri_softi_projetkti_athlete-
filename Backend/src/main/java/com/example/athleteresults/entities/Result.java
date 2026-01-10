package com.example.athleteresults.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "results")
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Integer id;

    @Column(name = "athlete_id")
    private Integer athleteId;

    private String race;

    @Column(name = "race_type")
    private String raceType;

    @Column(name = "race_date")
    private LocalDate raceDate;

    private Integer distance;

    @Column(name = "time_ms")
    private Integer timeMs;

    //  New optional weight column
    @Column(name = "weight", nullable = true)
    private Integer weight;

    @Column(name = "notes")
    private String notes;  //  new field added

    // 🔹 Constructors
    public Result() {
    }

    public Result(Integer id, Integer athleteId, String race, String raceType,
                  LocalDate raceDate, Integer distance, Integer timeMs, Integer weight,String notes) {
        this.id = id;
        this.athleteId = athleteId;
        this.race = race;
        this.raceType = raceType;
        this.raceDate = raceDate;
        this.distance = distance;
        this.timeMs = timeMs;
        this.weight = weight;
        this.notes = notes;
    }

    // 🔹 Getters & Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAthleteId() {
        return athleteId;
    }

    public void setAthleteId(Integer athleteId) {
        this.athleteId = athleteId;
    }

    public String getRace() {
        return race;
    }

    public void setRace(String race) {
        this.race = race;
    }

    public String getRaceType() {
        return raceType;
    }

    public void setRaceType(String raceType) {
        this.raceType = raceType;
    }

    public LocalDate getRaceDate() {
        return raceDate;
    }

    public void setRaceDate(LocalDate raceDate) {
        this.raceDate = raceDate;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    public Integer getTimeMs() {
        return timeMs;
    }

    public void setTimeMs(Integer timeMs) {
        this.timeMs = timeMs;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}


