package com.zrh.record.screen

import android.app.Application
import java.io.File

/**
 *
 * @author zrh
 * @date 2023/7/14
 *
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        ScreenRecordManager.init(this, RecordConfig().apply {
            notificationIcon = R.mipmap.ic_launcher_round
            notificationContent = "Recording..."

            maxWidth = 1080
            maxHeight = 1920
            maxDurationSec = 60
            bitrate = maxWidth * maxHeight * 2
            fps = 24

            val cache = if (externalCacheDir != null)externalCacheDir else cacheDir
            outputDir = File(cache, "screen_record")
        })
    }
}