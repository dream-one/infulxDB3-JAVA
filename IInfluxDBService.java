package com.ruoyi.oil.service;

import com.influxdb.v3.client.Point;
import com.influxdb.v3.client.PointValues;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface IInfluxDBService {

    boolean writePoint(String measurement, Map<String, String> tags, Map<String, Object> fields, Instant timestamp);

    boolean writePoints(List<Point> points);

    boolean writeRecord(String lineProtocol);

    /**
     * 执行SQL查询并返回原始的Object数组流。
     * 调用者负责关闭Stream，并根据SQL查询的SELECT子句解析Object[]中的数据。
     *
     * @param sqlQuery SQL查询语句
     * @return 代表结果行的Object数组流
     */
    Stream<Object[]> queryRaw(String sqlQuery);

    /**
     * 执行参数化的SQL查询并返回原始的Object数组流。
     * 调用者负责关闭Stream，并根据SQL查询的SELECT子句解析Object[]中的数据。
     *
     * @param sqlQuery 参数化的SQL查询语句 (例如 "SELECT * FROM table WHERE tag1 = $param1")
     * @param params   参数名和值的Map
     * @return 代表结果行的Object数组流
     */
    Stream<Object[]> queryRaw(String sqlQuery, Map<String, Object> params);

    /**
     * 执行InfluxQL查询并返回原始的Object数组流。
     * 调用者负责关闭Stream，并根据InfluxQL查询的SELECT子句解析Object[]中的数据。
     *
     * @param influxQLQuery InfluxQL查询语句
     * @return 代表结果行的Object数组流
     */
    Stream<Object[]> queryRawWithInfluxQL(String influxQLQuery);
    boolean processAndWriteDeviceData(String jsonMessage);
    /**
     * 执行SQL查询并返回 PointValues 流，方便按类型获取字段和标签。
     * 调用者负责关闭Stream。
     *
     * @param sqlQuery SQL查询语句
     * @return PointValues对象的流
     */
    Stream<PointValues> queryPoints(String sqlQuery);

    /**
     * 执行参数化的SQL查询并返回 PointValues 流。
     * 注意：influxdb3-java 1.0.0 的 queryPoints API 可能不直接支持 Map 形式的参数化。
     * 此方法目前可能回退到非参数化版本或需要调用者自行构造含参数的SQL。
     *
     * @param sqlQuery 参数化的SQL查询语句
     * @param params   参数名和值的Map (其在此方法中的支持取决于客户端库的实际能力)
     * @return PointValues对象的流
     */
    Stream<PointValues> queryPoints(String sqlQuery, Map<String, Object> params);
}