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
