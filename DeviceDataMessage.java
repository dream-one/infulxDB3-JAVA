package com.ruoyi.oil.domain;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty; // 如果需要处理大小写不一致的key

/*
* 设备数据消息类
* 接收到设备数据时，会解析成该类
* */
public class DeviceDataMessage {
    private String deviceType;
    private String iotId;
    private String requestId;
    private Map<String, Object> checkFailedData; // 可以根据实际情况定义更具体的类
    private String productKey;
    private Long gmtCreate; // 接收原始long类型时间戳
    private String deviceName;
    private Items items;

    // Getters and Setters
    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getIotId() {
        return iotId;
    }

    public void setIotId(String iotId) {
        this.iotId = iotId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Map<String, Object> getCheckFailedData() {
        return checkFailedData;
    }

    public void setCheckFailedData(Map<String, Object> checkFailedData) {
        this.checkFailedData = checkFailedData;
    }

    public String getProductKey() {
        return productKey;
    }

    public void setProductKey(String productKey) {
        this.productKey = productKey;
    }

    public Long getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Long gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Items getItems() {
        return items;
    }

    public void setItems(Items items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "DeviceDataMessage{" +
                "deviceType='" + deviceType + '\'' +
                ", iotId='" + iotId + '\'' +
                ", productKey='" + productKey + '\'' +
                ", gmtCreate=" + gmtCreate +
                ", deviceName='" + deviceName + '\'' +
                ", items=" + items +
                '}';
    }

    // 内部类表示 "items" 结构
    public static class Items {
        @JsonProperty("LightCurrent") // Jackson注解，用于匹配JSON中的键名
        private ItemData lightCurrent;
        private ItemData ax;
        private ItemData roll;
        private ItemData ay;
        private ItemData temperature;
        private ItemData az;
        private ItemData pitch;
        private ItemData yaw;

        // Getters and Setters
        public ItemData getLightCurrent() {
            return lightCurrent;
        }

        public void setLightCurrent(ItemData lightCurrent) {
            this.lightCurrent = lightCurrent;
        }

        public ItemData getAx() {
            return ax;
        }

        public void setAx(ItemData ax) {
            this.ax = ax;
        }

        public ItemData getRoll() {
            return roll;
        }

        public void setRoll(ItemData roll) {
            this.roll = roll;
        }

        public ItemData getAy() {
            return ay;
        }

        public void setAy(ItemData ay) {
            this.ay = ay;
        }

        public ItemData getTemperature() {
            return temperature;
        }

        public void setTemperature(ItemData temperature) {
            this.temperature = temperature;
        }

        public ItemData getAz() {
            return az;
        }

        public void setAz(ItemData az) {
            this.az = az;
        }

        public ItemData getPitch() {
            return pitch;
        }

        public void setPitch(ItemData pitch) {
            this.pitch = pitch;
        }

        public ItemData getYaw() {
            return yaw;
        }

        public void setYaw(ItemData yaw) {
            this.yaw = yaw;
        }

        @Override
        public String toString() {
            return "Items{" +
                    "lightCurrent=" + lightCurrent +
                    ", ax=" + ax +
                    ", roll=" + roll +
                    // ... (其他字段)
                    '}';
        }
    }

    // 内部类表示每个item的 "value" 和 "time"
    public static class ItemData {
        private Double value; // 假设所有value都是数字，用Double兼容整数和浮点数
        private Long time;    // 接收原始long类型时间戳

        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }

        public Long getTime() {
            return time;
        }

        public void setTime(Long time) {
            this.time = time;
        }

        @Override
        public String toString() {
            return "{value=" + value + ", time=" + time + '}';
        }
    }
}


