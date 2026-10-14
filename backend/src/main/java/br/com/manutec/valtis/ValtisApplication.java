package br.com.manutec.valtis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
// Liga o @Scheduled, usado pelo worker do Outbox da agenda (D-13).
@EnableScheduling
public class ValtisApplication {

	public static void main(String[] args) {
		SpringApplication.run(ValtisApplication.class, args);
	}

}
