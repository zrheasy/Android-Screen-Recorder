package com.zrh.record.screen

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.zrh.permission.PermissionUtils
import com.zrh.record.screen.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity(), ScreenRecordCallback {
    private lateinit var mBinding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)


        mBinding.btnStart.setOnClickListener {
            PermissionUtils.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO)) { _, granted ->
                startRecord()
            }
        }

        mBinding.btnStop.setOnClickListener {
            ScreenRecordManager.stop()
        }

        ScreenRecordManager.addCallback(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        ScreenRecordManager.removeCallback(this)
    }

    private fun startRecord() {
        ScreenRecordManager.start(this)
    }

    override fun onBackPressed() {
        moveTaskToBack(false)
    }

    override fun onCompleted(file: File) {
        mBinding.tvResult.text = "录制完成：${file.absoluteFile}"
    }

    override fun onError(code: Int, msg: String) {
        mBinding.tvResult.text = "录制失败：$code $msg"
    }

    override fun onDurationChanged(duration: Long) {
        var seconds = (duration / 1000).toInt()
        val minutes = seconds / 60
        seconds %= 60
        val time = "${formatNum(minutes)}:${formatNum(seconds)}"
        mBinding.tvResult.text = "录制中：$time"
    }

    private fun formatNum(num: Int): String {
        return if (num < 10) "0$num" else num.toString()
    }
}