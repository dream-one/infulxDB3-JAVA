package com.ruoyi.oil.controller;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.oil.domain.IotAlert;
import com.ruoyi.oil.domain.IotDevice;
import com.ruoyi.oil.domain.dto.DeptsDTO;
import com.ruoyi.oil.domain.dto.DeviceQuery;
import com.ruoyi.oil.domain.dto.DeviceTopic;
import com.ruoyi.oil.domain.dto.DeviceTsDTO;
import com.ruoyi.oil.domain.vo.DeviceInfoVO;
import com.ruoyi.oil.service.IInfluxDBService;
import com.ruoyi.oil.service.IIotDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import com.influxdb.v3.client.PointValues; // 如果直接处理Stream<PointValues>
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.LinkedHashMap;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Anonymous
@RestController
@RequestMapping("/iot/data")
public class IotDataController extends BaseController {
    @Autowired
    private IInfluxDBService influxDBService;

    /**
     * 获取特定风机在指定时间范围内的所有指标 (使用 queryPoints)
     */
    @Anonymous
    @GetMapping("/query")
    public ResponseEntity<List<Map<String, Object>>> getDataByFan(
            @RequestParam String deviceName,
            @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
            @RequestParam(defaultValue = "100") int limit
    ) {
        // 基础SQL查询语句
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT time, ")
                .append("\"deviceName\", ") // <--- 修改这里
                .append("\"iotId\", ")      // <--- 建议所有tag和field名都用双引号，特别是如果它们不是纯小写
                .append("\"productKey\", ")
                .append("ax, ay, az, roll, pitch, yaw, temperature, \"LightCurrent\" "); // LightCurrent也用引号
        sqlBuilder.append("FROM ").append("fan_data").append(" ");
        sqlBuilder.append("WHERE \"deviceName\" = '").append(escapeSqlIdentifier(deviceName)).append("'");        // 添加时间过滤条件（如果提供了时间参数）
        if (startTime != null) {
            sqlBuilder.append(" AND time >= '").append(startTime.toInstant().toString()).append("'");
        }
        if (endTime != null) {
            sqlBuilder.append(" AND time < '").append(endTime.toInstant().toString()).append("'");
        }
        sqlBuilder.append(" ORDER BY time DESC LIMIT ").append(limit);

        String sqlQuery = sqlBuilder.toString();

        List<Map<String, Object>> results = new ArrayList<>();
        try (Stream<PointValues> stream = influxDBService.queryPoints(sqlQuery)) {
            results = stream.map(pv -> {
                Map<String, Object> row = new LinkedHashMap<>();
                if (pv.getTimestamp() != null) row.put("time", pv.getTimestamp());

                // 根据你的SELECT语句明确提取
                if (pv.getTag("deviceName") != null) row.put("deviceName", pv.getTag("deviceName"));
                if (pv.getTag("iotId") != null) row.put("iotId", pv.getTag("iotId"));
                if (pv.getTag("productKey") != null) row.put("productKey", pv.getTag("productKey"));

                putFieldIfPresent(pv, row, "ax", Double.class);
                putFieldIfPresent(pv, row, "ay", Double.class);
                putFieldIfPresent(pv, row, "az", Double.class);
                putFieldIfPresent(pv, row, "roll", Double.class);
                putFieldIfPresent(pv, row, "pitch", Double.class);
                putFieldIfPresent(pv, row, "yaw", Double.class);
                putFieldIfPresent(pv, row, "temperature", Double.class);
                putFieldIfPresent(pv, row, "LightCurrent", Double.class);

                return row;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            // log error
            return ResponseEntity.status(500).body(null);
        }
        return ResponseEntity.ok(results);
    }

    // 辅助方法，用于从 PointValues 安全地获取字段并放入 Map
    private <T> void putFieldIfPresent(PointValues pv, Map<String, Object> map, String fieldName, Class<T> type) {
        try {
            T value = pv.getField(fieldName, type);
            if (value != null) {
                map.put(fieldName, value);
            }
        } catch (Exception e) {
            // 字段不存在或类型不匹配时，getField会抛异常
            // logger.trace("Field '{}' not found or type mismatch in PointValues", fieldName);
        }
    }

    // 非常基础的SQL标识符清理，防止简单注入。生产环境需要更健壮的方案或使用预编译语句。
    private String escapeSqlIdentifier(String identifier) {
        if (identifier == null) return null;
        return identifier.replace("'", "''");
    }
}
