package com.swissquote.caa.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Minimal in-memory brute-force guard for the login endpoint: at most
 * {@value #MAX_FAILURES} failed attempts per username within {@link #WINDOW}. Per-instance
 * state is acceptable for this deployment shape; a shared store (Redis) would replace it
 * behind the same interface in a multi-instance setup.
 */
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES = 10;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> failuresByUsername = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String username) {
        Deque<Instant> failures = failuresByUsername.get(normalise(username));
        if (failures == null) {
            return false;
        }
        synchronized (failures) {
            prune(failures);
            return failures.size() >= MAX_FAILURES;
        }
    }

    public void recordFailure(String username) {
        Deque<Instant> failures =
            failuresByUsername.computeIfAbsent(normalise(username), k -> new ArrayDeque<>());
        synchronized (failures) {
            prune(failures);
            failures.addLast(clock.instant());
        }
    }

    public void recordSuccess(String username) {
        failuresByUsername.remove(normalise(username));
    }

    private void prune(Deque<Instant> failures) {
        Instant cutoff = clock.instant().minus(WINDOW);
        while (!failures.isEmpty() && failures.peekFirst().isBefore(cutoff)) {
            failures.removeFirst();
        }
    }

    private static String normalise(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
