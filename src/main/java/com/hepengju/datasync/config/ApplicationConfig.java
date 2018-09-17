package com.hepengju.datasync.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 应用程序配置类
 * 
 * @author hepengju
 *
 */
@Component @Data
@ConfigurationProperties
public class ApplicationConfig {

	private Map<String,DataSourceConfig> dataSourceConfigMap;  //数据源配置
	private List<DataSyncConfig> dataSyncConfigList;           //数据同步配置
	
}
