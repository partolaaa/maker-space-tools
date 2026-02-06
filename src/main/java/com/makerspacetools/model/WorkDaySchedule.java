package com.makerspacetools.model;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Represents a weekly schedule for workdays, where each day of the week
 * is mapped to a specific time window.
 *
 * @param schedule an unmodifiable map of days of the week and their corresponding time windows
 */
public record WorkDaySchedule(Map<DayOfWeek, TimeWindow> schedule) {

    private static final WorkDaySchedule BUSINESS_HOURS = new WorkDaySchedule(null);

    /**
     * Ctor.
     */
    public WorkDaySchedule(Map<DayOfWeek, TimeWindow> schedule) {
        Map<DayOfWeek, TimeWindow> map = schedule != null ? new EnumMap<>(schedule) : new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            map.putIfAbsent(day, defaultWindowFor(day));
        }
        this.schedule = Collections.unmodifiableMap(map);
    }

    /**
     * Returns the default business-hours schedule.
     *
     * @return business-hours schedule
     */
    public static WorkDaySchedule businessHours() {
        return BUSINESS_HOURS;
    }

    private static TimeWindow defaultWindowFor(DayOfWeek day) {
        return switch (day) {
            case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> TimeWindow.weekday();
            case SATURDAY -> TimeWindow.weekend();
            case SUNDAY -> TimeWindow.holiday();
        };
    }

    /**
     * Returns the time window configured for the given day.
     *
     * @param day day of week
     * @return time window for the day
     */
    public TimeWindow windowFor(DayOfWeek day) {
        return schedule.getOrDefault(day, defaultWindowFor(day));
    }

    /**
     * Returns whether the given date time is within the schedule.
     *
     * @param dateTime date time to test
     * @return true when the time is inside the window
     */
    public boolean isWithinSchedule(LocalDateTime dateTime) {
        TimeWindow window = windowFor(dateTime.getDayOfWeek());
        return window.isWithinWorkday(dateTime.toLocalTime());
    }

    /**
     * Represents a time window for a single day.
     *
     * @param start start time, inclusive
     * @param end end time, exclusive
     */
    public record TimeWindow(LocalTime start, LocalTime end) {

        private static final int WEEK_DAY_START_HOUR = 8;
        private static final int WEEK_DAY_END_HOUR = 16;
        private static final int WEEKEND_START_HOUR = 9;
        private static final int WEEKEND_END_HOUR = 17;

        /**
         * Returns whether the given time is within the window.
         *
         * @param time time to test
         * @return true when the time is inside the window
         */
        public boolean isWithinWorkday(LocalTime time) {
            return !time.isBefore(start) && time.isBefore(end);
        }

        /**
         * Returns the weekday time window.
         *
         * @return weekday time window
         */
        public static TimeWindow weekday() {
            return TimeWindow.of(LocalTime.of(WEEK_DAY_START_HOUR, 0), LocalTime.of(WEEK_DAY_END_HOUR, 0));
        }

        /**
         * Returns the Saturday time window.
         *
         * @return Saturday time window
         */
        public static TimeWindow weekend() {
            return TimeWindow.of(LocalTime.of(WEEKEND_START_HOUR, 0), LocalTime.of(WEEKEND_END_HOUR, 0));
        }

        /**
         * Returns the holiday time window.
         *
         * @return holiday time window
         */
        public static TimeWindow holiday() {
            return TimeWindow.of(LocalTime.MIN, LocalTime.MIN);
        }

        /**
         * Creates a time window for the given start and end.
         *
         * @param start start time, inclusive
         * @param end end time, exclusive
         * @return time window
         */
        public static TimeWindow of(LocalTime start, LocalTime end) {
            return new TimeWindow(start, end);
        }
    }
}
