package com.example.athleteresults.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) 
@Entity
@Table(name = "weight_metrics")
public class WeightMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metric_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    @JsonIgnoreProperties({"weightMetrics", "plyoMetrics", "reflexMetrics", "athlete"})
    private GymSession gymSession;

    @Column
    private Integer sets;

    @Column
    private Integer reps;

    @Column(name = "weight_gym")
    private Double weightGym;

    //Getters dhe Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public GymSession getGymSession() { return gymSession; }
    public void setGymSession(GymSession gymSession) { this.gymSession = gymSession; }

    public Integer getSets() { return sets; }
    public void setSets(Integer sets) { this.sets = sets; }

    public Integer getReps() { return reps; }
    public void setReps(Integer reps) { this.reps = reps; }

    public Double getWeightGym() { return weightGym; }
    public void setWeightGym(Double weightGym) { this.weightGym = weightGym; }
}
