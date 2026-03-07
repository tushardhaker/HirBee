package com.freelance.projectmanager.repository;

import com.freelance.projectmanager.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByClientEmail(String clientEmail);

    @Query("SELECT j FROM Job j WHERE UPPER(j.location) = 'GLOBAL' OR " +
            "LOWER(j.location) LIKE LOWER(CONCAT('%', :city, '%')) OR " +
            "LOWER(:city) LIKE LOWER(CONCAT('%', j.location, '%'))")
    List<Job> findJobsByLocation(@Param("city") String city);

    @Query("SELECT DISTINCT j.category FROM Job j WHERE j.category IS NOT NULL ORDER BY j.category")
    List<String> findDistinctCategories();

    // Optional: Agar direct search karni ho category se
    List<Job> findByCategory(String category);
}