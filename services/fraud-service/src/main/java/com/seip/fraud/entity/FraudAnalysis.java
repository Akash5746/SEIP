package com.seip.fraud.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fraud_analysis", schema = "fraud")
public class FraudAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_id", unique = true, nullable = false)
    private Long expenseId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "is_duplicate", nullable = false)
    private boolean isDuplicate;

    @CreationTimestamp
    @Column(name = "analysis_time", nullable = false, updatable = false)
    private LocalDateTime analysisTime;

    @Column(name = "ml_fraud_probability", precision = 5, scale = 4)
    private BigDecimal mlFraudProbability;

    @Column(name = "analyst_notes", columnDefinition = "TEXT")
    private String analystNotes;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<FraudFlag> flags = new ArrayList<>();
}
