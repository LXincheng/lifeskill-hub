package dev.lifeskill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LifeSkillBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(LifeSkillBackendApplication.class, args);
	}

}
