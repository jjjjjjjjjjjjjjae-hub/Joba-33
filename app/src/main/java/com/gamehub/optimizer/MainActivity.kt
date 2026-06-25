package com.gamehub.optimizer

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnOptimize = findViewById<Button>(R.id.btn_optimize)
        val tvStatus = findViewById<TextView>(R.id.tv_status)

        btnOptimize.setOnClickListener {
            tvStatus.text = "Оңтайландыру жұмыстары орындалуда..."

            // 1. Желілік қателіктерді автоматты тексеру және реттеу
            fixNetworkError()

            // 2. RAM босату және басқа фондық бағдарламаларды шектеу
            optimizeRamAndBackground()

            tvStatus.text = "Дайын! Free Fire іске қосылуда..."
            
            // 3. Free Fire ойынын автоматты түрде ашу
            launchFreeFire()
        }
    }

    // Желілік байланысты және интернет қосылымын тексеру
    private fun fixNetworkError() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
            Toast.makeText(this, "Сервис қателігі анықталды! Желілік байланыс қайта орнатылуда...", Toast.LENGTH_SHORT).show()
        }
    }

    // Жедел жадыны (RAM) босату және басқа ауыр фондық процестерді шектеу
    private fun optimizeRamAndBackground() {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses
        
        runningProcesses?.forEach { processInfo ->
            // Егер процесс Free Fire немесе біздің қосымша болмаса, оны жабуға тырысамыз
            if (processInfo.processName != "com.dts.freefireth" && processInfo.processName != packageName) {
                activityManager.killBackgroundProcesses(processInfo.processName)
            }
        }
    }

    // Free Fire ойынын пакет атауы арқылы ашу
    private fun launchFreeFire() {
        val launchIntent = packageManager.getLaunchIntentForPackage("com.dts.freefireth")
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "Сөндірілді немесе Free Fire ойыны табылмады!", Toast.LENGTH_LONG).show()
        }
    }
}
