package demo;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import java.time.Duration;

public final class Pace {
    static Duration seconds(double seconds) {
        return Duration.ofMillis((long) (seconds * 1000));
    }

    // a slower demo pace needs a longer timeout and a longer wait between retries
    static ActivityOptions options(String queue, double pace) {
        return ActivityOptions.newBuilder()
            .setTaskQueue(queue)
            .setStartToCloseTimeout(seconds(5 + 4 * pace))
            .setRetryOptions(RetryOptions.newBuilder().setInitialInterval(seconds(1 + pace)).setMaximumAttempts(5).build())
            .build();
    }

    // simulated latency: zero in tests, a few seconds in the demo
    static void pause(double pace, double seconds) {
        try {
            Thread.sleep((long) (seconds * pace * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", e);
        }
    }
}
