package com.EscuelaEmpresa.gestor_pasantes.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

/**
 * Corre las migraciones de Flyway a mano, escuchando el evento MAS TEMPRANO
 * posible del arranque (justo despues de resolver el Environment, antes de
 * crear ningun bean). Hace falta porque Spring Boot 4.1 dejo de traer la
 * auto-configuracion de Flyway: sin esto, agregar flyway-core al pom.xml no
 * alcanza, y Hibernate (ddl-auto=validate) fallaria contra un esquema viejo.
 *
 * No depende de ningun bean de Spring (DataSource, JPA, etc.) para evitar
 * quedar atado al orden interno de arranque de esta version.
 */
public class FlywayEnvironmentInitializer implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Environment env = event.getEnvironment();
        if (!env.getProperty("spring.flyway.enabled", Boolean.class, true)) {
            return;
        }
        String url = env.getProperty("spring.datasource.url");
        if (url == null || !url.startsWith("jdbc:mysql")) {
            return;
        }
        String usuario = env.getProperty("spring.datasource.username", "root");
        String contrasena = env.getProperty("spring.datasource.password", "");

        Flyway.configure()
                .dataSource(url, usuario, contrasena)
                .baselineOnMigrate(env.getProperty("spring.flyway.baseline-on-migrate", Boolean.class, false))
                .baselineVersion(env.getProperty("spring.flyway.baseline-version", "1"))
                .baselineDescription(env.getProperty("spring.flyway.baseline-description", "baseline"))
                .load()
                .migrate();
    }
}
