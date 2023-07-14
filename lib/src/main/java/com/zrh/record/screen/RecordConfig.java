package com.zrh.record.screen;

import java.io.File;
import java.io.Serializable;

/**
 * @author zrh
 * @date 2023/7/14
 */
public class RecordConfig implements Serializable {
    private Integer maxWidth;
    private Integer maxHeight;
    private Integer bitrate;
    private Integer fps;
    private Boolean recordAudio;
    private Integer maxDurationSec;
    private File outputDir;

    private int notificationIcon;
    private String notificationContent;

    public int getNotificationIcon() {
        return notificationIcon;
    }

    public void setNotificationIcon(int notificationIcon) {
        this.notificationIcon = notificationIcon;
    }

    public String getNotificationContent() {
        return notificationContent;
    }

    public void setNotificationContent(String notificationContent) {
        this.notificationContent = notificationContent;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public void setRecordAudio(boolean recordAudio) {
        this.recordAudio = recordAudio;
    }

    public void setMaxDurationSec(int maxDurationSec) {
        this.maxDurationSec = maxDurationSec;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    public Integer getMaxWidth() {
        return maxWidth;
    }

    public Integer getMaxHeight() {
        return maxHeight;
    }

    public Integer getBitrate() {
        return bitrate;
    }

    public Integer getFps() {
        return fps;
    }

    public Boolean getRecordAudio() {
        return recordAudio;
    }

    public Integer getMaxDurationSec() {
        return maxDurationSec;
    }

    public File getOutputDir() {
        return outputDir;
    }
}
