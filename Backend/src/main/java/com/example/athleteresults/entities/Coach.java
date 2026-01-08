package com.example.athleteresults.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "coaches")
public class Coach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coach_id")
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column
    private String lastname;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column
    private String specialization;

    @Column
    private String phone;

    @Column
    private String club;

    @Column
    private String country;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    //Relation me perdoruesit
    @Column(name = "user_id")
    private Integer userId;

    //Many-to-Many me Athletes
    @ManyToMany
    @JoinTable(
            name = "coach_athlete_relation",
            joinColumns = @JoinColumn(name = "coach_id"),
            inverseJoinColumns = @JoinColumn(name = "athlete_id")
    )
    @JsonIgnore
    private Set<Athlete> athletes = new HashSet<>();

    //Relation Entity
    @OneToMany(mappedBy = "coach", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<CoachAthleteRelation> coachAthleteRelations = new HashSet<>();

    //Konstruktoret
    public Coach() {}

    public Coach(String name, Integer userId) {
        this.name = name;
        this.userId = userId;
    }

    //Getters dhe Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getClub() { return club; }
    public void setClub(String club) { this.club = club; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Set<Athlete> getAthletes() { return athletes; }
    public void setAthletes(Set<Athlete> athletes) { this.athletes = athletes; }

    public Set<CoachAthleteRelation> getCoachAthleteRelations() { return coachAthleteRelations; }
    public void setCoachAthleteRelations(Set<CoachAthleteRelation> coachAthleteRelations) {
        this.coachAthleteRelations = coachAthleteRelations;
    }

    //Linkimi me perdoruesin
    public void setUser(User savedUser) {
        if (savedUser != null) {
            this.userId = savedUser.getId();
        }
    }

    // Metoda per te marre atletet me statusin e tyre
    @Transient
    public Set<Map<String, Object>> getAthletesWithStatus() {
        Set<Map<String, Object>> result = new HashSet<>();
        for (CoachAthleteRelation rel : coachAthleteRelations) {
            Map<String, Object> map = new LinkedHashMap<>();
            Athlete a = rel.getAthlete();
            map.put("id", a.getId());
            map.put("athWeight", a.getAthWeight());
            map.put("name", a.getName());
            map.put("userId", a.getUserId());
            map.put("status", rel.getStatus() != null ? rel.getStatus().getStatusName() : "pending");
            result.add(map);
        }
        return result;
    }
}
