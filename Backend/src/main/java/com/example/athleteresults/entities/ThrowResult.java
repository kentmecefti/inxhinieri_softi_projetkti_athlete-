package com.example.athleteresults.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "throw_results")
public class ThrowResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "throw_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @Column(name = "throw_date", nullable = false)
    private LocalDate throwDate;

    @Column(name = "throw_type", nullable = false)
    private String throwType;

    @Column(nullable = false)
    private String event;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal distance;

    private BigDecimal wind;

    private String notes;

    @Column(name = "throw_style", length = 100)
    private String throwStyle;

    public ThrowResult() {}

    public ThrowResult(Athlete athlete, LocalDate throwDate, String throwType,
                       String event, BigDecimal distance, BigDecimal wind,
                       String notes, String throwStyle) {
        this.athlete = athlete;
        this.throwDate = throwDate;
        this.throwType = throwType;
        this.event = event;
        this.distance = distance;
        this.wind = wind;
        this.notes = notes;
        this.throwStyle = throwStyle;
    }

    //Getters dhe Setters
    public Integer getId() { return id; }

    public Athlete getAthlete() { return athlete; }
    public void setAthlete(Athlete athlete) { this.athlete = athlete; }

    public LocalDate getThrowDate() { return throwDate; }
    public void setThrowDate(LocalDate throwDate) { this.throwDate = throwDate; }

    public String getThrowType() { return throwType; }
    public void setThrowType(String throwType) { this.throwType = throwType; }

    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    public BigDecimal getDistance() { return distance; }
    public void setDistance(BigDecimal distance) { this.distance = distance; }

    public BigDecimal getWind() { return wind; }
    public void setWind(BigDecimal wind) { this.wind = wind; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getThrowStyle() { return throwStyle; }
    public void setThrowStyle(String throwStyle) { this.throwStyle = throwStyle; }
}
