package com.avrgaming.civcraft.util;

import javax.annotation.Nonnull;

public class CivTime {
    private final long ticks;

    protected CivTime(long ticks) {
        this.ticks = ticks;
    }

    @Nonnull
    public static CivTime zero() {
        return new CivTime(0);
    }

    @Nonnull
    public static CivTime ticks(long ticks) {
        return new CivTime(ticks);
    }

    @Nonnull
    public static CivTime seconds(long seconds) {
        return CivTime.ticks(seconds * 20);
    }

    @Nonnull
    public static CivTime minutes(long minutes) {
        return CivTime.seconds(minutes * 60);
    }

    @Nonnull
    public static CivTime hours(long hours) {
        return CivTime.minutes(hours * 60);
    }

    public long toTicks() {
        return ticks;
    }
}
