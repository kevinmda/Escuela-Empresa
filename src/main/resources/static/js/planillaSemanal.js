// Guarda los indices de los dias que el alumno ya toco a mano (escribio o borro),
// para no pisarle ese campo con el autocompletado. Como este script nunca hace
// input.value = ... a traves de un evento del navegador, cualquier evento "input"
// que llegue es garantizado que vino de una accion real del alumno.
const camposTocados = new Set();

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

    camposTocados.clear(); // arranca una planilla nueva, se olvida de lo tocado antes
    actualizarLimitesFecha();
}

function limpiarFormulario() {
    const campos = document.querySelectorAll('#fieldsetPlanilla input, #fieldsetPlanilla textarea');
    campos.forEach(campo => campo.value = '');
    camposTocados.clear();
    actualizarLimitesFecha();
}

// En cuanto el alumno elige/cambia la fecha de un día, calculamos cuál es la única fecha
// posible para los demás días (según su posición Lunes->Sábado), se la ponemos como min/max
// (para que el calendario del navegador no deje elegir otra cosa), y AUTOCOMPLETAMOS con esa
// fecha los campos que el alumno todavia no toco a mano.
//
// "indiceAnclaForzado" es el indice del campo que el alumno acaba de editar (si vino de un
// evento de input) -- lo usamos como ancla en vez de "el primero que tenga algo cargado",
// para que si el alumno corrige una fecha que ya tenia (por ej. el Miercoles autocompletado
// antes) todo el resto se recalcule en base a ESE cambio, no al valor viejo del Lunes.
function actualizarLimitesFecha(indiceAnclaForzado) {
    const inputsFecha = document.querySelectorAll('.dia-bloque input[type="date"]');
    if (inputsFecha.length === 0) return;

    let indiceAncla = null;
    let fechaAncla = null;

    if (indiceAnclaForzado !== undefined && inputsFecha[indiceAnclaForzado].value) {
        indiceAncla = indiceAnclaForzado;
        fechaAncla = new Date(inputsFecha[indiceAnclaForzado].value + 'T00:00:00');
    } else {
        inputsFecha.forEach((input, indice) => {
            if (input.value && fechaAncla === null) {
                indiceAncla = indice;
                fechaAncla = new Date(input.value + 'T00:00:00');
            }
        });
    }

    if (fechaAncla === null) {
        // no queda ningun dia cargado: no restringimos nada
        inputsFecha.forEach(input => {
            input.removeAttribute('min');
            input.removeAttribute('max');
        });
        return;
    }

    inputsFecha.forEach((input, indice) => {
        if (indice === indiceAncla) return; // el ancla no se toca a si misma

        const diferenciaDias = indice - indiceAncla;
        const fechaEsperada = new Date(fechaAncla);
        fechaEsperada.setDate(fechaEsperada.getDate() + diferenciaDias);
        const fechaTexto = fechaEsperada.toISOString().split('T')[0];

        input.min = fechaTexto;
        input.max = fechaTexto;

        // autocompletamos (o realineamos) el campo, siempre y cuando el alumno
        // no lo haya tocado el a mano
        if (!camposTocados.has(indice)) {
            input.value = fechaTexto;
        }
    });
}

document.addEventListener('DOMContentLoaded', () => {
    actualizarLimitesFecha();

    const inputsFecha = document.querySelectorAll('.dia-bloque input[type="date"]');

    inputsFecha.forEach((input, indice) => {
        input.addEventListener('input', () => {

            if (input.value === '') {
                // Si el campo que se acaba de vaciar era el "ancla real" (o sea, no habia
                // ningun OTRO campo TOCADO A MANO en una posicion anterior a la suya -- los
                // que tienen valor solo porque se autocompletaron no cuentan), reiniciamos
                // todo: se borran los demas dias y nos olvidamos de cuales se habian tocado,
                // para que la proxima fecha que cargue el alumno dispare el autocompletado
                // de cero otra vez.
                let habiaOtroAnclaAntes = false;
                for (let i = 0; i < indice; i++) {
                    if (inputsFecha[i].value !== '' && camposTocados.has(i)) {
                        habiaOtroAnclaAntes = true;
                        break;
                    }
                }

                if (!habiaOtroAnclaAntes) {
                    inputsFecha.forEach(otro => otro.value = '');
                    camposTocados.clear();
                    actualizarLimitesFecha();
                    return;
                }
            }

            camposTocados.add(indice); // el alumno escribio o borro este campo a mano
            actualizarLimitesFecha(indice);
        });
    });
});
