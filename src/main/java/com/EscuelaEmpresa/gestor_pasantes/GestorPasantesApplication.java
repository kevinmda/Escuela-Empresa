package com.EscuelaEmpresa.gestor_pasantes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.EscuelaEmpresa.gestor_pasantes.config.FlywayEnvironmentInitializer;

@SpringBootApplication
public class GestorPasantesApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(GestorPasantesApplication.class);
		app.addListeners(new FlywayEnvironmentInitializer());
		app.run(args);
	}

}
