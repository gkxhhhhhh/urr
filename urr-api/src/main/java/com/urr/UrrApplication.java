package com.urr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.net.InetAddress;

@Slf4j
@SpringBootApplication
@MapperScan(basePackages = "com.urr.infra.mapper")
public class UrrApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(UrrApplication.class, args);
        ConfigurableEnvironment environment = run.getEnvironment();
        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            log.info(
                    "\r\n\r\n==============================================================\r\n"
                            + "\r\n   启动完成，访问地址：" + hostAddress + ":"
                            + environment.getProperty("server.port")
                            + "\r\n\r\n==============================================================\r\n");
        }catch (Exception e){
            e.printStackTrace();
            log.error("服务启动成功 ip获取失败");
        }
    }
}
