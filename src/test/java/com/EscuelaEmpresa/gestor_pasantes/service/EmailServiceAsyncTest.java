package com.EscuelaEmpresa.gestor_pasantes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.EscuelaEmpresa.gestor_pasantes.config.AsyncConfig;

// Prueba que enviarCorreoRecuperacion realmente corre en otro hilo, que es la
// unica razon por la que OlvideContrasenaController puede responder a la misma
// velocidad exista o no la cuenta (ver OlvideContrasenaControllerEnumeracionTest,
// que prueba esa otra mitad con un mock y no puede ver esto: un mock no sabe
// si el metodo real es @Async).
//
// Sin @EnableAsync -- o si a alguien se le escapa el @Async del metodo en un
// refactor -- este test se cuelga hasta el timeout y falla en vez de pasar en
// silencio.
@SpringBootTest(classes = { AsyncConfig.class, EmailServiceAsyncTest.Config.class })
class EmailServiceAsyncTest {

    @TestConfiguration
    static class Config {
        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }

        @Bean
        EmailService emailService(JavaMailSender mailSender) {
            return new EmailService(mailSender);
        }
    }

    @Autowired
    private EmailService emailService;

    @Autowired
    private JavaMailSender javaMailSender;

    @Test
    void enviarCorreoRecuperacion_noBloqueaAlLlamador() throws InterruptedException {
        CountDownLatch elEnvioEmpezo = new CountDownLatch(1);
        CountDownLatch dejarTerminarElEnvio = new CountDownLatch(1);

        doAnswer(invocacion -> {
            elEnvioEmpezo.countDown();
            // No vuelve hasta que el test se lo permite. Si enviarCorreoRecuperacion
            // fuera sincronico, la llamada de mas abajo quedaria trabada aca
            // tambien, y "duracionMs" nunca bajaria de los 2 segundos del await.
            dejarTerminarElEnvio.await(2, TimeUnit.SECONDS);
            return null;
        }).when(javaMailSender).send(any(SimpleMailMessage.class));

        long inicio = System.nanoTime();
        emailService.enviarCorreoRecuperacion("alguien@ejemplo.com", "123456");
        long duracionMs = (System.nanoTime() - inicio) / 1_000_000;

        assertThat(duracionMs)
                .as("enviarCorreoRecuperacion tiene que volver de inmediato, sin esperar a que mailSender.send() termine")
                .isLessThan(500);

        assertThat(elEnvioEmpezo.await(2, TimeUnit.SECONDS))
                .as("el envio real tiene que arrancar en otro hilo, aunque todavia no haya terminado")
                .isTrue();

        dejarTerminarElEnvio.countDown();
    }
}
