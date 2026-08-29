package com.swissquote.caa.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-08-29T10:00:00Z"));
    private final LoginAttemptService service = new LoginAttemptService(clock);

    @Test
    void blocksAfterTooManyFailuresAndUnblocksWhenTheWindowPasses() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) {
            assertThat(service.isBlocked("alice")).isFalse();
            service.recordFailure("alice");
        }
        assertThat(service.isBlocked("alice")).isTrue();
        assertThat(service.isBlocked("ALICE ")).isTrue(); // normalised
        assertThat(service.isBlocked("bob")).isFalse();

        clock.advance(LoginAttemptService.WINDOW.plusSeconds(1));
        assertThat(service.isBlocked("alice")).isFalse();
    }

    @Test
    void successfulLoginClearsTheCounter() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) {
            service.recordFailure("alice");
        }
        service.recordSuccess("alice");
        assertThat(service.isBlocked("alice")).isFalse();
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
