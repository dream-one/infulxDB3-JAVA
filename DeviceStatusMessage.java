package com.ruoyi.oil.domain;
import com.fasterxml.jackson.annotation.JsonProperty; // 如果需要处理大小写不一致的key

public class DeviceStatusMessage {

    private String lastTime;
    private String iotId;
    private String utcLastTime;
    private String clientIp;
    private String utcTime;
    private String time;
    private String productKey;
    private String deviceName;
    private String status; // "online", "offline" etc.

    // Getters and Setters
    public String getLastTime() {
        return lastTime;
    }

    public void setLastTime(String lastTime) {
        this.lastTime = lastTime;
    }

    public String getIotId() {
        return iotId;
    }

    public void setIotId(String iotId) {
        this.iotId = iotId;
    }

    public String getUtcLastTime() {
        return utcLastTime;
    }

    public void setUtcLastTime(String utcLastTime) {
        this.utcLastTime = utcLastTime;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUtcTime() {
        return utcTime;
    }

    public void setUtcTime(String utcTime) {
        this.utcTime = utcTime;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getProductKey() {
        return productKey;
    }

    public void setProductKey(String productKey) {
        this.productKey = productKey;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "DeviceStatusMessage{" +
                "iotId='" + iotId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", status='" + status + '\'' +
                ", utcTime='" + utcTime + '\'' +
                '}';
    }
}