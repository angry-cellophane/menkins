package org.ka.menkins

import java.util.concurrent.TimeUnit

trait TimerHelper {
    long seconds(long value) {
        TimeUnit.SECONDS.toNanos(value)
    }
}
