package com.nivy.wallet.domain.pure.data

import com.nivy.base.time.TimeProvider
import com.nivy.wallet.utils.nivyMinTime
import java.time.Instant

data class ClosedTimeRange(
    val from: Instant,
    val to: Instant,
) {
    companion object {
        fun allTimeNivy(
            timeProvider: TimeProvider,
        ): ClosedTimeRange = ClosedTimeRange(
            from = nivyMinTime(),
            to = timeProvider.utcNow(),
        )

        fun to(to: Instant): ClosedTimeRange = ClosedTimeRange(
            from = nivyMinTime(),
            to = to
        )
    }
}
