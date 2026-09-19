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
    //
    // target="_blank" queda afuera: esos formularios (el del Contrato, en
    // /alumno/documentos/antes-de-empezar) abren su resultado en otra pestaña y
    // la pagina actual nunca navega, asi que nada vuelve a habilitar el botón
    // despues -- se quedaria diciendo "Enviando..." para siempre.
    function marcarEnvioEnCurso(form) {
        if (form.target === '_blank') return;

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

    // Reemplaza confirm() nativo del navegador. Cualquier <form> con
    // data-confirmar dispara el <dialog> de fragments/chrome en vez de
    // enviarse directo; data-confirmar-titulo/-mensaje/-texto arman el
    // contenido y data-confirmar-variante ("peligro", por defecto, o
    // "neutral") elige el color del ícono y del botón. Ej:
    //   <form data-confirmar data-confirmar-titulo="¿Eliminar?"
    //         data-confirmar-mensaje="No se puede deshacer."
    //         data-confirmar-texto="Eliminar">
    //
    // El listener va en document y en fase de CAPTURA (el tercer "true") y
    // no en cada form: tiene que interceptar el submit antes de que
    // prepararValidacion -más abajo en este archivo- llegue a correr y deje
    // el botón real en "Enviando...", porque en ese momento el envío
    // todavía no pasó por el diálogo.
    function inicializarConfirmaciones() {
        const dialogo = document.getElementById('dialogo-confirmacion');
        if (!dialogo || typeof dialogo.showModal !== 'function') return;

        const titulo = document.getElementById('dialogo-confirmacion-titulo');
        const mensaje = document.getElementById('dialogo-confirmacion-mensaje');
        const botonCancelar = document.getElementById('dialogo-confirmacion-cancelar');
        const botonConfirmar = document.getElementById('dialogo-confirmacion-confirmar');

        let formPendiente = null;
        // Guarda el form que se acaba de confirmar para distinguir, en el
        // próximo submit que llegue (el que dispara requestSubmit más
        // abajo), "esto ya pasó por el diálogo" de "esto es un pedido nuevo".
        let formConfirmado = null;

        document.addEventListener('submit', function (evento) {
            const form = evento.target;
            if (!(form instanceof HTMLFormElement) || !form.hasAttribute('data-confirmar')) return;

            if (formConfirmado === form) {
                formConfirmado = null;
                return; // ya se confirmó: lo deja seguir de verdad
            }

            evento.preventDefault();
            evento.stopPropagation();

            formPendiente = form;
            titulo.textContent = form.dataset.confirmarTitulo || '¿Confirmás esta acción?';
            mensaje.textContent = form.dataset.confirmarMensaje || '';
            const esNeutral = form.dataset.confirmarVariante === 'neutral';
            dialogo.dataset.variante = esNeutral ? 'neutral' : 'peligro';
            botonConfirmar.textContent = form.dataset.confirmarTexto || 'Confirmar';
            botonConfirmar.className = esNeutral ? 'btn-primary' : 'btn-danger';

            dialogo.showModal();
        }, true);

        botonConfirmar.addEventListener('click', function () {
            if (!formPendiente) return;
            formConfirmado = formPendiente;
            const form = formPendiente;
            formPendiente = null;
            dialogo.close();
            form.requestSubmit();
        });

        botonCancelar.addEventListener('click', function () {
            formPendiente = null;
            dialogo.close();
        });

        // Clic afuera de la tarjeta cae sobre el propio <dialog> (el
        // ::backdrop no es un elemento aparte), así que ahí adentro se
        // distingue del clic en el contenido.
        dialogo.addEventListener('click', function (evento) {
            if (evento.target === dialogo) {
                formPendiente = null;
                dialogo.close();
            }
        });

        // Esc dispara 'cancel' y cierra el <dialog> solo; acá solo hace
        // falta soltar el form pendiente para que no quede colgado.
        dialogo.addEventListener('cancel', function () {
            formPendiente = null;
        });
    }

    // Botón de ojo (mostrar/ocultar) para campos de contraseña. Se activa con
    // data-target apuntando al id del <input>, ej:
    // <div class="campo-password">
    //   <input type="password" id="password" ...>
    //   <button type="button" class="toggle-password" data-target="password">
    //     <svg class="ico-ojo">...</svg>
    //     <svg class="ico-ojo-tachado">...</svg>
    //   </button>
    // </div>
    // El estado visual (qué ojo se ve) lo decide el CSS a partir de la clase
    // "mostrando"; acá solo se togglea esa clase y el texto va por aria-label.
    function inicializarTogglesPassword() {
        document.querySelectorAll('.toggle-password').forEach(function (boton) {
            boton.addEventListener('click', function () {
                const campo = document.getElementById(boton.dataset.target);
                if (!campo) return;

                const seVaAMostrar = campo.type === 'password';
                campo.type = seVaAMostrar ? 'text' : 'password';
                boton.classList.toggle('mostrando', seVaAMostrar);
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

    // Mejora encima de <details>, no el mecanismo: el menu de la cuenta (y ahora
    // tambien el desplegable "Documentos" del riel, que reusa la misma clase)
    // abre y cierra sin JavaScript, que es lo que garantiza que cerrar sesion y
    // cambiar la contraseña sigan estando al alcance con el JS caido. Esto solo
    // agrega lo que el elemento nativo no trae: cerrar con Escape y al tocar
    // afuera. querySelectorAll y no querySelector: con dos .cuenta en la misma
    // pagina (cuenta + Documentos), quedarse con "el primero" dejaba al otro sin
    // esta mejora.
    // El CSS posiciona .cuenta-menu con position:absolute (ver styles.css),
    // que alcanza siempre que no haya un ancestro con overflow que lo recorte.
    // En pantallas angostas .barra-riel necesita overflow-x:auto para poder
    // desplazarse cuando hay muchas secciones -- y por como funciona overflow,
    // eso recorta también lo que se sale por abajo del riel, así que el menú
    // del desplegable "Documentos" quedaba invisible ahí. Se recalcula acá con
    // position:fixed (relativo a la pantalla, no al riel) desde donde está
    // el botón en ese momento, así escapa del recorte sin importar el ancho.
    // No se saca el CSS: sigue siendo el respaldo si este script no corre.
    function posicionarMenuCuenta(cuenta) {
        const resumen = cuenta.querySelector('summary');
        const menu = cuenta.querySelector('.cuenta-menu');
        if (!resumen || !menu) return;

        const rect = resumen.getBoundingClientRect();
        menu.style.position = 'fixed';
        menu.style.top = (rect.bottom + 8) + 'px';

        if (cuenta.closest('.barra-riel')) {
            // Desplegables del riel (ej. "Documentos"): se abren hacia la
            // derecha desde el propio botón, como ya definía el CSS con
            // left:0 -- no es lo último de la barra, así que anclar a la
            // derecha de la pantalla lo haría abrirse hacia la izquierda.
            menu.style.left = rect.left + 'px';
            menu.style.right = 'auto';
        } else {
            // El menú de la cuenta: anclado a su borde derecho, como ya
            // definía el CSS con right:0.
            menu.style.left = 'auto';
            menu.style.right = (window.innerWidth - rect.right) + 'px';
        }
    }

    function prepararMenuCuenta() {
        const cuentas = document.querySelectorAll('.cuenta');
        if (!cuentas.length) return;

        cuentas.forEach(function (cuenta) {
            cuenta.addEventListener('toggle', function () {
                if (cuenta.open) posicionarMenuCuenta(cuenta);
            });
        });

        window.addEventListener('resize', function () {
            cuentas.forEach(function (cuenta) {
                if (cuenta.open) posicionarMenuCuenta(cuenta);
            });
        });

        document.addEventListener('click', function (evento) {
            cuentas.forEach(function (cuenta) {
                if (cuenta.open && !cuenta.contains(evento.target)) {
                    cuenta.open = false;
                }
            });
        });

        document.addEventListener('keydown', function (evento) {
            if (evento.key !== 'Escape') return;
            cuentas.forEach(function (cuenta) {
                if (cuenta.open) {
                    cuenta.open = false;
                    const resumen = cuenta.querySelector('summary');
                    if (resumen) resumen.focus();
                }
            });
        });
    }

    // Tercera capa del crédito de autoría (las otras dos: el <footer> de
    // fragments/chrome.html y fragments/auth.html, y el respaldo en CSS de
    // styles.css que se activa con :has() cuando ese <footer> no está). Esta
    // corre en caliente: si alguien lo borra del DOM ya cargado -a mano desde
    // devtools, o un script que limpia el <body>- el MutationObserver lo nota
    // y lo vuelve a poner. Las tres capas son independientes a propósito: hay
    // que tocar plantilla, CSS y este archivo para que el crédito desaparezca
    // de verdad, no solo uno de los tres.
    function crearCreditoDisenio() {
        const footer = document.createElement('footer');
        footer.className = 'credito-disenio';
        footer.setAttribute('aria-label', 'Créditos de diseño y desarrollo');
        footer.innerHTML =
            '<span>Desarrollado por:</span> ' +
            '<a href="#" target="_blank" rel="noopener noreferrer">[Nombre 1]</a> ' +
            '<span>y por:</span> ' +
            '<a href="#" target="_blank" rel="noopener noreferrer">[Nombre 2]</a>';
        return footer;
    }

    // En movil .barra-riel se desplaza en horizontal (ver el media query en
    // styles.css) y sin nada que lo indique, el riel de cuatro o cinco
    // secciones (coordinación, administración) parece una lista corta y
    // completa: no hay ninguna pista de que sigue hacia la derecha. Estas
    // clases prenden un degradado en cada borde solo cuando de verdad hay
    // contenido tapado de ese lado, y se apagan solas al llegar al final.
    function inicializarRielDesbordado() {
        document.querySelectorAll('.barra-riel').forEach(function (riel) {
            function actualizar() {
                const desborda = riel.scrollWidth > riel.clientWidth + 1;
                riel.classList.toggle('riel-desborda-izq', desborda && riel.scrollLeft > 1);
                riel.classList.toggle('riel-desborda-der',
                    desborda && riel.scrollLeft < riel.scrollWidth - riel.clientWidth - 1);
            }
            riel.addEventListener('scroll', actualizar, { passive: true });
            window.addEventListener('resize', actualizar);
            actualizar();
        });
    }

    function asegurarCreditoDisenio() {
        if (!document.body || document.querySelector('.credito-disenio')) return;
        document.body.appendChild(crearCreditoDisenio());
    }

    function vigilarCreditoDisenio() {
        asegurarCreditoDisenio();
        new MutationObserver(asegurarCreditoDisenio)
            .observe(document.body, { childList: true, subtree: true });
    }

    window.addEventListener('pageshow', restaurarBotones);

    document.addEventListener('DOMContentLoaded', function () {
        actualizarIndicadorCarga();
        document.querySelectorAll('form').forEach(prepararValidacion);
        inicializarTogglesPassword();
        inicializarRielDesbordado();
        inicializarConfirmaciones();
        prepararMenuCuenta();
        prepararGiroMarca();
        vigilarCreditoDisenio();
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
