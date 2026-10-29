package com.bookmyhive.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Theatre / auditorium where shows take place. */
@Entity
@Table(name = "venues")
@Data
@NoArgsConstructor
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 300)
    private String address;

    @OneToMany(mappedBy = "venue")
    private List<Show> shows = new ArrayList<>();

    public Venue(String name, String city, String address) {
        this.name = name;
        this.city = city;
        this.address = address;
    }
}
