package com.school.hotel;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan(
        value = "com.school.hotel.mapper",
        lazyInitialization = "${mybatis-plus.lazy-initialization:false}"
)
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class HotelApplication {
    public static void main(String[] args) {
        SpringApplication.run(HotelApplication.class, args);
    }
}
