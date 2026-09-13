package com.gym.workload.domain;

import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class TrainerWorkload {

    private final String trainerUsername;
    private volatile String trainerFirstName;
    private volatile String trainerLastName;
    private volatile boolean active;
    private final ConcurrentMap<Integer, ConcurrentMap<Integer, Integer>> minutesByYearMonth = new ConcurrentHashMap<>();

    public TrainerWorkload(String trainerUsername, String trainerFirstName, String trainerLastName, boolean active) {
        this.trainerUsername = trainerUsername;
        this.trainerFirstName = trainerFirstName;
        this.trainerLastName = trainerLastName;
        this.active = active;
    }

    public String getTrainerUsername() {
        return trainerUsername;
    }

    public String getTrainerFirstName() {
        return trainerFirstName;
    }

    public String getTrainerLastName() {
        return trainerLastName;
    }

    public boolean isActive() {
        return active;
    }

    public void updateProfile(String firstName, String lastName, boolean isActive) {
        this.trainerFirstName = firstName;
        this.trainerLastName = lastName;
        this.active = isActive;
    }

    public void addMinutes(int year, int month, int minutes) {
        minutesByYearMonth
                .computeIfAbsent(year, key -> new ConcurrentHashMap<>())
                .merge(month, minutes, Integer::sum);
    }

    public void subtractMinutes(int year, int month, int minutes) {
        ConcurrentMap<Integer, Integer> months = minutesByYearMonth.get(year);
        if (months == null) {
            return;
        }

        months.compute(month, (key, current) -> {
            if (current == null) {
                return null;
            }
            int updated = Math.max(0, current - minutes);
            return updated == 0 ? null : updated;
        });

        if (months.isEmpty()) {
            minutesByYearMonth.remove(year, months);
        }
    }

    public OptionalInt getMinutes(int year, int month) {
        ConcurrentMap<Integer, Integer> months = minutesByYearMonth.get(year);
        if (months == null) {
            return OptionalInt.empty();
        }
        Integer minutes = months.get(month);
        return minutes == null ? OptionalInt.empty() : OptionalInt.of(minutes);
    }

    public Map<Integer, Map<Integer, Integer>> snapshotByYear() {
        return minutesByYearMonth.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
    }
}
