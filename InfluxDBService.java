package com.ruoyi.oil.service.impl;

import com.influxdb.v3.client.InfluxDBClient;
import com.influxdb.v3.client.Point;
import com.influxdb.v3.client.PointValues;
import com.influxdb.v3.client.query.QueryOptions;
import com.ruoyi.oil.service.IInfluxDBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.ObjectMapper; // 用于JSON解析
import com.ruoyi.oil.domain.DeviceDataMessage; // 导入你创建的POJO

@Service
public class InfluxDBService implements IInfluxDBService {

    private static final Logger logger = LoggerFactory.getLogger(InfluxDBService.class);
    private final ObjectMapper objectMapper = new ObjectMapper(); // 用于将JSON字符串转换为对象

    @Value("${influxdb.client3.host}")
    private String host;

    @Value("${influxdb.client3.token}")
    private String token;

    @Value("${influxdb.client3.database}")
    private String database;

    private InfluxDBClient client;

    @PostConstruct
    public void init() {
        logger.info("Initializing InfluxDB 3 native client for host: {}, database: {}", host, database);
        try {
            this.client = InfluxDBClient.getInstance(host, token.toCharArray(), database);
            logger.info("InfluxDB 3 native client initialized successfully.");
        } catch (Exception e) {
            logger.error("Failed to initialize InfluxDB 3 native client", e);
        }
    }

    @PreDestroy
    public void close() {
        if (this.client != null) {
            try {
                this.client.close();
                logger.info("InfluxDB 3 native client closed.");
            } catch (Exception e) {
                logger.error("Error closing InfluxDB 3 native client", e);
            }
        }
    }
    /**
     * 处理并写入从硬件设备接收到的JSON消息。
     *
     * @param jsonMessage 收到的JSON字符串
     * @return 如果处理和写入至少一个点成功则返回true，否则false
     */
    public boolean processAndWriteDeviceData(String jsonMessage) {
        if (client == null) {
            logger.error("InfluxDB 3 client is not initialized. Cannot process message.");
            return false;
        }

        try {
            DeviceDataMessage message = objectMapper.readValue(jsonMessage, DeviceDataMessage.class);
            logger.info("Parsed device data message: {}", message.getDeviceName());

            if (message.getItems() == null) {
                logger.warn("No items found in the message for device: {}", message.getDeviceName());
                return false;
            }

            List<Point> pointsToWrite = new ArrayList<>();
            String measurement = "fan_data"; // 你可以根据 deviceType 或其他逻辑动态设置

            // 通用标签，适用于该消息中的所有数据点
            Map<String, String> commonTags = new HashMap<>();
            commonTags.put("iotId", message.getIotId());
            commonTags.put("productKey", message.getProductKey());
            commonTags.put("deviceName", message.getDeviceName());
            if (message.getDeviceType() != null) {
                commonTags.put("deviceType", message.getDeviceType());
            }

            DeviceDataMessage.Items items = message.getItems();

            // 处理每个item
            if (items.getLightCurrent() != null && items.getLightCurrent().getValue() != null) {
                pointsToWrite.add(createPointFromItemData(measurement, commonTags,
                        "LightCurrent", items.getLightCurrent()));
            }
            if (items.getAx() != null && items.getAx().getValue() != null) {
                pointsToWrite.add(createPointFromItemData(measurement, commonTags,
                        "ax", items.getAx()));
            }
            if (items.getRoll() != null && items.getRoll().getValue() != null) {
                pointsToWrite.add(createPointFromItemData(measurement, commonTags,
                        "roll", items.getRoll()));
            }
            if (items.getAy() != null && items.getAy().getValue() != null) {
                pointsToWrite.add(createPointFromItemData(measurement, commonTags,
                        "ay", items.getAy()));
            }
            if (items.getTemperature() != null && items.getTemperature().getValue() != null) {
                pointsToWrite.add(createPointFromItemData(measurement, commonTags,
                        "temperature", items.getTemperature()));
            }
            if (items.getAz() != null && items.getAz().getValue() != null) {
                pointsToWrite.add(createPointFromItemData(measurement, commonTags,
                        "az", items.getAz()));
            }
            if (items.getPitch() != null && items.getPitch().getValue() != null) {
                pointsToWrite.add(createPointFromItemData(measurement, commonTags,
                        "pitch", items.getPitch()));
            }
            if (items.getYaw() != null && items.getYaw().getValue() != null) {
                pointsToWrite.add(createPointFromItemData(measurement, commonTags,
                        "yaw", items.getYaw()));
            }

            if (pointsToWrite.isEmpty()) {
                logger.warn("No valid data points to write from message for device: {}", message.getDeviceName());
                return false;
            }

            return writePoints(pointsToWrite);

        } catch (Exception e) {
            logger.error("Error processing and writing device data: {}", e.getMessage(), e);
            return false;
        }
    }
    /**
            * 辅助方法，从ItemData创建InfluxDB Point。
            */
    private Point createPointFromItemData(String measurement, Map<String, String> commonTags,
                                          String fieldName, DeviceDataMessage.ItemData itemData) {
        Point point = Point.measurement(measurement)
                .setTimestamp(Instant.ofEpochMilli(itemData.getTime())); // 从毫秒时间戳创建Instant

        commonTags.forEach(point::setTag);
        point.setField(fieldName, itemData.getValue()); // ItemData中的value是Double

        return point;
    }
    @Override
    public boolean writePoint(String measurement, Map<String, String> tags, Map<String, Object> fields, Instant timestamp) {
        if (client == null) {
            logger.error("InfluxDB 3 client is not initialized. Cannot write point.");
            return false;
        }
        try {
            Point point = Point.measurement(measurement);
            if (timestamp != null) {
                point.setTimestamp(timestamp);
            } else {
                point.setTimestamp(Instant.now());
            }

            if (tags != null) {
                tags.forEach(point::setTag);
            }

            if (fields != null) {
                fields.forEach((key, value) -> {
                    if (value instanceof Long) point.setField(key, (Long) value);
                    else if (value instanceof Double) point.setField(key, (Double) value);
                    else if (value instanceof Boolean) point.setField(key, (Boolean) value);
                    else if (value instanceof String) point.setField(key, (String) value);
                    else if (value instanceof Integer) point.setField(key, ((Integer)value).longValue());
                    else if (value instanceof Float) point.setField(key, ((Float)value).doubleValue());
                    else {
                        logger.warn("Unsupported field type for key '{}': {}. Converting to string.", key, value.getClass().getName());
                        point.setField(key, value.toString());
                    }
                });
            }
            client.writePoint(point); // 默认写入到客户端初始化时指定的database
            logger.debug("Successfully wrote point using influxdb3-java: {}", point.toLineProtocol());
            return true;
        } catch (Exception e) {
            logger.error("Error writing point with influxdb3-java: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean writePoints(List<Point> points) {
        if (client == null) {
            logger.error("InfluxDB 3 client is not initialized. Cannot write points.");
            return false;
        }
        if (points == null || points.isEmpty()) {
            logger.warn("Point list is empty or null. Nothing to write.");
            return true;
        }
        try {
            client.writePoints(points); // 默认写入到客户端初始化时指定的database
            logger.debug("Successfully wrote {} points using influxdb3-java.", points.size());
            return true;
        } catch (Exception e) {
            logger.error("Error writing points with influxdb3-java: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean writeRecord(String lineProtocol) {
        if (client == null) {
            logger.error("InfluxDB 3 client is not initialized. Cannot write record.");
            return false;
        }
        try {
            client.writeRecord(lineProtocol); // 默认写入到客户端初始化时指定的database
            logger.debug("Successfully wrote line protocol record using influxdb3-java.");
            return true;
        } catch (Exception e) {
            logger.error("Error writing line protocol record with influxdb3-java: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Stream<Object[]> queryRaw(String sqlQuery) {
        if (client == null) {
            logger.error("InfluxDB 3 client is not initialized. Cannot query.");
            return Stream.empty();
        }
        logger.debug("Executing SQL query (raw Object[]): {}", sqlQuery);
        try {
            return client.query(sqlQuery);
        } catch (Exception e) {
            logger.error("Error executing SQL query (raw Object[]): {}", e.getMessage(), e);
            return Stream.empty();
        }
    }

    @Override
    public Stream<Object[]> queryRaw(String sqlQuery, Map<String, Object> params) {
        if (client == null) {
            logger.error("InfluxDB 3 client is not initialized. Cannot query.");
            return Stream.empty();
        }
        logger.debug("Executing parametrized SQL query (raw Object[]): {} with params: {}", sqlQuery, params);
        try {
            return client.query(sqlQuery, params);
        } catch (Exception e) {
            logger.error("Error executing parametrized SQL query (raw Object[]): {}", e.getMessage(), e);
            return Stream.empty();
        }
    }

    @Override
    public Stream<Object[]> queryRawWithInfluxQL(String influxQLQuery) {
        if (client == null) {
            logger.error("InfluxDB 3 client is not initialized. Cannot query.");
            return Stream.empty();
        }
        logger.debug("Executing InfluxQL query (raw Object[]): {}", influxQLQuery);
        try {
            return client.query(influxQLQuery, QueryOptions.INFLUX_QL);
        } catch (Exception e) {
            logger.error("Error executing InfluxQL query (raw Object[]): {}", e.getMessage(), e);
            return Stream.empty();
        }
    }

    @Override
    public Stream<PointValues> queryPoints(String sqlQuery) {
        if (client == null) {
            logger.error("InfluxDB 3 client is not initialized. Cannot query points.");
            return Stream.empty();
        }
        logger.debug("Executing SQL query for PointValues: {}", sqlQuery);
        try {
            return client.queryPoints(sqlQuery);
        } catch (Exception e) {
            logger.error("Error executing SQL query for PointValues: {}", e.getMessage(), e);
            return Stream.empty();
        }
    }

    @Override
    public Stream<PointValues> queryPoints(String sqlQuery, Map<String, Object> params) {
        if (client == null) {
            logger.error("InfluxDB 3 client is not initialized. Cannot query points with params.");
            return Stream.empty();
        }
        logger.warn("Executing parametrized SQL query for PointValues. " +
                "The influxdb3-java client.queryPoints API in README (1.0.0) " +
                "does not show direct Map-based parameterization for PointValues stream. " +
                "This method might require constructing SQL with parameters manually if not supported by API. " +
                "Falling back to non-parametrized queryPoints for now if params are present and non-empty, or use queryRaw.");

        // 根据 README, client.query(sql, params) 返回 Stream<Object[]>
        // client.queryPoints(sql) 返回 Stream<PointValues> 但没有 params map
        // 如果确实需要参数化并得到 PointValues，需要检查库是否有其他方法，或手动处理
        if (params != null && !params.isEmpty()) {
            logger.error("Parameter-map based queryPoints is not directly supported by this example based on README. " +
                    "Use queryRaw(sql, params) and process Object[] or construct SQL string with parameters manually for queryPoints.");
            // 或者可以尝试动态构建SQL字符串，但要注意SQL注入风险
            // String finalSql = replaceQueryParameters(sqlQuery, params); // 你需要实现这个方法
            // return client.queryPoints(finalSql);
            return Stream.empty(); // 或者抛出 UnsupportedOperationException
        }
        try {
            return client.queryPoints(sqlQuery);
        } catch (Exception e) {
            logger.error("Error executing SQL query for PointValues (fallback non-parametrized): {}", e.getMessage(), e);
            return Stream.empty();
        }
    }

    // 示例方法 (与之前一致，只是调用的方法现在是基于influxdb3-java的)
    public void writeFanMetricsExample() {
        String measurement = "fan_sensor_data";
        Map<String, String> tags = new HashMap<>();
        tags.put("tunnel_id", "T002");
        tags.put("fan_id", "Fan_D01");
        Map<String, Object> fields = new HashMap<>();
        fields.put("vibration_x", 0.33);
        fields.put("temperature_celsius", 31.5);
        fields.put("active_power", 2.5);
        writePoint(measurement, tags, fields, Instant.now());
    }

    public void queryFanMetricsExample() {
        String sql = "SELECT time, tunnel_id, fan_id, vibration_x, temperature_celsius, active_power " +
                "FROM fan_sensor_data WHERE fan_id = 'Fan_D01' AND time >= now() - interval '1 hour' " +
                "ORDER BY time DESC LIMIT 3";

        logger.info("Querying with SQL for PointValues stream (Recommended for typed access):");
        try (Stream<PointValues> stream = queryPoints(sql)) { // 使用实现了的 queryPoints
            stream.forEach(
                    (PointValues p) -> {
                        // 根据你的SELECT语句，你知道这些字段和标签是存在的
                        System.out.printf("| Time: %-30s | Tunnel: %-8s | Fan: %-8s | VibX: %-8.3f | Temp: %-8.2f | Power: %-8.2f |%n",
                                p.getTimestamp(), // 主时间戳
                                p.getTag("tunnel_id"),
                                p.getTag("fan_id"),
                                p.getField("vibration_x", Double.class),
                                p.getField("temperature_celsius", Double.class),
                                p.getField("active_power", Double.class)
                        );
                    });
        } catch (Exception e) {
            logger.error("Error in queryFanMetricsExample (queryPoints): ", e);
        }
        System.out.println("----------------------------------------------------------------------------------------------------------");

        logger.info("Querying with SQL for raw Object[] stream (manual handling based on SELECT order):");
        // 列顺序: time, tunnel_id, fan_id, vibration_x, temperature_celsius, active_power
        try (Stream<Object[]> stream = queryRaw(sql)) { // 使用实现了的 queryRaw
            stream.forEach(row -> {
                if (row != null && row.length == 6) {
                    System.out.printf("| Time: %-30s | Tunnel: %-8s | Fan: %-8s | VibX: %-8s | Temp: %-8s | Power: %-8s |%n",
                            row[0], row[1], row[2], row[3], row[4], row[5]);
                } else {
                    logger.warn("Unexpected row format in raw query: {}", (Object)row);
                }
            });
        }  catch (Exception e) {
            logger.error("Error in queryFanMetricsExample (queryRaw): ", e);
        }
        System.out.println("----------------------------------------------------------------------------------------------------------");
    }
}