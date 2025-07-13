package org.igye.remem3.test;

import org.igye.remem3.utils.Exn;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class TestClock extends Clock {
    private Instant instant;

    public TestClock(Instant instant) {
        this.instant = instant;
    }

    @Override
    public ZoneId getZone() {
        throw new Exn("Not implemented");
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new Exn("Not implemented");
    }

    @Override
    public Instant instant() {
        return instant;
    }

    public Instant plusSeconds(long seconds) {
        instant = instant.plusSeconds(seconds);
        return instant;
    }
}
