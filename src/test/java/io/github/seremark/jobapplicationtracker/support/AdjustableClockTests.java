package io.github.seremark.jobapplicationtracker.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AdjustableClockTests {

  private static final Instant INITIAL_INSTANT = Instant.parse("2026-08-16T08:30:00Z");

  private final AdjustableClock clock = new AdjustableClock(INITIAL_INSTANT, ZoneOffset.UTC);

  @Test
  void instantCanBeAdjusted() {
    Instant adjustedInstant = INITIAL_INSTANT.plusSeconds(60);

    clock.setInstant(adjustedInstant);

    assertThat(clock.instant()).isEqualTo(adjustedInstant);
  }

  @Test
  void clockWithAnotherZoneSharesTheAdjustableInstant() {
    ZoneId budapest = ZoneId.of("Europe/Budapest");
    Clock clockInBudapest = clock.withZone(budapest);
    Instant adjustedInstant = INITIAL_INSTANT.plusSeconds(60);

    clock.setInstant(adjustedInstant);

    assertThat(clock.withZone(ZoneOffset.UTC)).isSameAs(clock);
    assertThat(clockInBudapest.getZone()).isEqualTo(budapest);
    assertThat(clockInBudapest.instant()).isEqualTo(adjustedInstant);
  }
}
