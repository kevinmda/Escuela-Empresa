function habilitarFormulario() {
    limpiarFormulario();
    document.getElementById('selectorPlanilla').value = '';
    document.getElementById('fieldsetPlanilla').disabled = false;
    document.getElementById('btnNuevo').disabled = true;

    const btnPdf = document.getElementById('btnGenerarPdf');
    if (btnPdf.tagName === 'A') {
        btnPdf.removeAttribute('href');
        btnPdf.classList.add('deshabilitado');
    } else {
        btnPdf.disabled = true;
    }

    const formEliminar = document.getElementById('formEliminar');
    if (formEliminar) {
        formEliminar.style.display = 'none';
    }

    actualizarLimitesFecha();
}

function limpiarFormulario() {
    const campos = document.querySelectorAll('#fieldsetPlanilla input, #fieldsetPlanilla textarea');
    campos.forEach(campo => campo.value = '');
    actualizarLimitesFecha();
}

// En cuanto el alumno elige la fecha de CUALQUIER día, calculamos cuál es la única fecha
// posible para los demás días (según su posición Lunes->Sábado) y se la ponemos como min/max
// al resto de los inputs -- asi el propio calendario del navegador no deja elegir una fecha
// que no corresponda a esa semana, aunque el alumno haya arrancado por un día distinto al Lunes.
function actualizarLimitesFecha() {
    const inputsFecha = document.querySelectorAll('.dia-bloque input[type="date"]');
    if (inputsFecha.length === 0) return;

    let indiceAncla = null;
    let fechaAncla = null;

    inputsFecha.forEach((input, indice) => {
        if (input.value && fechaAncla === null) {
            indiceAncla = indice;
            fechaAncla = new Date(input.value + 'T00:00:00');
        }
    });

    if (fechaAncla === null) {
        // todavia no cargo ningun dia: no restringimos nada
        inputsFecha.forEach(input => {
            input.removeAttribute('min');
            input.removeAttribute('max');
        });
        return;
    }

    inputsFecha.forEach((input, indice) => {
        if (indice === indiceAncla) return; // el que ya eligio, no lo tocamos

        const diferenciaDias = indice - indiceAncla;
        const fechaEsperada = new Date(fechaAncla);
        fechaEsperada.setDate(fechaEsperada.getDate() + diferenciaDias);

        const fechaTexto = fechaEsperada.toISOString().split('T')[0];
        input.min = fechaTexto;
        input.max = fechaTexto;
    });
}

document.addEventListener('DOMContentLoaded', () => {
    actualizarLimitesFecha();

    document.querySelectorAll('.dia-bloque input[type="date"]').forEach(input => {
        input.addEventListener('change', actualizarLimitesFecha);
    });
});