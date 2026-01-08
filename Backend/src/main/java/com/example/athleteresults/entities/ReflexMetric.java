package com.example.athleteresults.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "reflex_metrics")
public class ReflexMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metric_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    @JsonIgnoreProperties({"weightMetrics", "plyoMetrics", "reflexMetrics", "athlete"})
    private GymSession gymSession;

    @Column(name = "reaction_time_ms")
    private Integer reactionTimeMs;

    @Column
    private Integer trials;

    @Column(name = "best_trial_ms")
    private Integer bestTrialMs;

    //Getters dhe Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public GymSession getGymSession() { return gymSession; }
    public void setGymSession(GymSession gymSession) { this.gymSession = gymSession; }

    public Integer getReactionTimeMs() { return reactionTimeMs; }
    public void setReactionTimeMs(Integer reactionTimeMs) { this.reactionTimeMs = reactionTimeMs; }

    public Integer getTrials() { return trials; }
    public void setTrials(Integer trials) { this.trials = trials; }

    public Integer getBestTrialMs() { return bestTrialMs; }
    public void setBestTrialMs(Integer bestTrialMs) { this.bestTrialMs = bestTrialMs; }
}
