package com.example.athleteresults.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "plyo_metrics")
public class PlyoMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metric_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    @JsonIgnoreProperties({"weightMetrics", "plyoMetrics", "reflexMetrics", "athlete"})
    private GymSession gymSession;

    @Column
    private Integer contacts;

    @Column
    private Double height;

    @Column
    private Integer intensity;

    // ===== GETTERS & SETTERS =====
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public GymSession getGymSession() { return gymSession; }
    public void setGymSession(GymSession gymSession) { this.gymSession = gymSession; }

    public Integer getContacts() { return contacts; }
    public void setContacts(Integer contacts) { this.contacts = contacts; }

    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }

    public Integer getIntensity() { return intensity; }
    public void setIntensity(Integer intensity) { this.intensity = intensity; }
}
