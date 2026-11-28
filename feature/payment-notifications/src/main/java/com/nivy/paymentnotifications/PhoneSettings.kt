package com.nivy.paymentnotifications

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.nivy.domain.notification.NotificationAccess
import com.nivy.ui.R
import com.nivy.wallet.android.notification.NivyNotificationChannel
import com.nivy.wallet.android.notification.NotificationService
import timber.log.Timber

/** An app with an icon among the phone's apps. */
data class InstalledApp(val packageName: String, val name: String)

/** Android's own pages and checks for reading notifications, and keeping Nivy listening. */
internal object PhoneSettings {
    // The test notification's number, under its tag.
    private const val TestId = 1

    /** Opens Android's notification access page: Nivy's own, where Android has one. */
    fun openNotificationAccess(context: Context) {
        val listener = NotificationAccess.listener(context).flattenToString()
        val detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listener)
        } else {
            null
        }
        open(context, detail, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    /** Opens Nivy's App info page. */
    fun openAppInfo(context: Context) {
        open(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context)))
    }

    /** Asks Android to stop limiting Nivy's battery use, or opens the list where that's set. */
    @SuppressLint("BatteryLife")
    fun openBatteryUse(context: Context) {
        open(
            context,
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri(context)),
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        )
    }

    /** Whether Android leaves Nivy's battery use unrestricted, so it doesn't stop Nivy in the background. */
    fun batteryUnrestricted(context: Context): Boolean =
        context.getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(context.packageName) == true

    /**
     * Whether Nivy was installed from a file: from Android 13, its notification access stays locked
     * until restricted settings are allowed in its App info.
     */
    fun installedFromFile(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val source = runCatching {
            context.packageManager.getInstallSourceInfo(context.packageName).packageSource
        }.getOrNull()
        return source == PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE ||
            source == PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE
    }

    /** The phone's apps with an icon, but Nivy, by name. */
    fun launchableApps(context: Context): List<InstalledApp> {
        val packageManager = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launcher, 0)
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(packageManager).toString()) }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Posts Nivy's silent test notification, which the listener reports when it hears it. False
     * when Nivy can't post notifications.
     */
    fun postTest(context: Context, notificationService: NotificationService): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        val notification = notificationService
            .defaultNivyNotification(NivyNotificationChannel.LISTENING_TEST, priority = NotificationCompat.PRIORITY_LOW)
            .setContentTitle(context.getString(R.string.notification_test_title))
            .setContentText(context.getString(R.string.notification_test_text))
            .setSilent(true)
        notificationService.showNotification(notification, TestId, tag = NotificationAccess.TestTag)
        return true
    }

    /** Takes the test notification away, once heard. */
    fun cancelTest(context: Context) {
        NotificationManagerCompat.from(context).cancel(NotificationAccess.TestTag, TestId)
    }

    private fun packageUri(context: Context): Uri = Uri.fromParts("package", context.packageName, null)

    // The first of [intents]' pages this phone can open, from the screen's [context].
    private fun open(context: Context, vararg intents: Intent?) {
        val opened = intents.filterNotNull().any { intent ->
            try {
                context.startActivity(intent)
                true
            } catch (e: ActivityNotFoundException) {
                Timber.w(e)
                false
            } catch (e: SecurityException) {
                Timber.w(e)
                false
            }
        }
        if (!opened) Timber.w("No settings page to open")
    }
}
