// /alumno/documentos/antes-de-empezar tiene dos tarjetas con formulario
// (Contrato y Autorización), cada una con su propio botón "Generar PDF" que
// arranca deshabilitado y se habilita recién cuando sus campos [required]
// tienen texto.
(function () {
    function activarBotonSegunObligatorios(formId, botonId) {
        const form = document.getElementById(formId);
        const boton = document.getElementById(botonId);
        if (!form || !boton) return;

        const obligatorios = form.querySelectorAll('[required]');

        function actualizar() {
            let completos = true;
            obligatorios.forEach(function (campo) {
                if (!campo.value.trim()) completos = false;
            });
            boton.disabled = !completos;
        }

        obligatorios.forEach(function (campo) {
            campo.addEventListener('input', actualizar);
        });

        actualizar();
    }

    // Muestra el error en el mismo lugar donde aparecería si el servidor
    // redirigiera con un mensaje (mismo banner, misma clase), pero sin
    // navegar a ningún lado: #documentos-error siempre existe en la página
    // (con o sin error inicial), así que esto funciona aunque no haya
    // arrancado con ninguno.
    function mostrarError(mensaje) {
        const contenedor = document.getElementById('documentos-error');
        if (!contenedor) return;

        contenedor.innerHTML = '';
        const banner = document.createElement('div');
        banner.className = 'mensaje-error';
        const parrafo = document.createElement('p');
        parrafo.textContent = mensaje;
        banner.appendChild(parrafo);
        contenedor.appendChild(banner);
        banner.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    // El Padre/Encargado (nombre en Contrato; nombre + cédula en Autorización) se
    // escribe en el PDF tal cual lo ingresó el alumno: ya no se compara con lo
    // que hay en la base. Contrato es un GET simple y se envía de forma nativa;
    // Autorización se manda por fetch (ver enviarFormularioPorFetch más abajo).
    function activarEnvioPorFetch(formId) {
        const form = document.getElementById(formId);
        if (!form) return;

        form.addEventListener('submit', function (evento) {
            evento.preventDefault();
            const boton = form.querySelector('button[type="submit"]');
            if (boton) boton.disabled = true;
            enviarFormularioPorFetch(form, boton);
        });
    }

    // Autorización es el único formulario de esta página que junta POST +
    // multipart/form-data (por los archivos adjuntos) + target="_blank" -- y
    // esa combinación es la que algunos navegadores reportan como "problema
    // de red" al abrir la pestaña nueva (el resto de los documentos son GET
    // simples, sin archivos, y no tienen este problema). En vez de dejar que
    // el <form> se someta nativamente, se arma con FormData y se manda por
    // fetch: el servidor genera el PDF, lo deja guardado del lado de la
    // sesión, y devuelve solo un link corto. Ese link se abre con
    // window.open() como una navegación GET normal -- igual que el resto de
    // los documentos -- así que el navegador lo trata igual: pestaña nueva,
    // nombre correcto, sin ningún truco de blob de por medio (una URL de
    // blob abierta con window.open() no respeta el nombre del archivo).
    function enviarFormularioPorFetch(form, boton) {
        const datos = new FormData(form);

        fetch(form.action, { method: 'POST', body: datos, credentials: 'same-origin' })
            .then(function (respuesta) { return respuesta.json(); })
            .then(function (resultado) {
                if (resultado.valido) {
                    window.open(resultado.url, '_blank');
                } else {
                    mostrarError(resultado.error || 'No se pudo generar el PDF.');
                }
                if (boton) boton.disabled = false;
            })
            .catch(function () {
                mostrarError('No se pudo generar el PDF. Revisá los datos e intentá de nuevo.');
                if (boton) boton.disabled = false;
            });
    }

    // Misma zona de arrastre que ya usa Subir documentos (subir.html), copiada
    // acá en vez de compartida porque esa vive en un <script> inline de esa
    // página y no en un archivo aparte. Único agregado: el título también
    // contempla varios archivos elegidos a la vez.
    function activarDropzone(dropzone, inputArchivo, dropzoneTitulo, tituloVacio) {
        if (!dropzone || !inputArchivo || !dropzoneTitulo) return;

        function actualizarNombreArchivo() {
            const cantidad = inputArchivo.files ? inputArchivo.files.length : 0;
            if (cantidad === 1) {
                dropzoneTitulo.textContent = inputArchivo.files[0].name;
                dropzone.classList.add('dropzone-con-archivo');
            } else if (cantidad > 1) {
                dropzoneTitulo.textContent = cantidad + ' archivos seleccionados';
                dropzone.classList.add('dropzone-con-archivo');
            } else {
                dropzoneTitulo.textContent = tituloVacio;
                dropzone.classList.remove('dropzone-con-archivo');
            }
        }

        inputArchivo.addEventListener('change', actualizarNombreArchivo);

        ['dragover', 'dragleave', 'drop'].forEach(function (evento) {
            dropzone.addEventListener(evento, function (e) {
                e.preventDefault();
                e.stopPropagation();
            });
        });

        dropzone.addEventListener('dragover', function () {
            dropzone.classList.add('dropzone-activo');
        });
        dropzone.addEventListener('dragleave', function () {
            dropzone.classList.remove('dropzone-activo');
        });
        dropzone.addEventListener('drop', function (e) {
            dropzone.classList.remove('dropzone-activo');
            if (e.dataTransfer.files.length > 0) {
                inputArchivo.files = e.dataTransfer.files;
                actualizarNombreArchivo();
            }
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        activarBotonSegunObligatorios('form-contrato', 'btn-generar-contrato');
        activarBotonSegunObligatorios('form-autorizacion', 'btn-generar-autorizacion');

        activarEnvioPorFetch('form-autorizacion');

        const tituloVacio = 'Hacé click o arrastrá el PDF acá';

        activarDropzone(
            document.getElementById('dropzone-cedula-alumno'),
            document.getElementById('cedula-alumno-input'),
            document.getElementById('dropzone-titulo-cedula-alumno'),
            tituloVacio);

        activarDropzone(
            document.getElementById('dropzone-cedula-padre'),
            document.getElementById('cedula-padre-input'),
            document.getElementById('dropzone-titulo-cedula-padre'),
            tituloVacio);
    });
})();
