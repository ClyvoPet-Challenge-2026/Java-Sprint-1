package br.com.fiap.ClyvoCareAPI.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@Builder
@Table(name = "TB_CAD_PLAN")
@AllArgsConstructor
@NoArgsConstructor
public class Plan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyValue;
}
