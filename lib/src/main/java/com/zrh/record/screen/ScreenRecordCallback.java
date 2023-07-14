package com.zrh.record.screen;

import androidx.annotation.NonNull;

import java.io.File;

/**
 * @author zrh
 * @date 2023/7/14
 */
public interface ScreenRecordCallback {
    void onCompleted(@NonNull File file);

    void onError(int code, @NonNull String msg);

    void onDurationChanged(long duration);
}
