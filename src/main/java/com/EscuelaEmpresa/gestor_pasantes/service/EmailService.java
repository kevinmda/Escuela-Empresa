package com.EscuelaEmpresa.gestor_pasantes.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

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
            "Ingresalo en la página para poder iniciar sesión. Este código vence en 15 minutos.\n\n" +
            "Si no intentaste iniciar sesión, podés ignorar este correo.\n"
        );

        mailSender.send(mensaje);
    }
}
