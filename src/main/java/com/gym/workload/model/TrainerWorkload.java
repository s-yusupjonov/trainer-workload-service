package com.gym.workload.model;

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

    public ConcurrentMap<Integer, ConcurrentMap<Integer, Integer>> getMinutesByYearMonth() {
        return minutesByYearMonth;
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