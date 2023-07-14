package com.zrh.record.screen;

import static android.media.MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED;
import static android.media.MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author zrh
 * @date 2023/7/14
 */
public class ScreenRecorder {
    private final int maxWidth;
    private final int maxHeight;
    private final int bitrate;
    private final int fps;
    private final boolean recordAudio;
    private final long maxDuration;
    private final File outputDir;

    private final Context context;
    private final MediaProjection projection;

    private ScreenRecordCallback callback;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;
    private Timer timer;
    private File output;

    private boolean isRunning;
    private long duration;

    private ScreenRecorder(Builder builder) {
        this.maxWidth = builder.maxWidth;
        this.maxHeight = builder.maxHeight;
        this.bitrate = Math.min(builder.bitrate, maxWidth * maxHeight * 3);
        this.fps = builder.fps;
        this.recordAudio = builder.recordAudio;
        this.outputDir = builder.outputDir;
        this.maxDuration = builder.maxDurationSec * 1000L;
        this.callback = builder.callback;
        this.context = builder.context;
        this.projection = builder.projection;
    }

    public void setCallback(ScreenRecordCallback callback) {
        this.callback = callback;
    }

    public void start() {
        if (isRunning) return;
        cancel();

        initOutputFile();

        int[] size = calculateRecordSize();

        initMediaRecorder(size[0], size[1]);

        try {
            mediaRecorder.prepare();
            initVirtualDisplay(size[0], size[1], mediaRecorder.getSurface());
            mediaRecorder.start();
            startTimer();
        } catch (Exception e) {
            e.printStackTrace();
            release();
            notifyError(ErrorCode.RECORD_ERROR, e.toString());
        }
    }

    private void startTimer() {
        duration = 0;
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                duration += 100;
                notifyDurationChanged(duration);
            }
        }, 100, 100);
    }

    private void initVirtualDisplay(int width, int height, Surface surface) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int flag = DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR;
        virtualDisplay = projection.createVirtualDisplay("ScreenRecord", width, height, displayMetrics.densityDpi,
                                                         flag, surface, null, null);
    }

    private void initMediaRecorder(int width, int height) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mediaRecorder = new MediaRecorder(context);
        } else {
            mediaRecorder = new MediaRecorder();
        }

        boolean isRecordAudio = recordAudio && checkAudioPermission();
        if (isRecordAudio) {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        }
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        if (isRecordAudio) {
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        }
        mediaRecorder.setOutputFile(output.getAbsolutePath());
        mediaRecorder.setVideoEncodingBitRate(bitrate);
        mediaRecorder.setVideoSize(width, height);
        mediaRecorder.setVideoFrameRate(fps);
        mediaRecorder.setMaxDuration((int) maxDuration);

        mediaRecorder.setOnInfoListener((mr, what, extra) -> {
            if (what == MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                    what == MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                stop();
            }
        });
    }

    private boolean checkAudioPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        String p = Manifest.permission.RECORD_AUDIO;
        return ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED;
    }

    private int[] calculateRecordSize() {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        float widthScale = (screenWidth <= maxWidth) ? 1f : 1f * maxWidth / screenWidth;
        float heightScale = (screenHeight <= maxHeight) ? 1f : 1f * maxHeight / screenHeight;
        float scale = Math.min(widthScale, heightScale);

        int width = Math.round(screenWidth * scale);
        int height = Math.round(screenHeight * scale);

        if (width % 2 == 1) width--;
        if (height % 2 == 1) height--;
        return new int[]{width, height};
    }

    private void initOutputFile() {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        String fileName = System.currentTimeMillis() + ".temp";
        output = new File(outputDir, fileName);
    }

    public void cancel() {
        release();

        if (output != null && output.exists()) {
            output.delete();
            output = null;
        }
    }

    public void stop() {
        Exception error = null;
        try {
            mediaRecorder.stop();
        } catch (Exception e) {
            error = e;
        }

        release();

        if (error == null && output != null && output.exists()) {
            String fileName = System.currentTimeMillis() + ".mp4";
            File newFile = new File(outputDir, fileName);
            output.renameTo(newFile);
            notifyCompleted(newFile);
        } else {
            notifyError(ErrorCode.RECORD_ERROR, error != null ? error.toString() : "output not found");
        }
    }

    private void release() {
        mainHandler.removeCallbacksAndMessages(null);
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }

        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
    }

    private void notifyCompleted(File file) {
        isRunning = false;

        if (callback == null) return;
        callback.onCompleted(file);
    }

    private void notifyError(int code, String msg) {
        isRunning = false;

        if (callback == null) return;
        callback.onError(code, msg);
    }

    private void notifyDurationChanged(long duration) {
        if (callback == null) return;
        ScreenRecordCallback temp = callback;
        mainHandler.post(() -> {
            temp.onDurationChanged(duration);
        });
    }

    public static class Builder {
        private int maxWidth = 720;
        private int maxHeight = 1280;
        private int bitrate = 720 * 1280 * 3;
        private int fps = 30;
        private boolean recordAudio = false;
        private int maxDurationSec = 60;
        private File outputDir;

        private final Context context;
        private final MediaProjection projection;

        private ScreenRecordCallback callback;

        public Builder(@NonNull Context context, @NonNull MediaProjection projection) {
            this.context = context;
            this.projection = projection;
            File cache = context.getExternalCacheDir() == null ? context.getCacheDir() : context.getExternalCacheDir();
            outputDir = new File(cache, "screen_record");
        }

        public Builder setConfig(RecordConfig config) {
            if (config.getMaxWidth() != null) {
                maxWidth = config.getMaxWidth();
            }
            if (config.getMaxHeight() != null) {
                maxHeight = config.getMaxHeight();
            }
            if (config.getBitrate() != null) {
                bitrate = config.getBitrate();
            }
            if (config.getFps() != null) {
                fps = config.getFps();
            }
            if (config.getRecordAudio() != null) {
                recordAudio = config.getRecordAudio();
            }
            if (config.getMaxDurationSec() != null) {
                maxDurationSec = config.getMaxDurationSec();
            }
            if (config.getOutputDir() != null) {
                outputDir = config.getOutputDir();
            }
            return this;
        }

        public Builder setCallback(@NonNull ScreenRecordCallback callback) {
            this.callback = callback;
            return this;
        }

        public ScreenRecorder build() {
            return new ScreenRecorder(this);
        }
    }
}
