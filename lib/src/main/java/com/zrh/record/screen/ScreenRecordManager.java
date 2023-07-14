package com.zrh.record.screen;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * @author zrh
 * @date 2023/7/14
 */
public class ScreenRecordManager {
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private static final Set<ScreenRecordCallback> callbacks = new HashSet<>();
    private static RecordConfig config;

    public static void init(Context context, RecordConfig config) {
        ScreenRecordManager.context = context.getApplicationContext();
        ScreenRecordManager.config = config;

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (!ScreenRecordService.EVENT_ACTION.equals(action)) return;
                int eventType = intent.getIntExtra(ScreenRecordService.EVENT_TYPE, -1);
                switch (eventType) {
                    case ScreenRecordService.EVENT_COMPLETED: {
                        String path = intent.getStringExtra(ScreenRecordService.OUTPUT_PATH);
                        notifyCompleted(new File(path));
                        break;
                    }
                    case ScreenRecordService.EVENT_DURATION_CHANGED: {
                        long duration = intent.getLongExtra(ScreenRecordService.DURATION, 0);
                        notifyDuration(duration);
                        break;
                    }
                    case ScreenRecordService.EVENT_ERROR: {
                        int code = intent.getIntExtra(ScreenRecordService.ERROR_CODE, 0);
                        String msg = intent.getStringExtra(ScreenRecordService.ERROR_MSG);
                        notifyError(code, msg);
                        break;
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(ScreenRecordService.EVENT_ACTION);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);
    }

    public static void addCallback(ScreenRecordCallback callback) {
        callbacks.add(callback);
    }

    public static void removeCallback(ScreenRecordCallback callback) {
        callbacks.remove(callback);
    }

    public static void start(FragmentActivity activity) {
        requestMediaProjection(activity, new MediaProjectionCallback() {
            @Override
            public void onResult(ActivityResult result) {
                internalStart(result);
            }

            @Override
            public void onError(Exception e) {
                notifyError(ErrorCode.PROJECTION_REQUEST_ERROR, "request MediaProjection error");
            }
        });
    }

    public static void stop() {
        ScreenRecordService.stop(context);
    }

    private static void notifyError(int code, String msg) {
        for (ScreenRecordCallback callback : callbacks) {
            callback.onError(code, msg);
        }
    }

    private static void notifyCompleted(File file) {
        for (ScreenRecordCallback callback : callbacks) {
            callback.onCompleted(file);
        }
    }

    private static void notifyDuration(long duration) {
        for (ScreenRecordCallback callback : callbacks) {
            callback.onDurationChanged(duration);
        }
    }

    private static void internalStart(ActivityResult result) {
        ScreenRecordService.start(context, result.getResultCode(), result.getData(), config);
    }

    private static void requestMediaProjection(FragmentActivity activity, MediaProjectionCallback callback) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag("MediaProjectionFragment");
        MediaProjectionFragment requestFragment;
        if (fragment == null) {
            requestFragment = new MediaProjectionFragment();
            fragmentManager.beginTransaction().add(requestFragment, "MediaProjectionFragment").commitNow();
        } else {
            requestFragment = (MediaProjectionFragment) fragment;
        }
        requestFragment.setCallback(callback);
        MediaProjectionManager manager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        requestFragment.request(manager.createScreenCaptureIntent());
    }

    public static class MediaProjectionFragment extends Fragment {
        private MediaProjectionCallback callback;

        private ActivityResultLauncher<Intent> launcher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        notifyCallback(result);
                    }
                });

        public void setCallback(MediaProjectionCallback callback) {
            this.callback = callback;
        }

        private void notifyCallback(ActivityResult result) {
            if (callback != null) callback.onResult(result);
        }

        public void request(Intent intent) {
            try {
                launcher.launch(intent);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        }
    }

    private interface MediaProjectionCallback {
        void onResult(ActivityResult result);

        void onError(Exception e);
    }
}
