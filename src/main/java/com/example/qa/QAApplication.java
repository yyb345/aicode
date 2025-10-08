package com.example.qa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * QA应用启动类
 */
@SpringBootApplication(scanBasePackages = {"com.example.qa"})
public class QAApplication {

    public static void main(String[] args) {
        SpringApplication.run(QAApplication.class, args);
    }
}
