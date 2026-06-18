package com.seip.fraud.repository;

import com.seip.fraud.entity.FraudAnalysis;
import com.seip.fraud.entity.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FraudAnalysisRepository extends JpaRepository<FraudAnalysis, Long> {

    Optional<FraudAnalysis> findByExpenseId(Long expenseId);

    boolean existsByExpenseId(Long expenseId);

    Page<FraudAnalysis> findByRiskLevel(RiskLevel riskLevel, Pageable pageable);

    long countByRiskLevel(RiskLevel riskLevel);

    long countByIsDuplicateTrue();

    @Query("SELECT COUNT(a) FROM FraudAnalysis a WHERE a.riskLevel = 'HIGH' OR a.riskLevel = 'MEDIUM'")
    long countHighAndMediumRisk();
}
