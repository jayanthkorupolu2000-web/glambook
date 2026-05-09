package com.salon.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "SalonOwner")
public class SalonOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "salon_name")
    private String salonName;

    private String city;

    private String email;

    private String password;

    private String phone;
}
