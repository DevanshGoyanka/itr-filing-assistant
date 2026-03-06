package com.itr.repository;

import com.itr.entity.ClientYearData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClientYearDataRepository extends JpaRepository<ClientYearData, Long> {

    List<ClientYearData> findByClientId(Long clientId);

    Optional<ClientYearData> findByClientIdAndAssessmentYear(Long clientId, String assessmentYear);

    boolean existsByClientIdAndAssessmentYear(Long clientId, String assessmentYear);
}
