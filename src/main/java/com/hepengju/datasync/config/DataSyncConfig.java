package com.hepengju.datasync.config;

import lombok.Data;

/**
 * 数据同步配置类
 * 
 * @author hepengju
 *
 */
@Data
public class DataSyncConfig {

	//源数据源及表名
	private String srcDbName;
	private String srcTableName;
	
	//目标数据源及表名
	private String tarDbName;
	private String tarTableName;
	
	//移动条数的最大值
	private Integer moveMaxCount;
}
