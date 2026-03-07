package com.freelance.projectmanager.repository;

import com.freelance.projectmanager.model.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    
    // Latest activity sabse upar dikhane ke liye
    List<UserActivity> findAllByOrderByTimestampDesc();
}