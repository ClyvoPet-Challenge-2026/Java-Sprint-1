package br.com.fiap.ClyvoCareAPI.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.type.YesNoConverter;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@Table(name = "TB_CAD_OWNER")
@AllArgsConstructor
@NoArgsConstructor
public class Owner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private City city;

    private String name;

    private String cpf;

    private String email;

    @JsonIgnore
    private String passwordHash;

    @Column(name = "ROLE_NAME", nullable = false)
    private String roleName;

    @Column(name = "ENABLED", nullable = false)
    @Convert(converter = YesNoConverter.class)
    private boolean enabled;

    private String phone;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
