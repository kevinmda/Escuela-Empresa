package com.EscuelaEmpresa.gestor_pasantes.service;

import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// @Component (y no @Service) porque esta clase no representa una "logica de negocio" que otro
// controller vaya a llamar directamente - es una tarea que Spring ejecuta sola en background
@Component
public class ActivacionScheduler {

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    public ActivacionScheduler(UsuarioRepository usuarioRepository, EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
    }

    // fixedRate = 60000 significa "cada 60000 milisegundos" (1 minuto).
    // Cada 1 minuto, Spring llama sola a este metodo sin que nadie lo dispare manualmente.
    @Scheduled(fixedRate = 60000)
    public void procesarUsuariosPendientesDeActivacion() {

        // busca usuarios con activo=false que TODAVIA no tienen token (o sea, recien cargados,
        // a los que nunca se les mando el correo)
        List<Usuario> pendientes = usuarioRepository.findByActivoFalseAndTokenActivacionIsNull();

        for (Usuario usuario : pendientes) {
            String token = UUID.randomUUID().toString(); // token unico e imposible de adivinar

            usuario.setTokenActivacion(token);
            usuario.setTokenExpiracion(LocalDateTime.now().plusHours(24)); // vence en 24hs
            usuarioRepository.save(usuario);

            emailService.enviarCorreoActivacion(usuario.getEmail(), token);
        }
    }
}
