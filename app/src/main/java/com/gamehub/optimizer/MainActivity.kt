package com.gamehub.optimizer

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Батырманы XML-сіз, тікелей кодпен жасау
        val btnStartFreeFire = Button(this).apply {
            text = "BOOST & START FREE FIRE"
            setOnClickListener {
                boostAndPlay()
            }
        }

        setContentView(btnStartFreeFire)
    }

    private fun enableDoNotDisturb() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            } else {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(intent)
            }
        }
    }

    private fun boostAndPlay() {
        enableDoNotDisturb()

        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses
        runningProcesses?.forEach { processInfo ->
            if (processInfo.processName != "com.dts.freefireth" && processInfo.processName != packageName) {
                activityManager.killBackgroundProcesses(processInfo.processName)
            }
        }

        val launchIntent = packageManager.getLaunchIntentForPackage("com.dts.freefireth")
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "Free Fire телефоныңызда орнатылмаған!", Toast.LENGTH_SHORT).show()
        }
    }
}
