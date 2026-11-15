package my.solat.aod

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object Solat {
    // SGR01 = Gombak, Petaling, Sepang, Hulu Langat, Hulu Selangor, Rawang, Shah Alam
    const val ZONE = "SGR01"

    private val prayers = listOf(
        "fajr" to "Subuh", "dhuhr" to "Zohor", "asr" to "Asar",
        "maghrib" to "Maghrib", "isha" to "Isyak"
    )
    private val tz = TimeZone.getTimeZone("Asia/Kuala_Lumpur")

    /** Panggil dari thread latar belakang. */
    fun refreshBlocking(ctx: Context) {
        val now = System.currentTimeMillis()
        try {
            val days = loadMonth(ctx)
            val next = findNext(days, now)
            Notifier.show(ctx, next.first, next.second)
            schedule(ctx, next.second + 1000)
        } catch (e: Exception) {
            schedule(ctx, now + 15 * 60_000) // cuba lagi 15 minit
        }
    }

    fun refreshAsync(ctx: Context, done: (() -> Unit)? = null) {
        Thread { try { refreshBlocking(ctx) } finally { done?.invoke() } }.start()
    }

    private fun loadMonth(ctx: Context): List<JSONObject> {
        val prefs = ctx.getSharedPreferences("cache", Context.MODE_PRIVATE)
        val cal = Calendar.getInstance(tz)
        val key = "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        var json = prefs.getString("json", null)
        if (json == null || prefs.getString("month", "") != key) {
            try {
                val url = URL("https://www.e-solat.gov.my/index.php?r=esolatApi/TakwimSolat&period=month&zone=$ZONE")
                val c = url.openConnection() as HttpURLConnection
                c.connectTimeout = 15000
                c.readTimeout = 15000
                val body = c.inputStream.bufferedReader().readText()
                JSONObject(body).getJSONArray("prayerTime") // semak sah
                json = body
                prefs.edit().putString("json", body).putString("month", key).apply()
            } catch (e: Exception) {
                if (json == null) throw e
            }
        }
        val arr = JSONObject(json!!).getJSONArray("prayerTime")
        return List(arr.length()) { arr.getJSONObject(it) }
    }

    private fun findNext(days: List<JSONObject>, now: Long): Pair<String, Long> {
        val fmt = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH).apply { timeZone = tz }
        for (d in days) {
            for ((k, label) in prayers) {
                val t = fmt.parse(d.getString("date") + " " + d.getString(k))!!.time
                if (t > now) return label to t
            }
        }
        // Hujung bulan: anggar Subuh esok daripada Subuh hari terakhir
        val last = days.last()
        val f = fmt.parse(last.getString("date") + " " + last.getString("fajr"))!!.time
        return "Subuh" to (f + 24 * 3600_000)
    }

    private fun schedule(ctx: Context, at: Long) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            ctx, 0, Intent(ctx, RefreshReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }
}
