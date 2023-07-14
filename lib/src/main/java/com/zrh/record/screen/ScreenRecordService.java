package com.zrh.record.screen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;

/**
 * @author zrh
 * @date 2023/7/14
 */
public class ScreenRecordService extends Service {

    private static final String RESULT_CODE = "RESULT_CODE";
    private static final String RESULT_DATA = "RESULT_DATA";

    private static final String ACTION_START = "ACTION_START";
    private static final String ACTION_STOP = "ACTION_STOP";

    public static final String EVENT_ACTION = "EVENT_ACTION";
    public static final String EVENT_TYPE = "EVENT_TYPE";
    public static final int EVENT_DURATION_CHANGED = 0;
    public static final int EVENT_COMPLETED = 1;
    public static final int EVENT_ERROR = 2;

    public static final String DURATION = "DURATION";
    public static final String OUTPUT_PATH = "OUTPUT_PATH";
    public static final String ERROR_CODE = "ERROR_CODE";
    public static final String ERROR_MSG = "ERROR_MSG";

    public static final String RECORD_CONFIG = "RECORD_CONFIG";

    public static void start(Context context,
                             int resultCode,
                             Intent resultData,
                             RecordConfig config) {
        Intent intent = new Intent(context, ScreenRecordService.class);
        intent.putExtra(RESULT_CODE, resultCode);
        intent.putExtra(RESULT_DATA, resultData);
        intent.putExtra(RECORD_CONFIG, config);
        intent.setAction(ACTION_START);
        sendCommand(context, intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, ScreenRecordService.class);
        intent.setAction(ACTION_STOP);
        sendCommand(context, intent);
    }

    private static void sendCommand(Context context, Intent cmd) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(cmd);
        } else {
            context.startService(cmd);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private ScreenRecorder recorder;
    private boolean isRecording = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            start(intent);
        } else if (ACTION_STOP.equals(action)) {
            stop();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void start(Intent intent) {
        if (isRecording) return;
        isRecording = true;

        RecordConfig config = (RecordConfig) intent.getSerializableExtra(RECORD_CONFIG);

        createNotification(config);

        int resultCode = intent.getIntExtra(RESULT_CODE, 0);
        Intent resultData = intent.getParcelableExtra(RESULT_DATA);
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        MediaProjection projection = projectionManager.getMediaProjection(resultCode, resultData);
        if (projection == null) {
            stopSelf();
            notifyError(ErrorCode.RECORD_NOT_SUPPORT, "projection is null");
        } else {
            startRecorder(projection, config);
        }
    }

    private void startRecorder(MediaProjection projection, RecordConfig config) {
        ScreenRecorder.Builder builder = new ScreenRecorder.Builder(getApplicationContext(), projection);

        // config
        builder.setConfig(config);
        builder.setCallback(new ScreenRecordCallback() {
            @Override
            public void onCompleted(@NonNull File file) {
                notifyCompleted(file);
            }

            @Override
            public void onError(int code, @NonNull String msg) {
                notifyError(code, msg);
            }

            @Override
            public void onDurationChanged(long duration) {
                notifyDurationChanged(duration);
            }
        });
        recorder = builder.build();
        recorder.start();
    }

    private void notifyError(int code, String msg) {
        Intent event = new Intent(EVENT_ACTION);

        event.putExtra(EVENT_TYPE, EVENT_ERROR);
        event.putExtra(ERROR_CODE, code);
        event.putExtra(ERROR_MSG, msg);

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(event);
    }

    private void notifyDurationChanged(long duration) {
        Intent event = new Intent(EVENT_ACTION);

        event.putExtra(EVENT_TYPE, EVENT_DURATION_CHANGED);
        event.putExtra(DURATION, duration);

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(event);
    }

    private void notifyCompleted(File file) {
        Intent event = new Intent(EVENT_ACTION);

        event.putExtra(EVENT_TYPE, EVENT_COMPLETED);
        event.putExtra(OUTPUT_PATH, file.getAbsolutePath());

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(event);
    }

    private void createNotification(RecordConfig config) {
        int icon = config.getNotificationIcon();
        String content = config.getNotificationContent();
        if (content == null) {
            content = "Screen Recording......";
        }
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), icon);
        Notification.Builder builder = new Notification.Builder(getApplicationContext())
                .setLargeIcon(bitmap)
                .setSmallIcon(icon)
                .setContentText(content);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String id = "ScreenRecordService";
            String name = "ScreenRecordService";
            builder.setChannelId(id);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
        startForeground(777, builder.build());
    }

    private void stop() {
        if (recorder != null) {
            recorder.stop();
            recorder = null;
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recorder != null) {
            recorder.stop();
            recorder = null;
        }
    }
}
