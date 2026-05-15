package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"ru.practicum.ewm", "ru.practicum"})
public class ExploreWithMeApp {
    public static void main(String[] args) {
        SpringApplication.run(ExploreWithMeApp.class, args);
    }
}
