package com.hepengju.datasync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 主程序
 * 
 * @author hepengju
 *
 */
@EnableScheduling
@SpringBootApplication
public class DataSyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataSyncApplication.class, args);
	}
}
