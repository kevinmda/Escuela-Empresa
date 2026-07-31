package com.EscuelaEmpresa.gestor_pasantes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    // esta URL base la sacamos de application.properties, asi en desarrollo apunta a localhost
    // y en el VPS (produccion) apunta al dominio real, sin tener que tocar el codigo Java
    @Value("${app.url.base}")
    private String urlBase;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarCorreoActivacion(String destinatario, String token) {
        String enlaceActivacion = urlBase + "/activar-cuenta?token=" + token;

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject("Activá tu cuenta - Gestor de Pasantes");
        mensaje.setText(
            "¡Hola!\n\n" +
            "Se creó una cuenta para vos en el Gestor de Pasantes.\n" +
            "Para activarla, hacé click en el siguiente enlace:\n\n" +
            enlaceActivacion + "\n\n" +
            "Si no esperabas este correo, podés ignorarlo.\n"
        );

        mailSender.send(mensaje);
    }
}
