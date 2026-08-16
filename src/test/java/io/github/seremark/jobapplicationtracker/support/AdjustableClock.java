package io.github.seremark.jobapplicationtracker.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class AdjustableClock extends Clock {

  private final AtomicReference<Instant> currentInstant;
  private final ZoneId zone;

  public AdjustableClock(Instant initialInstant, ZoneId zone) {
    this(new AtomicReference<>(Objects.requireNonNull(initialInstant, "initialInstant")), zone);
  }

  private AdjustableClock(AtomicReference<Instant> currentInstant, ZoneId zone) {
    this.currentInstant = Objects.requireNonNull(currentInstant, "currentInstant");
    this.zone = Objects.requireNonNull(zone, "zone");
  }

  public void setInstant(Instant instant) {
    currentInstant.set(Objects.requireNonNull(instant, "instant"));
  }

  @Override
  public ZoneId getZone() {
    return zone;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    ZoneId requestedZone = Objects.requireNonNull(zone, "zone");
    return this.zone.equals(requestedZone)
        ? this
        : new AdjustableClock(currentInstant, requestedZone);
  }

  @Override
  public Instant instant() {
    return currentInstant.get();
  }
}
