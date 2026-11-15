package my.solat.aod

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pr = goAsync()
        Solat.refreshAsync(context.applicationContext) { pr.finish() }
    }
}
