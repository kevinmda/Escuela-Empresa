// Habilita "Con mis datos" (Ficha final, en /alumno/documentos/al-terminar)
// recien cuando el campo Área de pasantía tiene texto. Ese campo arranca
// deshabilitado por el servidor (th:disabled) hasta que hay 6 planillas
// completas, y un campo deshabilitado siempre cuenta como "vacío" para este
// chequeo -- así que, sin ninguna lógica extra, el botón tampoco se habilita
// hasta que el campo lo esté.
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

    document.addEventListener('DOMContentLoaded', function () {
        activarBotonSegunObligatorios('form-ficha-final', 'btn-generar-ficha-final');
    });
})();
