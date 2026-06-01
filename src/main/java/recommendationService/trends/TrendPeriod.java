package recommendationService.trends;

import java.time.Clock;
import java.time.LocalDate;

public enum TrendPeriod {
    DAY(0),
    WEEK(6),
    MONTH(29);

    private final int daysBeforeToday;

    TrendPeriod(int daysBeforeToday) {
        this.daysBeforeToday = daysBeforeToday;
    }

    public LocalDate startDate(Clock clock) {
        return LocalDate.now(clock).minusDays(daysBeforeToday);
    }

    public LocalDate endDate(Clock clock) {
        return LocalDate.now(clock);
    }
}
