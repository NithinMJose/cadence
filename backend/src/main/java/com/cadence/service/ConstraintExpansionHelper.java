package com.cadence.service;

import com.cadence.model.ScheduleConstraintEntity;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Expands recurring weekly constraints into concrete ISO timestamps across horizon.
 */
public final class ConstraintExpansionHelper {

    private ConstraintExpansionHelper() {}

    public static List<Map<String, Object>> expandBlocked(
            List<ScheduleConstraintEntity> constraints,
            ZonedDateTime horizonStart,
            ZoneId userTz
    ) {
        List<Map<String, Object>> blocked = new ArrayList<>();
        for (ScheduleConstraintEntity c : constraints) {
            if (!c.isHardBlock() || c.getWindowStart() == null || c.getWindowEnd() == null) continue;
            if (c.getWindowStart().length() <= 8) {
                LocalTime s = LocalTime.parse(c.getWindowStart());
                LocalTime e = LocalTime.parse(c.getWindowEnd());
                for (int d = 0; d < 7; d++) {
                    ZonedDateTime day = horizonStart.plusDays(d);
                    if (c.getDaysOfWeekJson() == null || c.getDaysOfWeekJson().contains(day.getDayOfWeek().name())) {
                        String startIso = day.toLocalDate().atTime(s).atZone(userTz).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        String endIso = day.toLocalDate().atTime(e).atZone(userTz).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        blocked.add(Map.of("start", startIso, "end", endIso, "reason", c.getReason(), "is_hard", true));
                    }
                }
            } else {
                blocked.add(Map.of("start", c.getWindowStart(), "end", c.getWindowEnd(), "reason", c.getReason(), "is_hard", true));
            }
        }
        return blocked;
    }

    public static List<Map<String, Object>> expandLeisure(
            List<ScheduleConstraintEntity> constraints,
            ZonedDateTime horizonStart,
            ZoneId userTz
    ) {
        List<Map<String, Object>> leisure = new ArrayList<>();
        for (ScheduleConstraintEntity c : constraints) {
            if (c.isHardBlock() || c.getWindowStart() == null || c.getWindowEnd() == null) continue;
            if (c.getWindowStart().length() <= 8) {
                LocalTime s = LocalTime.parse(c.getWindowStart());
                LocalTime e = LocalTime.parse(c.getWindowEnd());
                for (int d = 0; d < 7; d++) {
                    ZonedDateTime day = horizonStart.plusDays(d);
                    if (c.getDaysOfWeekJson() == null || c.getDaysOfWeekJson().contains(day.getDayOfWeek().name())) {
                        String startIso = day.toLocalDate().atTime(s).atZone(userTz).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        String endIso = day.toLocalDate().atTime(e).atZone(userTz).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        leisure.add(Map.of("start", startIso, "end", endIso, "name", c.getReason(), "is_protected", true));
                    }
                }
            }
        }
        return leisure;
    }
}
