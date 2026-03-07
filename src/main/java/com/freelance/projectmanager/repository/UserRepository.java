package com.freelance.projectmanager.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.freelance.projectmanager.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role = :role AND LOWER(u.location) LIKE LOWER(CONCAT('%', :location, '%')) " +
           "ORDER BY u.isTrending DESC, u.fullName ASC")
    List<User> findByRoleAndLocationWithRanking(@Param("role") User.Role role, @Param("location") String location);

    @Query("SELECT u FROM User u WHERE u.role = :role ORDER BY u.isTrending DESC, u.fullName ASC")
    List<User> findAllByRoleOrderByTrending(@Param("role") User.Role role);

    // Filter for users who are NOT on the FREE plan
    @Query("SELECT u FROM User u WHERE u.subscriptionPlan != 'FREE' AND u.subscriptionPlan IS NOT NULL")
    List<User> findSubscribedUsers();

    // Required for Stats
    long countByRole(User.Role role);

    @Query("SELECT COUNT(DISTINCT u.location) FROM User u WHERE u.location IS NOT NULL")
    long countDistinctLocation();

    List<User> findByLocationIgnoreCase(String location);
}