package my.solat.aod

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object Notifier {
    private const val CH = "solat"
    private const val ID = 1

    fun show(ctx: Context, name: String, whenMs: Long) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH, "Waktu solat", NotificationManager.IMPORTANCE_LOW)
        )
        val fmt = SimpleDateFormat("h:mma", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
        }
        val time = fmt.format(whenMs)

        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val b = Notification.Builder(ctx, CH)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("$name $time")
            .setContentText("Waktu solat seterusnya")
            .setStyle(Notification.BigTextStyle().bigText("$name pada $time"))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setWhen(whenMs)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setContentIntent(open)

        if (Build.VERSION.SDK_INT >= 36) {
            b.setRequestPromotedOngoing(true)
            b.setShortCriticalText(time)
        }
        nm.notify(ID, b.build())
    }
}
