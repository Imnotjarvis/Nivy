package com.nivy.paymentnotifications

import com.nivy.domain.notification.NotificationAccess
import io.kotest.matchers.shouldBe
import org.junit.Test

class PaymentNotificationListenerTest {
    @Test
    fun `the listener is the class Android keeps the access under`() {
        PaymentNotificationListener::class.java.name shouldBe NotificationAccess.ListenerClass
    }
}
