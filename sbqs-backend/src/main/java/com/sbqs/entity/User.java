package com.sbqs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String fullName;

    @Column(unique = true)
    private String email;

    @JsonIgnore
    private String passwordHash;

    @JsonIgnore
    @Column(name = "keycloak_user_id")
    private String keycloakUserId;

    @Column(name = "identity_provider")
    private String identityProvider;

    private String phone;

    private String role;

    private String status;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @JsonIgnore
    @OneToOne(mappedBy = "user")
    private CustomerProfile customerProfile;

    @PrePersist
    public void prePersist() {

        createdAt = LocalDateTime.now();

        if (status == null) {
            status = "ACTIVE";
        }
    }
}
