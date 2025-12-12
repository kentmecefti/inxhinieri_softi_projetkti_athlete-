package com.example.athleteresults.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "athletes")
public class Athlete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "athlete_id")
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column
    private String lastname;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "birth_date")
    private java.sql.Date birthDate;

    @Column
    private Integer age;

    @Column(name = "ath_weight")
    private Double athWeight;

    @Column(name = "ath_height")
    private Double athHeight;

    @Column
    private String category;

    @Column(name = "performance")
    private String performance;

    @Column
    private String club;

    @Column
    private String country;

    @Column
    private String city;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "user_id")
    private Integer userId;

    //Many-to-Many relation me trajneret
    @ManyToMany(mappedBy = "athletes")
    @JsonIgnore
    private Set<Coach> coaches = new HashSet<>();

    //Relation me CoachAthleteRelation
    @OneToMany(mappedBy = "athlete", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<CoachAthleteRelation> coachAthleteRelations = new HashSet<>();

    //Konstruktoret
    public Athlete() {}

    public Athlete(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Athlete(Integer id, Double athWeight, String name, Integer userId, Set<Coach> coaches) {
        this.id = id;
        this.athWeight = athWeight;
        this.name = name;
        this.userId = userId;
        this.coaches = coaches;
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

    public java.sql.Date getBirthDate() { return birthDate; }
    public void setBirthDate(java.sql.Date birthDate) { this.birthDate = birthDate; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public Double getAthWeight() { return athWeight; }
    public void setAthWeight(Double athWeight) { this.athWeight = athWeight; }

    public Double getAthHeight() { return athHeight; }
    public void setAthHeight(Double athHeight) { this.athHeight = athHeight; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPerformance() { return performance; }
    public void setPerformance(String performance) { this.performance = performance; }

    public String getClub() { return club; }
    public void setClub(String club) { this.club = club; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Set<Coach> getCoaches() { return coaches; }
    public void setCoaches(Set<Coach> coaches) { this.coaches = coaches; }

    public Set<CoachAthleteRelation> getCoachAthleteRelations() { return coachAthleteRelations; }
    public void setCoachAthleteRelations(Set<CoachAthleteRelation> coachAthleteRelations) { this.coachAthleteRelations = coachAthleteRelations; }

    //Lidh atletin me nje user te ruajtur
    public void setUser(User savedUser) {
        if (savedUser != null) {
            this.userId = savedUser.getId();
        }
    }
}
