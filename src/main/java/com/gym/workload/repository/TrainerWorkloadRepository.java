package com.gym.workload.repository;

import com.gym.workload.model.TrainerWorkload;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TrainerWorkloadRepository {

    private final Map<String, TrainerWorkload> workloadsByUsername = new ConcurrentHashMap<>();

    public Optional<TrainerWorkload> findByUsername(String username) {
        return Optional.ofNullable(workloadsByUsername.get(username));
    }

    public TrainerWorkload getOrCreate(String username, String firstName, String lastName, boolean isActive) {
        return workloadsByUsername.compute(username, (key, existing) -> {
            if (existing == null) {
                return new TrainerWorkload(username, firstName, lastName, isActive);
            }
            existing.updateProfile(firstName, lastName, isActive);
            return existing;
        });
    }
}