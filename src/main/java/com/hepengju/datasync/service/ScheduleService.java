package com.hepengju.datasync.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.hepengju.datasync.config.ApplicationConfig;
import com.hepengju.datasync.config.DataSyncConfig;

/**
 * 调用服务
 * 
 * @author hepengju
 *
 */
@Service
public class ScheduleService {
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired ApplicationConfig applicationConfig;
	@Autowired DataSyncService dataSyncService;
	
	@Value("${runOnStart}")    boolean runOnStart;    //是否启动运行
	@Value("${runOnSchedule}") boolean runOnSchedule; //是否启用调度
	
	/**
	 * 启动时运行
	 */
	@PostConstruct
	public void scheduleOnApplicationStart() {
		if(runOnStart) {
			logger.info("runOnStart is true, now begin");
			schedule();
			logger.info("runOnStart end");
		}else {
			logger.info("runOnStart is false, wait for schedule");
		}
		
		if(!runOnSchedule) {
			logger.info("runOnSchedule is false, now will exit");
			System.exit(0);
		}
	}
	
	/**
	 * 定时调度
	 */
	@Scheduled(cron = "${scheduleCron}")
	public synchronized void schedule() {
		logger.info("schedule begin");
		
		/*
		 * 1.取得所有数据源名称
		 * 2.打开所有数据库连接池
		 * 3.逐一执行数据移动任务
		 * 4.关闭所有数据库连接池
		 */
		List<DataSyncConfig> dataSyncConfigList = applicationConfig.getDataSyncConfigList();
		Set<String> dbNameSet = dataSyncConfigList.stream().map(DataSyncConfig::getSrcDbName).collect(Collectors.toSet());
		dbNameSet.addAll(dataSyncConfigList.stream().map(DataSyncConfig::getTarDbName).collect(Collectors.toSet()));
		dbNameSet.forEach(dataSyncService::createDataSource);
		
		//数据移动
		dataSyncConfigList.forEach(dataSyncService::dataSync);
		
		dbNameSet.forEach(dataSyncService::destroyDataSource);
		logger.info("schedule end");
	}
	
}
