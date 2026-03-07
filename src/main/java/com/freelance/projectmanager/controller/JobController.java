package com.freelance.projectmanager.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.freelance.projectmanager.model.Job;
import com.freelance.projectmanager.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
// @CrossOrigin(origins = { "http://localhost:5500", "https://hir-bee-3nwb.vercel.app" }, allowCredentials = "true")
public class JobController {

    @Autowired
    private JobRepository jobRepository;

    @PostMapping("/create")
    public ResponseEntity<?> createJob(@RequestBody Job job) {
        try {
            if (job.getClientEmail() == null || job.getClientEmail().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User session expired. Please login again."));
            }

            if (job.getStatus() == null)
                job.setStatus("ACTIVE");

            System.out.println("LOG: Saving Job Title -> " + job.getTitle() + " | Category: " + job.getCategory());

            Job savedJob = jobRepository.save(job);
            return ResponseEntity.ok(savedJob);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Database save failed: " + e.getMessage()));
        }
    }

    @GetMapping("/my-jobs")
    public ResponseEntity<List<Job>> getMyJobs(@RequestParam String email) {
        List<Job> jobs = jobRepository.findByClientEmail(email);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/freelancer-feed")
    public ResponseEntity<List<Job>> getFreelancerFeed(@RequestParam(required = false) String city) {
        List<Job> jobs;
        if (city == null || city.trim().isEmpty() || city.equalsIgnoreCase("GLOBAL")
                || city.equalsIgnoreCase("Not Set")) {
            jobs = jobRepository.findAll();
        } else {
            jobs = jobRepository.findJobsByLocation(city.trim());
        }

        List<Job> activeJobs = jobs.stream()
                .filter(j -> !"BANNED".equalsIgnoreCase(j.getStatus()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(activeJobs);
    }

    @GetMapping("/admin-all")
    public ResponseEntity<List<Job>> getAllJobsForAdmin() {
        return ResponseEntity.ok(jobRepository.findAll());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateJobStatus(@PathVariable Long id, @RequestParam String status) {
        return jobRepository.findById(id)
                .map(job -> {
                    job.setStatus(status.toUpperCase());
                    jobRepository.save(job);
                    return ResponseEntity.ok(job);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Job>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double maxBudget,
            @RequestParam(required = false) String category, // Added Category param
            @RequestParam(required = false, defaultValue = "all") String mode) {

        try {
            List<Job> allJobs = jobRepository.findAll();

            List<Job> filtered = allJobs.stream()
                    .filter(job -> !"BANNED".equalsIgnoreCase(job.getStatus()))
                    .filter(job -> {
                        // Category Filter
                        if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("all")) {
                            if (job.getCategory() == null || !job.getCategory().equalsIgnoreCase(category.trim()))
                                return false;
                        }

                        // Keyword Logic
                        if (keyword != null && !keyword.trim().isEmpty()) {
                            String kw = keyword.trim().toLowerCase();
                            String title = job.getTitle() != null ? job.getTitle().toLowerCase() : "";
                            String details = job.getDetails() != null ? job.getDetails().toLowerCase() : "";
                            if (!title.contains(kw) && !details.contains(kw))
                                return false;
                        }

                        // Location Filter
                        if (location != null && !location.trim().isEmpty()) {
                            String loc = location.trim().toLowerCase();
                            String jobLoc = job.getLocation() != null ? job.getLocation().toLowerCase() : "";
                            if (!jobLoc.contains(loc))
                                return false;
                        }

                        // Budget Filter
                        if (maxBudget != null && maxBudget > 0) {
                            Double b = job.getBudget();
                            if (b != null && b > maxBudget)
                                return false;
                        }

                        // Mode Filter
                        if (mode != null && !mode.equalsIgnoreCase("all")) {
                            String jobMode = job.getMode() != null ? job.getMode().toUpperCase() : "";
                            String reqMode = mode.toUpperCase();
                            if (reqMode.equals("ONLINE")) {
                                if (!jobMode.equals("ONLINE") && !jobMode.equals("ONILINE"))
                                    return false;
                            } else if (reqMode.equals("OFFLINE")) {
                                if (!jobMode.equals("OFFLINE"))
                                    return false;
                            }
                        }
                        return true;
                    })
                    .sorted((j1, j2) -> {
                        var d1 = j1.getCreatedAt();
                        var d2 = j2.getCreatedAt();
                        if (d1 == null && d2 == null)
                            return 0;
                        if (d1 == null)
                            return 1;
                        if (d2 == null)
                            return -1;
                        return d2.compareTo(d1);
                    })
                    .limit(50)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(filtered);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    @GetMapping("/categories")
    public List<String> getDistinctJobCategories() {
        return jobRepository.findDistinctCategories();
    }
}