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

    // El Padre/Encargado (Contrato: solo nombre; Autorización: nombre + cédula)
    // tiene que coincidir con lo que ya está cargado en la base, y eso solo se
    // puede saber preguntándole al servidor. Sin este chequeo previo, el
    // target="_blank" del formulario ya abrió la pestaña nueva para cuando la
    // respuesta dice que había que rechazarlo -- por eso esto corre ANTES de
    // dejar que el formulario se someta de verdad.
    //
    // "enviarPorFetch": true (solo Autorización) manda el formulario por fetch
    // en vez del envío nativo del <form> -- ver enviarFormularioPorFetch más
    // abajo para el motivo. Contrato (GET, sin archivos) sigue con
    // form.requestSubmit(), que ahí funciona bien.
    function activarVerificacionPadre(formId, nombreSelector, ciSelector, enviarPorFetch) {
        const form = document.getElementById(formId);
        if (!form) return;

        let verificado = false;

        form.addEventListener('submit', function (evento) {
            if (verificado) {
                verificado = false;
                return;
            }

            evento.preventDefault();

            const boton = form.querySelector('button[type="submit"]');
            const campoNombre = form.querySelector(nombreSelector);
            const campoCi = ciSelector ? form.querySelector(ciSelector) : null;
            if (!campoNombre) return;

            const params = new URLSearchParams({ nombre: campoNombre.value.trim() });
            if (campoCi) params.set('ci', campoCi.value.trim());

            if (boton) boton.disabled = true;

            fetch('/alumno/documentos/verificar-padre?' + params.toString())
                .then(function (respuesta) { return respuesta.json(); })
                .then(function (resultado) {
                    if (!resultado.valido) {
                        mostrarError(resultado.error
                            || 'El Padre/Encargado no coincide con lo que tenemos registrado.');
                        if (boton) boton.disabled = false;
                        return;
                    }

                    if (enviarPorFetch) {
                        enviarFormularioPorFetch(form, boton);
                        return;
                    }

                    verificado = true;
                    if (typeof form.requestSubmit === 'function') {
                        form.requestSubmit();
                    } else {
                        verificado = false; // form.submit() no dispara este listener de nuevo
                        form.submit();
                    }
                    // Se reactiva también en el caso exitoso: el PDF se abre en OTRA
                    // pestaña y esta página se queda tal cual, así que sin esto el
                    // botón quedaba deshabilitado para siempre después del primer
                    // click (antes solo se reactivaba si tocabas de nuevo un campo).
                    if (boton) boton.disabled = false;
                })
                .catch(function () {
                    mostrarError('No se pudo verificar los datos del Padre/Encargado. Probá de nuevo.');
                    if (boton) boton.disabled = false;
                });
        });
    }

    // Autorización es el único formulario de esta página que junta POST +
    // multipart/form-data (por los archivos adjuntos) + target="_blank" -- y
    // esa combinación es la que algunos navegadores reportan como "problema
    // de red" al abrir la pestaña nueva (el resto de los documentos son GET
    // simples, sin archivos, y no tienen este problema). En vez de dejar que
    // el <form> se someta nativamente, se arma con FormData (que junta solo,
    // sin recorrerlo a mano, todos los campos Y los archivos elegidos) y se
    // manda por fetch; el PDF que devuelve se descarga con el nombre correcto.
    //
    // Nota: esto descarga el archivo directo en vez de abrirlo primero en una
    // pestaña como el resto de los documentos. window.open(url) con una URL de
    // blob no respeta el nombre del archivo -- el navegador termina mostrando
    // el identificador interno del blob (algo como
    // "a0e4b4ed-755a-4588-a44d-17e15940d67f") en vez de
    // "Autorizacion_...". El truco de <a download="..."> sí funciona en todos
    // los navegadores para el nombre, pero fuerza la descarga en vez de abrir
    // un visor.
    function enviarFormularioPorFetch(form, boton) {
        const datos = new FormData(form);

        fetch(form.action, { method: 'POST', body: datos, credentials: 'same-origin' })
            .then(function (respuesta) {
                const tipo = respuesta.headers.get('content-type') || '';
                if (!respuesta.ok || tipo.indexOf('application/pdf') === -1) {
                    throw new Error('respuesta-no-pdf');
                }

                // El nombre con el que se guarda sale de Content-Disposition, el
                // mismo que ya manda el servidor. Con nombres que llevan tildes
                // (casi todos) Spring lo manda codificado como
                // filename*=UTF-8''...: hay que preferir ese y decodificarlo:
                // si solo se mirara filename="...", con un nombre así ni
                // siquiera matchea (el "*" antes del "=" lo saca de la pinta
                // que espera esa expresión regular).
                const disposicion = respuesta.headers.get('content-disposition') || '';
                let nombreArchivo = 'Autorizacion.pdf';
                const conTilde = disposicion.match(/filename\*=UTF-8''([^;]+)/i);
                if (conTilde) {
                    nombreArchivo = decodeURIComponent(conTilde[1]);
                } else {
                    const simple = disposicion.match(/filename="?([^";]+)"?/i);
                    if (simple) nombreArchivo = simple[1];
                }

                return respuesta.blob().then(function (blob) {
                    return { blob: blob, nombreArchivo: nombreArchivo };
                });
            })
            .then(function (resultado) {
                const url = URL.createObjectURL(resultado.blob);
                const enlace = document.createElement('a');
                enlace.href = url;
                enlace.download = resultado.nombreArchivo;
                document.body.appendChild(enlace);
                enlace.click();
                document.body.removeChild(enlace);
                setTimeout(function () { URL.revokeObjectURL(url); }, 10000);
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

        activarVerificacionPadre('form-contrato', '[name="padreEncargado"]', null, false);
        activarVerificacionPadre('form-autorizacion', '[name="padreNombre"]', '[name="padreCi"]', true);

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
