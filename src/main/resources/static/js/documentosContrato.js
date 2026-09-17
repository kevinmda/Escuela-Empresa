// Habilita "Generar PDF" (Contrato de pasantía, en /alumno/documentos/antes-de-empezar)
// recien cuando los campos obligatorios del formulario estan completos. El
// atributo "required" ya viene puesto en el HTML (Supervisor y Empresa; Área
// de Trabajo no lo tiene, por eso queda afuera de este chequeo sin necesitar
// una lista aparte).
(function () {
    document.addEventListener('DOMContentLoaded', function () {
        const form = document.getElementById('form-contrato');
        if (!form) return;

        const boton = document.getElementById('btn-generar-contrato');
        const obligatorios = form.querySelectorAll('[required]');

        function actualizarBoton() {
            let completos = true;
            obligatorios.forEach(function (campo) {
                if (!campo.value.trim()) completos = false;
            });
            boton.disabled = !completos;
        }

        obligatorios.forEach(function (campo) {
            campo.addEventListener('input', actualizarBoton);
        });

        actualizarBoton();
    });
})();
