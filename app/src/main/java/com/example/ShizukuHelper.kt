package com.example

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuHelper {

    fun isShizukuAvailable(): Boolean {
        return Shizuku.pingBinder()
    }

    fun hasShizukuPermission(): Boolean {
        return if (isShizukuAvailable()) {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    fun requestShizukuPermission(requestCode: Int, listener: OnRequestPermissionResultListener) {
        if (isShizukuAvailable() && !hasShizukuPermission()) {
            Shizuku.addRequestPermissionResultListener(listener)
            Shizuku.requestPermission(requestCode)
        }
    }

    fun removeShizukuPermissionListener(listener: OnRequestPermissionResultListener) {
        Shizuku.removeRequestPermissionResultListener(listener)
    }

    suspend fun getForegroundPackageName(): String? = withContext(Dispatchers.IO) {
        if (!hasShizukuPermission()) return@withContext null
        
        try {
            // Sử dụng lệnh dumpsys window để lấy tên app đang hiển thị
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", "dumpsys window | grep mCurrentFocus"), null, null) as Process
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            reader.close()
            process.waitFor()
            
            // Format trả về thường có dạng: mCurrentFocus=Window{xxx u0 com.example.app/com.example.app.MainActivity}
            if (line != null && line.contains("mCurrentFocus")) {
                val parts = line.split(" ")
                val component = parts.find { it.contains("/") }
                if (component != null) {
                    return@withContext component.split("/")[0]
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun whitelistApp(packageName: String) = withContext(Dispatchers.IO) {
        if (!hasShizukuPermission()) return@withContext
        try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            newProcessMethod.isAccessible = true
            
            val p1 = newProcessMethod.invoke(null, arrayOf("sh", "-c", "dumpsys deviceidle whitelist +$packageName"), null, null) as Process
            p1.waitFor()
            
            val p2 = newProcessMethod.invoke(null, arrayOf("sh", "-c", "appops set $packageName RUN_IN_BACKGROUND allow"), null, null) as Process
            p2.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
