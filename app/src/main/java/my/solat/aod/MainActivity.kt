package my.solat.aod

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
        }
        status = TextView(this).apply { textSize = 16f; gravity = Gravity.CENTER }
        val start = Button(this).apply { text = "Mulakan"; setOnClickListener { start() } }
        val live = Button(this).apply {
            text = "Tetapan Live updates"
            setOnClickListener { openLiveSettings() }
        }
        root.addView(status); root.addView(start); root.addView(live)
        setContentView(root)
        updateStatus()
    }

    override fun onResume() { super.onResume(); updateStatus() }

    private fun start() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            return
        }
        Solat.refreshAsync(applicationContext)
        status.text = "Notifikasi waktu solat (zon ${Solat.ZONE}) sedang aktif."
    }

    override fun onRequestPermissionsResult(code: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(code, p, r)
        if (r.isNotEmpty() && r[0] == PackageManager.PERMISSION_GRANTED) start()
    }

    private fun openLiveSettings() {
        val i = if (Build.VERSION.SDK_INT >= 36)
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        else
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        startActivity(i)
    }

    private fun updateStatus() {
        var msg = "Tekan Mulakan untuk papar waktu solat seterusnya."
        if (Build.VERSION.SDK_INT >= 36) {
            val nm = getSystemService(NotificationManager::class.java)
            if (!nm.canPostPromotedNotifications()) {
                msg += "\n\nLive updates belum dibenarkan. Buka Tetapan Live updates."
            }
        }
        status.text = msg
    }
}
