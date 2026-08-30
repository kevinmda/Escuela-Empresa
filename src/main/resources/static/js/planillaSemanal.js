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

            // ¿queda algun OTRO campo tocado a mano que todavia tenga una fecha cargada?
            // (los que tienen valor solo porque se autocompletaron no cuentan). Si la
            // respuesta es NO, este campo es el UNICO dato real que queda en la planilla
            // -- lo tratamos como el nuevo punto de partida: nos olvidamos de que los
            // demas habian sido tocados (aunque el alumno los haya borrado a mano antes),
            // para que se puedan volver a autocompletar en base a este campo.
            let quedaOtraAnclaTocada = false;
            for (let i = 0; i < inputsFecha.length; i++) {
                if (i === indice) continue;
                if (inputsFecha[i].value !== '' && camposTocados.has(i)) {
                    quedaOtraAnclaTocada = true;
                    break;
                }
            }

            if (!quedaOtraAnclaTocada) {
                camposTocados.clear();
                camposTocados.add(indice);

                if (input.value === '') {
                    // tambien se vacio este (ya no queda ningun dato cargado): borramos
                    // el resto por las dudas y no restringimos nada hasta que carguen algo
                    inputsFecha.forEach(otro => otro.value = '');
                    actualizarLimitesFecha();
                } else {
                    // este campo tiene una fecha nueva: recalculamos y autocompletamos
                    // el resto en base a el, de cero
                    actualizarLimitesFecha(indice);
                }
                return;
            }

            camposTocados.add(indice); // el alumno escribio o borro este campo a mano
            actualizarLimitesFecha(indice);
        });
    });
});
