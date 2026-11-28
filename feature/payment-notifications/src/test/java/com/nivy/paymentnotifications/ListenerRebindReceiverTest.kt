package com.nivy.paymentnotifications

import android.content.Context
import android.content.Intent
import com.nivy.domain.notification.NotificationAccess
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class ListenerRebindReceiverTest {
    private val context = mockk<Context>()

    @Before
    fun setUp() {
        mockkObject(NotificationAccess.Companion)
        every { NotificationAccess.rebind(any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkObject(NotificationAccess.Companion)
    }

    @Test
    fun `the phone restarting, or Nivy updating, binds the listener again`() {
        ListenerRebindReceiver().onReceive(context, intent(Intent.ACTION_BOOT_COMPLETED))
        ListenerRebindReceiver().onReceive(context, intent(Intent.ACTION_MY_PACKAGE_REPLACED))

        verify(exactly = 2) { NotificationAccess.rebind(context) }
    }

    @Test
    fun `any other broadcast, or one without an action, binds nothing`() {
        ListenerRebindReceiver().onReceive(context, intent("com.example.SPOOFED"))
        ListenerRebindReceiver().onReceive(context, intent(null))

        verify(exactly = 0) { NotificationAccess.rebind(any()) }
    }

    private fun intent(action: String?) = mockk<Intent> { every { this@mockk.action } returns action }
}
