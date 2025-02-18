package com.easylive.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.easylive"})
@MapperScan(basePackages = {"com.easylive.mappers"})
public class EasyliveAdminRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasyliveAdminRunApplication.class, args);
    }
}
