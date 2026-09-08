(function () {
    const root = document.documentElement;
    let solicitudesActivas = 0;
    const savedTheme = localStorage.getItem('theme');
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const initialTheme = savedTheme || (prefersDark ? 'dark' : 'light');

    root.dataset.theme = initialTheme;

    function actualizarIndicadorCarga() {
        let indicador = document.getElementById('indicador-carga');
        if (!indicador) {
            indicador = document.createElement('div');
            indicador.id = 'indicador-carga';
            indicador.setAttribute('role', 'progressbar');
            indicador.setAttribute('aria-label', 'Cargando');
            document.body.appendChild(indicador);
        }
        indicador.classList.toggle('activo', solicitudesActivas > 0);
        actualizarGiroMarca();
    }

    const prefiereMenosMovimiento = window.matchMedia('(prefers-reduced-motion: reduce)');

    // El logo de la barra es un engranaje, asi que gira con el mismo estado que
    // enciende la barra de carga: no es adorno, dice que la maquina esta
    // trabajando. Enciende al instante; apagar es otra historia, ver mas abajo.
    function actualizarGiroMarca() {
        const logo = document.querySelector('.barra-marca-logo');
        if (!logo) return;

        if (solicitudesActivas > 0) {
            logo.classList.add('trabajando');
        } else if (prefiereMenosMovimiento.matches) {
            // Sin animacion no hay vuelta que esperar: se saca en el momento.
            logo.classList.remove('trabajando');
        }
    }

    // Frena al completar la vuelta, no al terminar la peticion. Si parara donde
    // esta, el rayo del medio quedaria torcido hasta la proxima vez.
    function prepararGiroMarca() {
        const logo = document.querySelector('.barra-marca-logo');
        if (!logo) return;

        logo.addEventListener('animationiteration', function () {
            if (solicitudesActivas === 0) {
                logo.classList.remove('trabajando');
            }
        });
    }

    const fetchOriginal = window.fetch;
    window.fetch = function () {
        solicitudesActivas++;
        actualizarIndicadorCarga();
        return fetchOriginal.apply(this, arguments).finally(function () {
            solicitudesActivas = Math.max(0, solicitudesActivas - 1);
            actualizarIndicadorCarga();
        });
    };

    // El mensaje de error se inserta DESPUES de .campo-password, no despues del
    // <input>: ese wrapper es position:relative y tiene el boton Mostrar centrado
    // en absolute, asi que meterle el span adentro lo descentraria.
    function anclaDe(campo) {
        return campo.closest('.campo-password') || campo;
    }

    // Mensajes propios en vez de campo.validationMessage, que viene en el idioma
    // del navegador y mezclaria ingles con el resto de la interfaz en español.
    function mensajeDeError(campo) {
        const validez = campo.validity;

        if (validez.valueMissing) return 'Este campo es obligatorio.';
        if (validez.typeMismatch && campo.type === 'email') return 'Ingresá un email válido.';
        if (validez.tooShort) return 'Tiene que tener al menos ' + campo.minLength + ' caracteres.';
        if (validez.customError) return campo.validationMessage;

        return 'Revisá este campo.';
    }

    function limpiarError(campo) {
        const ancla = anclaDe(campo);
        const bloque = ancla.parentElement;
        if (bloque) {
            bloque.querySelectorAll('.campo-error').forEach(function (viejo) {
                viejo.remove();
            });
        }
        campo.removeAttribute('aria-invalid');
        campo.removeAttribute('aria-describedby');
        campo.setCustomValidity('');
    }

    function marcarError(campo, mensaje) {
        const ancla = anclaDe(campo);
        const error = document.createElement('span');
        const id = (campo.id || campo.name || 'campo') + '-error';

        error.className = 'campo-error';
        error.id = id;
        error.textContent = mensaje;

        campo.setAttribute('aria-invalid', 'true');
        campo.setAttribute('aria-describedby', id);
        ancla.insertAdjacentElement('afterend', error);
    }

    // Campos de confirmación: <input data-confirma="idDelOtroCampo">. Sirve tanto
    // para restablecer-contrasena como para cambiar-contrasena.
    // OJO: esto es solo conveniencia en el navegador. Un POST directo se lo saltea,
    // así que la comparación en el servidor sigue siendo necesaria.
    function validarConfirmacion(form) {
        let coinciden = true;

        form.querySelectorAll('[data-confirma]').forEach(function (confirmacion) {
            const original = form.querySelector('#' + confirmacion.dataset.confirma);
            if (!original) return;

            if (original.value !== confirmacion.value) {
                confirmacion.setCustomValidity('Las contraseñas no coinciden.');
                coinciden = false;
            } else {
                confirmacion.setCustomValidity('');
            }
        });

        return coinciden;
    }

    function prepararValidacion(form) {
        form.addEventListener('submit', function (event) {
            let primerInvalido = null;

            // El orden importa: limpiar primero (limpiarError borra el customValidity),
            // después marcar la confirmación, y recién entonces revisar validez.
            form.querySelectorAll('[required]').forEach(function (campo) {
                limpiarError(campo);
            });

            validarConfirmacion(form);

            form.querySelectorAll('[required]').forEach(function (campo) {
                if (!campo.checkValidity()) {
                    marcarError(campo, mensajeDeError(campo));
                    primerInvalido = primerInvalido || campo;
                }
            });

            if (primerInvalido) {
                event.preventDefault();
                primerInvalido.focus();
                return;
            }

            marcarEnvioEnCurso(form);
        });
    }

    // Bloquea el botón mientras el POST viaja. Sin esto, dos clicks en
    // /olvide-contrasena generan dos códigos y el primero queda invalidado.
    // El disabled va en el tick siguiente: hacerlo dentro del handler de submit
    // puede cancelar el envío en algunos navegadores.
    function marcarEnvioEnCurso(form) {
        const boton = form.querySelector('button[type="submit"]');
        if (!boton || boton.disabled) return;

        setTimeout(function () {
            boton.dataset.textoOriginal = boton.textContent;
            boton.textContent = 'Enviando...';
            boton.disabled = true;
        }, 0);
    }

    // Al volver con el botón "atrás", el navegador puede restaurar la página desde
    // caché con el botón todavía deshabilitado. Esto lo devuelve a su estado.
    function restaurarBotones() {
        document.querySelectorAll('button[type="submit"][disabled]').forEach(function (boton) {
            if (!boton.dataset.textoOriginal) return;
            boton.textContent = boton.dataset.textoOriginal;
            boton.disabled = false;
        });
    }

    // Botón "Mostrar/Ocultar" para campos de contraseña. Se activa con
    // data-target apuntando al id del <input>, ej:
    // <div class="campo-password">
    //   <input type="password" id="password" ...>
    //   <button type="button" class="toggle-password" data-target="password">Mostrar</button>
    // </div>
    function inicializarTogglesPassword() {
        document.querySelectorAll('.toggle-password').forEach(function (boton) {
            boton.addEventListener('click', function () {
                const campo = document.getElementById(boton.dataset.target);
                if (!campo) return;

                const seVaAMostrar = campo.type === 'password';
                campo.type = seVaAMostrar ? 'text' : 'password';
                boton.textContent = seVaAMostrar ? 'Ocultar' : 'Mostrar';
                boton.setAttribute('aria-label', seVaAMostrar ? 'Ocultar contraseña' : 'Mostrar contraseña');
                // aria-pressed comunica el estado del botón a los lectores de pantalla,
                // que con solo el aria-label no sabrían si está activo o no.
                boton.setAttribute('aria-pressed', seVaAMostrar ? 'true' : 'false');
            });
        });
    }

    function actualizarBoton() {
        const button = document.getElementById('theme-toggle');
        if (!button) return;

        const darkMode = root.dataset.theme === 'dark';
        const etiqueta = darkMode ? 'Activar modo claro' : 'Activar modo oscuro';

        // El toggle de la barra es un icono, y el par sol/luna lo elige el CSS a
        // partir de data-theme. Escribirle textContent le borraria los dos <svg>
        // de adentro, asi que ahi la etiqueta va solo por aria-label. El de las
        // pantallas de acceso (.theme-toggle-standalone) sigue siendo texto.
        if (!button.classList.contains('theme-toggle-icono')) {
            button.textContent = darkMode ? 'Modo claro' : 'Modo oscuro';
        }

        button.setAttribute('aria-label', etiqueta);
        button.setAttribute('title', etiqueta);
    }

    // Mejora encima de <details>, no el mecanismo: el menu de la cuenta abre y
    // cierra sin JavaScript, que es lo que garantiza que cerrar sesion y cambiar
    // la contraseña sigan estando al alcance con el JS caido. Esto solo agrega lo
    // que el elemento nativo no trae: cerrar con Escape y al tocar afuera.
    function prepararMenuCuenta() {
        const cuenta = document.querySelector('.cuenta');
        if (!cuenta) return;

        document.addEventListener('click', function (evento) {
            if (cuenta.open && !cuenta.contains(evento.target)) {
                cuenta.open = false;
            }
        });

        document.addEventListener('keydown', function (evento) {
            if (evento.key === 'Escape' && cuenta.open) {
                cuenta.open = false;
                const resumen = cuenta.querySelector('summary');
                if (resumen) resumen.focus();
            }
        });
    }

    window.addEventListener('pageshow', restaurarBotones);

    document.addEventListener('DOMContentLoaded', function () {
        actualizarIndicadorCarga();
        document.querySelectorAll('form').forEach(prepararValidacion);
        inicializarTogglesPassword();
        prepararMenuCuenta();
        prepararGiroMarca();
        actualizarBoton();
        const button = document.getElementById('theme-toggle');
        if (!button) return;

        let finTransicionTema;
        button.addEventListener('click', function () {
            const nextTheme = root.dataset.theme === 'dark' ? 'light' : 'dark';

            // Marca una ventana breve (250ms) durante la cual el CSS deja que los
            // colores de toda la superficie viren con transición, para que el
            // cambio de tema no sea un repintado a saltos (antes solo el <body>
            // animaba y todo lo demás -bordes, paneles, header- pegaba el salto).
            // Fuera de esta ventana ningún otro cambio de color anima.
            root.dataset.themeChanging = '';
            clearTimeout(finTransicionTema);
            finTransicionTema = setTimeout(function () {
                delete root.dataset.themeChanging;
            }, 250);

            root.dataset.theme = nextTheme;
            localStorage.setItem('theme', nextTheme);
            actualizarBoton();
        });
    });
})();
