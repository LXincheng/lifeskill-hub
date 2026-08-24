package dev.lifeskill.skill.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

public record WeeklySchedule(DayOfWeek dayOfWeek, LocalTime time, ZoneId timezone) {

    public WeeklySchedule {
        Objects.requireNonNull(dayOfWeek, "Schedule day is required");
        Objects.requireNonNull(time, "Schedule time is required");
        Objects.requireNonNull(timezone, "Schedule timezone is required");
    }
}
