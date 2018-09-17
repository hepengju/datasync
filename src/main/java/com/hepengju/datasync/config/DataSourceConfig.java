package com.hepengju.datasync.config;

import lombok.Data;

/**
 * 数据源配置类
 * 
 * @author hepengju
 *
 */
@Data
public class DataSourceConfig {

	// 四大连接参数
	private String driverClassName;
	private String jdbcUrl;
	private String username;
	private String password;

}
