package com.EscuelaEmpresa.gestor_pasantes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.EscuelaEmpresa.gestor_pasantes.entity.Usuario;
import com.EscuelaEmpresa.gestor_pasantes.repository.UsuarioRepository;
import com.EscuelaEmpresa.gestor_pasantes.service.EmailService;
import com.EscuelaEmpresa.gestor_pasantes.service.LimitadorEnvioCodigosService;
import com.EscuelaEmpresa.gestor_pasantes.service.LimitadorPeticionesPorIpService;

// Bloquea la garantia central de /olvide-contrasena: la respuesta (status,
// redirect, y si hay o no un mensaje flash) tiene que ser identica exista o no
// la cuenta. El envio de EmailService se mockea (no sale ningun correo real);
// el timing real de ese envio -- que ya no puede colgar la respuesta porque
// enviarCorreoRecuperacion es @Async -- se prueba aparte en
// EmailServiceAsyncTest, donde si hace falta un Spring real para que la
// anotacion tenga efecto.
class OlvideContrasenaControllerEnumeracionTest {

    // Mismo nombre que el atributo de sesion privado de OlvideContrasenaController
    // (ATRIBUTO_SESION_EMAIL). El controlador confia en la sesion, no en un
    // parametro del formulario, justamente para que este valor no se pueda
    // falsear desde afuera -- por eso el test lo carga directo en la sesion en
    // vez de pasar por el GET que lo dejaria ahi en un uso real.
    private static final String ATRIBUTO_SESION_EMAIL = "email_recuperacion";

    private UsuarioRepository usuarioRepository;
    private EmailService emailService;
    private MockMvc mockMvc;

    @BeforeEach
    void armar() {
        usuarioRepository = mock(UsuarioRepository.class);
        emailService = mock(EmailService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        OlvideContrasenaController controller = new OlvideContrasenaController(
                usuarioRepository,
                emailService,
                passwordEncoder,
                new LimitadorEnvioCodigosService(),
                new LimitadorPeticionesPorIpService());

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private MockHttpSession sesionCon(String email) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(ATRIBUTO_SESION_EMAIL, email);
        return session;
    }

    @Test
    void testOlvideContrasena_neverRevealsAccountExistence() throws Exception {
        String existente = "alumno.real@ejemplo.com";
        String noExiste = "nadie-se-registro-con-este@ejemplo.com";

        Usuario usuario = new Usuario();
        usuario.setEmail(existente);
        usuario.setActivo(true);

        when(usuarioRepository.findByEmail(existente)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByEmail(noExiste)).thenReturn(Optional.empty());

        MvcResult respuestaExistente = mockMvc.perform(post("/olvide-contrasena").session(sesionCon(existente)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/restablecer-contrasena"))
                .andExpect(flash().attributeCount(0))
                .andReturn();

        MvcResult respuestaNoExiste = mockMvc.perform(post("/olvide-contrasena").session(sesionCon(noExiste)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/restablecer-contrasena"))
                .andExpect(flash().attributeCount(0))
                .andReturn();

        // Mismo status HTTP y los mismos headers (Location incluido) para las dos:
        // nada en la respuesta permite distinguir cual de los dos emails existe.
        org.assertj.core.api.Assertions.assertThat(respuestaExistente.getResponse().getStatus())
                .isEqualTo(respuestaNoExiste.getResponse().getStatus());
        org.assertj.core.api.Assertions.assertThat(respuestaExistente.getResponse().getHeaderNames())
                .isEqualTo(respuestaNoExiste.getResponse().getHeaderNames());

        // La diferencia real (que solo se ve del lado del servidor, nunca en la
        // respuesta) es que a la cuenta real se le manda un codigo y a la que no
        // existe, no.
        verify(emailService).enviarCorreoRecuperacion(eq(existente), anyString());
        verify(emailService, never()).enviarCorreoRecuperacion(eq(noExiste), anyString());
    }

    @Test
    void testOlvideContrasena_elCooldownYaNoSeVeEnLaRespuesta() throws Exception {
        // Antes, pedir un codigo dos veces seguidas para la MISMA cuenta real
        // hacia aparecer un mensaje flash distinto en el segundo pedido ("Ya te
        // mandamos un código..."). Como ese mensaje solo podia aparecer para una
        // cuenta que existe, alcanzaba con mandar el mismo email dos veces y
        // mirar si aparecia para saber si esa cuenta estaba registrada. Ahora el
        // segundo pedido tiene que verse exactamente igual que el primero.
        String existente = "alumno.real@ejemplo.com";
        Usuario usuario = new Usuario();
        usuario.setEmail(existente);
        usuario.setActivo(true);
        when(usuarioRepository.findByEmail(existente)).thenReturn(Optional.of(usuario));

        MockHttpSession session = sesionCon(existente);

        mockMvc.perform(post("/olvide-contrasena").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/restablecer-contrasena"))
                .andExpect(flash().attributeCount(0));

        // Segundo pedido, sin esperar: cae dentro del cooldown de 60 segundos.
        mockMvc.perform(post("/olvide-contrasena").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/restablecer-contrasena"))
                .andExpect(flash().attributeCount(0));

        // Se genero y mando un solo codigo, no dos: el segundo pedido no reenvia.
        verify(emailService, org.mockito.Mockito.times(1))
                .enviarCorreoRecuperacion(eq(existente), anyString());
    }

    @Test
    void limiteDePeticionesCortaElBarridoMasivoSinDelatarNadaDistinto() throws Exception {
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());

        // Mismo origen (127.0.0.1, el que usa MockMvc por defecto) para las 20
        // peticiones que todavia tienen que pasar segun
        // LimitadorPeticionesPorIpService.
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/olvide-contrasena").session(sesionCon("quien-sea-" + i + "@ejemplo.com")))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/restablecer-contrasena"));
        }

        // La 21: mismo status, mismo redirect, ninguna pista de que se corto por
        // el limite en vez de por cualquier otro motivo.
        mockMvc.perform(post("/olvide-contrasena").session(sesionCon("otro-mas@ejemplo.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/restablecer-contrasena"))
                .andExpect(flash().attributeCount(0));
    }
}
