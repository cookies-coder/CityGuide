package com.city.guide;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.city.guide.mapper")
@SpringBootApplication
public class CityGuideApplication {

    public static void main(String[] args) {
        SpringApplication.run(CityGuideApplication.class, args);
    }

}

