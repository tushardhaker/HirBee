package com.freelance.projectmanager.service;

import com.freelance.projectmanager.model.User;
import com.freelance.projectmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SubscriptionScheduler {

    @Autowired
    private UserRepository userRepository;

    // Har raat 12 baje check karega
    @Scheduled(cron = "0 0 0 * * *")
    public void checkExpiredSubscriptions() {
        List<User> users = userRepository.findSubscribedUsers();
        
        for (User user : users) {
            if (user.getSubscriptionEndDate() != null && user.getSubscriptionEndDate().isBefore(LocalDateTime.now())) {
                user.setSubscriptionPlan("FREE");
                user.setSubscriptionStatus("NONE");
                user.setTrending(false); // Remove Blue Tick
                user.setSubscriptionEndDate(null);
                userRepository.save(user);
                System.out.println("Auto-Expired subscription for: " + user.getEmail());
            }
        }
    }
}