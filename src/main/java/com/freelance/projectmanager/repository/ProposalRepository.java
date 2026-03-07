package com.freelance.projectmanager.repository;

import com.freelance.projectmanager.model.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    List<Proposal> findByFreelancerEmail(String email);
    List<Proposal> findByClientEmail(String email);

    @Query(value = "SELECT COUNT(*) FROM proposals WHERE freelancer_email = :email AND created_at >= :startDate", nativeQuery = true)
    long countBidsInLastWeek(@Param("email") String email, @Param("startDate") LocalDateTime startDate);
}