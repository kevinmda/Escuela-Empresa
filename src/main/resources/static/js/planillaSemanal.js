// La primera fecha que el alumno carga (en cualquiera de los seis días) es el
// "ancla": a partir de ella se calculan las otras cinco (ancla ±1, ±2... según
// la posición de cada día) y esas cinco quedan bloqueadas -- ya no hace falta
// escribirlas a mano, ni se pueden desalinear de la primera. Si el alumno
// borra la fecha ancla, se borran las otras cinco y los seis campos vuelven a
// quedar libres para elegir una nueva ancla (no tiene que ser la misma fila
// de antes).
let indiceAnclaFecha = null;

function habilitarFormulario() {
    limpiarFormulario();
    // El selector de semanas dejo de ser un <select> y paso a ser el riel de
    // casilleros, que son enlaces: no hay ningun valor que resetear aca.
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
}

// Suma las horas de los seis dias mientras el alumno escribe. Es el numero que
// despues aparece en el PDF y en el informe, y hasta ahora habia que sacarlo
// de cabeza sumando los seis campos.
function actualizarTotalHoras() {
    const salida = document.getElementById('totalHoras');
    if (!salida) return;

    let total = 0;
    document.querySelectorAll('.dia-bloque input[type="number"]').forEach(campo => {
        const valor = parseFloat(campo.value);
        if (!isNaN(valor) && valor > 0) {
            total += valor;
        }
    });

    // Sin decimales cuando el total da redondo, que es el caso normal.
    salida.textContent = Number.isInteger(total) ? String(total) : total.toFixed(2);
}

function limpiarFormulario() {
    const campos = document.querySelectorAll('#fieldsetPlanilla input, #fieldsetPlanilla textarea');
    campos.forEach(campo => campo.value = '');
    document.querySelectorAll('.aviso-limite').forEach(aviso => aviso.style.display = 'none');
    liberarFechas();
    actualizarTotalHoras();
}

// Vuelve los seis campos de fecha a su estado inicial: vacios, editables, sin
// ancla. Es lo que pasa al arrancar una planilla nueva y lo que pasa cuando el
// alumno borra la fecha ancla.
function liberarFechas() {
    indiceAnclaFecha = null;
    document.querySelectorAll('.dia-bloque input[type="date"]').forEach(input => {
        input.value = '';
        input.readOnly = false;
    });
}

// Recalcula las otras cinco fechas a partir de la fecha ancla y las deja
// bloqueadas. La fecha ancla en si no se toca (sigue editable).
function recalcularDesdeAncla() {
    const inputsFecha = document.querySelectorAll('.dia-bloque input[type="date"]');
    const inputAncla = inputsFecha[indiceAnclaFecha];
    if (!inputAncla || !inputAncla.value) return;

    const fechaAncla = new Date(inputAncla.value + 'T00:00:00');

    inputsFecha.forEach((input, indice) => {
        if (indice === indiceAnclaFecha) return;

        const diferenciaDias = indice - indiceAnclaFecha;
        const fechaEsperada = new Date(fechaAncla);
        fechaEsperada.setDate(fechaEsperada.getDate() + diferenciaDias);

        input.value = fechaEsperada.toISOString().split('T')[0];
        input.readOnly = true;
    });
}

// Si la planilla ya viene con fechas cargadas (al editar una que se guardo con
// el esquema viejo, con varias fechas tocadas a mano), la del primer campo con
// valor pasa a ser la ancla y el resto se recalcula y se bloquea a partir de
// ella, aunque no coincidan con lo que ya estaba guardado ahi.
function inicializarFechas() {
    const inputsFecha = document.querySelectorAll('.dia-bloque input[type="date"]');
    if (inputsFecha.length === 0) return;

    indiceAnclaFecha = null;
    inputsFecha.forEach((input, indice) => {
        if (input.value && indiceAnclaFecha === null) {
            indiceAnclaFecha = indice;
        }
    });

    if (indiceAnclaFecha !== null) {
        recalcularDesdeAncla();
    }
}

// Para cada campo con "maxlength" dentro del formulario, agrega un pequeño mensaje
// (oculto por defecto) que aparece cuando el alumno llega al tope de caracteres permitido.
// No hace falta tocar el HTML: el mensaje se genera solo, campo por campo.
function inicializarAvisosLimite() {
    const campos = document.querySelectorAll('#fieldsetPlanilla [maxlength]');

    campos.forEach(campo => {
        const aviso = document.createElement('small');
        aviso.className = 'aviso-limite';
        aviso.textContent = 'Alcanzaste el límite de caracteres.';
        campo.insertAdjacentElement('afterend', aviso);

        const limite = parseInt(campo.getAttribute('maxlength'), 10);

        const actualizarAviso = () => {
            aviso.style.display = campo.value.length >= limite ? 'block' : 'none';
        };

        campo.addEventListener('input', actualizarAviso);
        actualizarAviso(); // por si el campo ya viene con datos cargados (ej. al editar)
    });
}

document.addEventListener('DOMContentLoaded', () => {
    inicializarFechas();
    inicializarAvisosLimite();
    actualizarTotalHoras();

    document.querySelectorAll('.dia-bloque input[type="number"]').forEach(campo => {
        campo.addEventListener('input', actualizarTotalHoras);
    });

    const inputsFecha = document.querySelectorAll('.dia-bloque input[type="date"]');

    inputsFecha.forEach((input, indice) => {
        input.addEventListener('input', () => {
            if (indiceAnclaFecha === null) {
                // Todavia no hay ancla: el primer campo que el alumno complete
                // pasa a serlo, y el resto se calcula y se bloquea a partir de el.
                if (input.value) {
                    indiceAnclaFecha = indice;
                    recalcularDesdeAncla();
                }
                return;
            }

            // Los demas campos estan bloqueados (readonly) mientras hay una
            // ancla, asi que un input real solo puede venir de ese mismo campo.
            if (indice !== indiceAnclaFecha) return;

            if (!input.value) {
                // Se borro la fecha ancla: se borran las otras cinco y los
                // seis quedan libres de nuevo.
                liberarFechas();
            } else {
                // La ancla cambio de fecha (no se borro): se recalcula el
                // resto en base a la fecha nueva.
                recalcularDesdeAncla();
            }
        });
    });
});
