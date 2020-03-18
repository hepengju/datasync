package com.hepengju.datasync.service;

import java.lang.reflect.Method;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hepengju.datasync.config.ApplicationConfig;
import com.hepengju.datasync.config.DataSourceConfig;
import com.hepengju.datasync.config.DataSyncConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.util.CollectionUtils;

/**
 * 数据同步服务
 *
 * @author hepengju
 */
@Service
public class DataSyncService {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private Map<String, DataSource> cacheDataSourceMap = new HashMap<String, DataSource>();

    @Autowired
    ApplicationConfig applicationConfig;

    /**
     * 数据移动(同步)
     */
    public void dataSync(DataSyncConfig dataSyncConfig) {
        logger.info("dataSync begin: " + dataSyncConfig);

        try (Connection srcConn = getConnection(dataSyncConfig.getSrcDbName());
             Connection tarConn = getConnection(dataSyncConfig.getTarDbName());) {

            String srcTableName = dataSyncConfig.getSrcTableName();
            String tarTableName = dataSyncConfig.getTarTableName();

            //1.清空目标表
            tarConn.setAutoCommit(false);
            PreparedStatement delPs = tarConn.prepareStatement("DELETE FROM " + tarTableName);
            int delCount = delPs.executeUpdate();
            logger.info(srcTableName + " delCount: " + delCount);
            tarConn.commit();

            // 20200317 hepengju 取两个表的列交集, 并注意排序问题(因为实际开发环境和生产环境的列数量和顺序不一致)
			List<String> tarColList = getColumnNameList(tarConn, tarTableName);
			List<String> srcColList = getColumnNameList(srcConn, srcTableName);
			tarColList.retainAll(srcColList); // 取交集
			String colComma = tarColList.stream().collect(Collectors.joining(","));
			String colInsertComma = tarColList.stream().map(n -> '`' + n + '`').collect(Collectors.joining(","));

			// 拼接INSERT语句
			String questions = tarColList.stream().map(c -> "?").collect(Collectors.joining(",")); //问号逗号分隔
			String insSql = "INSERT INTO " + dataSyncConfig.getSrcTableName() + " (" + colInsertComma + ") VALUES (" + questions + ")";

			//2.取得源表的结果集
            PreparedStatement selPs = srcConn.prepareStatement("SELECT " + colComma + " FROM " + srcTableName);
            ResultSet rst = selPs.executeQuery();

            //3.批量插入目标表
            PreparedStatement insPs = tarConn.prepareStatement(insSql);
            int readyCount = 0;
            int batchCount = 1000;
            while (rst.next()) {
                for (int i = 1; i <= tarColList.size(); i++) {
                    if (rst.getObject(i) instanceof oracle.sql.TIMESTAMP) {
                        insPs.setObject(i, getOracleTimestamp(rst.getObject(i)));
                    } else {
                        insPs.setObject(i, rst.getObject(i));
                    }

                }
                insPs.addBatch();
                readyCount++;

                //最大数目设置
                if (dataSyncConfig.getMoveMaxCount() != null && readyCount >= dataSyncConfig.getMoveMaxCount()) {
                    logger.info(tarTableName + " readyCount greater than moveMaxCount now");
                    break;
                }

                if (readyCount % batchCount == 0) {
                    insPs.executeBatch();
                    logger.info(tarTableName + " readyCount: " + readyCount);
                }
            }

            insPs.executeBatch();
            tarConn.commit();
            logger.info(tarTableName + " readyCount: " + readyCount);

        } catch (SQLException  e) {
            e.printStackTrace();
        }
        logger.info("dataSync end: " + dataSyncConfig);
    }


    public List<String> getColumnNameList(Connection conn, String tableName) throws SQLException {
		PreparedStatement selPs = conn.prepareStatement("SELECT * FROM " + tableName + " WHERE 1 = 0");
		ResultSet rst = selPs.executeQuery();
		ResultSetMetaData metaData = rst.getMetaData();
		List<String> columnNameList = new ArrayList<>();
		int columnCount = rst.getMetaData().getColumnCount();
		for (int i = 1; i <= columnCount; i++) {
			String columnName = metaData.getColumnLabel(i).toUpperCase(); // 取label
			columnNameList.add(columnName);
		}

		return columnNameList;
	}
    /**
     * 取得数据库连接
     */
    public Connection getConnection(String dbName) throws SQLException {
        if (!cacheDataSourceMap.containsKey(dbName)) createDataSource(dbName);
        return cacheDataSourceMap.get(dbName).getConnection();
    }

    /**
     * 创建数据源连接池
     */
    public DataSource createDataSource(String dbName) {
        if (cacheDataSourceMap.containsKey(dbName)) return cacheDataSourceMap.get(dbName);

        DataSourceConfig dataSourceConfig = applicationConfig.getDataSourceConfigMap().get(dbName);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(dataSourceConfig.getDriverClassName());
        dataSource.setJdbcUrl(dataSourceConfig.getJdbcUrl());
        dataSource.setUsername(dataSourceConfig.getUsername());
        dataSource.setPassword(dataSourceConfig.getPassword());

        cacheDataSourceMap.put(dbName, dataSource);
        return dataSource;
    }

    /**
     * 销毁数据源连接池
     */
    public void destroyDataSource(String dbName) {
        if (cacheDataSourceMap.containsKey(dbName)) {
            HikariDataSource dataSource = (HikariDataSource) cacheDataSourceMap.get(dbName);
            dataSource.close();
        }
    }


    private Timestamp getOracleTimestamp(Object value) {
        try {
            Class clz = value.getClass();
            Method m = clz.getMethod("timestampValue");
            //m = clz.getMethod("timeValue", null); 时间类型
            //m = clz.getMethod("dateValue", null); 日期类型
            return (Timestamp) m.invoke(value);

        } catch (Exception e) {
            return null;
        }
    }
}
