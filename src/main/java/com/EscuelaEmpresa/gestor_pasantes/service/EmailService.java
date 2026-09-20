package com.EscuelaEmpresa.gestor_pasantes.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarCorreoCodigoActivacion(String destinatario, String codigo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject("Código de activación - Gestor de Pasantes");
        mensaje.setText(
            "¡Hola!\n\n" +
            "Tu código de activación es:\n\n" +
            codigo + "\n\n" +
            "Ingresalo en la página para poder activar la cuenta. Este código vence en 5 minutos.\n\n" +
            "Si no intentaste activarla, puedes ignorar este correo.\n"
        );

        mailSender.send(mensaje);
    }

    // @Async: OlvideContrasenaController tiene que responder a la misma velocidad
    // exista o no la cuenta. Si este envio (una conexion SMTP real) se hiciera en
    // el mismo hilo del pedido, el tiempo de respuesta ya delataria por si solo
    // que cuenta existe -- exactamente lo que el resto del controlador se cuida
    // de no revelar en el cuerpo de la respuesta. Al despacharlo aparte, el
    // redirect sale de inmediato sin esperar a que el correo salga.
    //
    // Efecto secundario aceptado: si el envio falla (SMTP caido, mal configurado),
    // ya no hay forma de avisarle al usuario en la misma respuesta ni de devolverle
    // el cupo -- se pierde en silencio y queda solo en este log. Es el precio de
    // que la falla no sea, en si misma, otra señal de que la cuenta existe.
    @Async
    public void enviarCorreoRecuperacion(String destinatario, String codigo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject("Recuperación de contraseña - Gestor de Pasantes");
        mensaje.setText(
            "¡Hola!\n\n" +
            "Recibimos una solicitud para restablecer tu contraseña.\n" +
            "Tu código de recuperación es:\n\n" +
            codigo + "\n\n" +
            "Ingresalo en la página para poder elegir una nueva contraseña. Este código vence en 5 minutos.\n\n" +
            "Si no pediste esto, podés ignorar este correo — tu contraseña actual sigue funcionando.\n"
        );

        try {
            mailSender.send(mensaje);
        } catch (MailException e) {
            log.warn("No se pudo enviar el correo de recuperación a {}", destinatario, e);
        }
    }
}
