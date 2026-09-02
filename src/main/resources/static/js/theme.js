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

    function prepararValidacion(form) {
        form.addEventListener('submit', function (event) {
            let primerInvalido = null;
            form.querySelectorAll('[required]').forEach(function (campo) {
                const anterior = campo.parentElement.querySelector('.campo-error');
                if (anterior) anterior.remove();
                campo.removeAttribute('aria-invalid');

                if (!campo.checkValidity()) {
                    campo.setAttribute('aria-invalid', 'true');
                    const error = document.createElement('span');
                    error.className = 'campo-error';
                    error.textContent = 'Este campo es obligatorio.';
                    campo.insertAdjacentElement('afterend', error);
                    primerInvalido = primerInvalido || campo;
                }
            });

            if (primerInvalido) {
                event.preventDefault();
                primerInvalido.focus();
            }
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
            });
        });
    }

    function actualizarBoton() {
        const button = document.getElementById('theme-toggle');
        if (!button) return;

        const darkMode = root.dataset.theme === 'dark';
        button.textContent = darkMode ? 'Modo claro' : 'Modo oscuro';
        button.setAttribute('aria-label', darkMode ? 'Activar modo claro' : 'Activar modo oscuro');
        button.setAttribute('title', darkMode ? 'Activar modo claro' : 'Activar modo oscuro');
    }

    document.addEventListener('DOMContentLoaded', function () {
        actualizarIndicadorCarga();
        document.querySelectorAll('form').forEach(prepararValidacion);
        inicializarTogglesPassword();
        actualizarBoton();
        const button = document.getElementById('theme-toggle');
        if (!button) return;

        button.addEventListener('click', function () {
            const nextTheme = root.dataset.theme === 'dark' ? 'light' : 'dark';
            root.dataset.theme = nextTheme;
            localStorage.setItem('theme', nextTheme);
            actualizarBoton();
        });
    });
})();
