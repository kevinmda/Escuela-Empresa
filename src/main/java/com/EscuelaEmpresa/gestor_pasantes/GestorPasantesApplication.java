package com.EscuelaEmpresa.gestor_pasantes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // sin esto, @Scheduled en ActivacionScheduler no se ejecutaria nunca
public class GestorPasantesApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestorPasantesApplication.class, args);
	}

}
