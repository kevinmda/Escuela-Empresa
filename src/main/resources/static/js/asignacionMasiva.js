// Asignación de supervisor/empresa a los alumnos con UN solo botón de guardar.
//
// Antes cada fila era su propio formulario con su propio "Guardar": cambiar
// treinta alumnos eran treinta envíos y treinta recargas de la página. Ahora la
// tabla entera es un solo formulario. Este script solo lleva la cuenta de qué
// filas cambiaron (compara cada select con el valor que traía de la base, en
// data-original), marca esas filas, habilita el botón cuando hay algo para
// guardar y avisa si se intenta salir de la página con cambios sin guardar.
//
// Al enviar, solo viajan los selects que cambiaron: el servidor igualmente
// compara contra la base, así que mandar de más no rompería nada, pero así el
// envío es chico aunque la sección tenga muchos alumnos. Las filas ocultas por
// el selector de sección también cuentan: se puede cambiar A y B y guardar una vez.
(function () {
    const formulario = document.querySelector('form[data-asignacion-masiva]');
    if (!formulario) return;

    const selects = Array.from(formulario.querySelectorAll('select.select-asignacion'));
    const boton = formulario.querySelector('[data-asignacion-guardar]');
    const contador = formulario.querySelector('[data-asignacion-contador]');
    if (!boton || !contador) return;

    let enviando = false;

    function cambio(select) {
        return select.value !== (select.dataset.original || '');
    }

    function actualizar() {
        let cambios = 0;
        selects.forEach(select => {
            const modificado = cambio(select);
            if (modificado) cambios++;
            const fila = select.closest('tr');
            if (fila) fila.classList.toggle('fila-modificada', modificado);
        });

        boton.disabled = cambios === 0;
        contador.classList.toggle('asignacion-contador--pendiente', cambios > 0);
        contador.textContent = cambios === 0
            ? 'Sin cambios'
            : (cambios === 1 ? '1 cambio sin guardar' : cambios + ' cambios sin guardar');
    }

    formulario.addEventListener('change', evento => {
        if (evento.target.matches('select.select-asignacion')) actualizar();
    });

    formulario.addEventListener('submit', () => {
        enviando = true;
        boton.disabled = true;
        // Los selects deshabilitados no viajan en el POST.
        selects.forEach(select => { select.disabled = !cambio(select); });
    });

    window.addEventListener('beforeunload', evento => {
        if (enviando) return;
        if (selects.some(cambio)) {
            evento.preventDefault();
            evento.returnValue = '';
        }
    });

    // Al volver con "Atrás" el navegador puede restaurar los selects con lo que
    // se había elegido; actualizar() deja el contador de acuerdo con eso.
    window.addEventListener('pageshow', () => {
        enviando = false;
        selects.forEach(select => { select.disabled = false; });
        actualizar();
    });

    actualizar();
})();
