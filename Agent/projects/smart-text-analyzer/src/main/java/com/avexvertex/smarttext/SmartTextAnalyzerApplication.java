package com.avexvertex.smarttext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class SmartTextAnalyzerApplication {

    // Environment：Spring 提供给我们的“当前运行环境信息查询器”，它里面能读取很多配置
    // 什么配置文件啊、环境变量啊、JVM 参数、系统属性、以及Spring 自己运行时产生的属性等
    private final Environment environment;

    public static void main(String[] args) {
        SpringApplication.run(SmartTextAnalyzerApplication.class, args);
    }

    /**
     * Spring Boot 完全启动成功后执行
     * 在控制台打印 Swagger UI 地址，方便直接点击访问
     *
     * @EventListener : 这个方法要监听 Spring 事件，具体监听什么事件，注解里指定，或者别的方式(此处不提及)
     * ApplicationReadyEvent : 是一个普通的 Spring Boot 事件类型，继承 SpringApplicationEvent，表示 “应用启动完成事件”
     * 这里合起来是：当应用启动完成事件发生时，执行这个方法
     */
    @EventListener(ApplicationReadyEvent.class)
    public void printSwaggerUrl() {

        // 获取当前应用实际监听的端口
        String port = environment.getProperty("local.server.port", "8080");

        log.info("Swagger UI: http://localhost:{}/swagger-ui/index.html", port);
    }

}
