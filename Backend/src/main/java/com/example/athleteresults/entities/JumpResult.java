package com.example.athleteresults.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "jump_results")
public class JumpResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "jump_id")
    private Integer jumpId;

    @Column(name = "athlete_id", nullable = false)
    private Integer athleteId;

    @Column(name = "jump_date", nullable = false)
    private LocalDate jumpDate;

    @Column(name = "jump_type", nullable = false)
    private String jumpType;

    @Column(name = "detail")
    private String detail;

    @Column(name = "distance_m", nullable = false)
    private Double distanceM;

    @Column(name = "notes")
    private String notes;

    public JumpResult() {}

    public JumpResult(Integer athleteId, LocalDate jumpDate, String jumpType,
                      String detail, Double distanceM, String notes) {
        this.athleteId = athleteId;
        this.jumpDate = jumpDate;
        this.jumpType = jumpType;
        this.detail = detail;
        this.distanceM = distanceM;
        this.notes = notes;
    }

    public Integer getJumpId() { return jumpId; }
    public void setJumpId(Integer jumpId) { this.jumpId = jumpId; }

    public Integer getAthleteId() { return athleteId; }
    public void setAthleteId(Integer athleteId) { this.athleteId = athleteId; }

    public LocalDate getJumpDate() { return jumpDate; }
    public void setJumpDate(LocalDate jumpDate) { this.jumpDate = jumpDate; }

    public String getJumpType() { return jumpType; }
    public void setJumpType(String jumpType) { this.jumpType = jumpType; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public Double getDistanceM() { return distanceM; }
    public void setDistanceM(Double distanceM) { this.distanceM = distanceM; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
