package com.seip.fraud.repository;

import com.seip.fraud.entity.FlagType;
import com.seip.fraud.entity.FraudFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudFlagRepository extends JpaRepository<FraudFlag, Long> {

    List<FraudFlag> findByAnalysisId(Long analysisId);

    long countByFlagType(FlagType flagType);
}
