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

    // "Semana N (Cargar)" no navega -- a diferencia de un link a una semana ya
    // cargada, es un botón, así que la página nunca se recarga con datos
    // limpios. Sin esto, el panel de Generar PDF/Editar/Eliminar y el
    // resaltado de "activa" seguían mostrando la semana que se estaba mirando
    // antes de tocar "Cargar" acá -- las dos cosas a la vez, la vieja resaltada
    // y la nueva completándose, como si fueran la misma.
    const acciones = document.querySelector('.pl-semana-acciones');
    if (acciones) {
        acciones.style.display = 'none';
    }

    const activa = document.querySelector('.pl-semana-caja--activa');
    if (activa) {
        activa.classList.remove('pl-semana-caja--activa');
    }
}

// Habilita la edición de una planilla YA guardada (a diferencia de
// habilitarFormulario, que es para cargar una semana nueva). A propósito no
// llama a limpiarFormulario(): los datos que ya están cargados tienen que
// quedar tal cual, listos para corregir, no borrarse. El campo oculto
// "idPsEdicion" ya viene con el id correcto desde que se cargó la página
// (lo pone el servidor en cargarParaEdicion), así que "Cargar la planilla"
// va a actualizar esta planilla en vez de crear una nueva sin que haga
// falta tocar nada más acá.
//
// A propósito NO se deshabilita btnNuevo acá (a diferencia de
// habilitarFormulario): su :disabled tiene un estilo especial pensado para
// decir "esta es la que se está completando" (fondo sólido en vez de
// punteado), así que deshabilitarlo durante una edición lo pintaba como
// seleccionado sin que lo estuviera. Si igual se toca "Cargar" a mitad de
// una edición, habilitarFormulario() ya se encarga de limpiar idPsEdicion y
// ocultar el panel de esta semana, así que no hace falta bloquearlo acá.
function habilitarEdicion() {
    document.getElementById('fieldsetPlanilla').disabled = false;

    const btnEditar = document.getElementById('btnEditar');
    if (btnEditar) btnEditar.disabled = true;
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
    ocultarAvisoCambiosSinGuardar();

    // La causa real del bug de guardar sobre la semana equivocada: este campo
    // vive fuera del fieldset (como indice-fecha-ancla) y por eso el bucle de
    // arriba no lo toca. Si la página venía de mirar una semana ya guardada
    // (cargarParaEdicion lo dejó con ese id), y de ahí se pasaba a "Cargar
    // semana nueva" -- que no navega, solo corre este JS -- el id viejo se
    // quedaba puesto y "Cargar la planilla" terminaba actualizando esa semana
    // en vez de crear una nueva.
    const idPsEdicion = document.getElementById('id-ps-edicion');
    if (idPsEdicion) {
        idPsEdicion.value = '';
    }
}

// Aviso de "tenés cambios sin guardar": aparece con el primer input real
// dentro del formulario (nuevo o en edición) y se apaga al guardar (la
// página recarga entera) o al limpiar con "Borrar lo escrito".
function mostrarAvisoCambiosSinGuardar() {
    const aviso = document.getElementById('aviso-cambios-sin-guardar');
    if (aviso) aviso.hidden = false;
}

function ocultarAvisoCambiosSinGuardar() {
    const aviso = document.getElementById('aviso-cambios-sin-guardar');
    if (aviso) aviso.hidden = true;
}

// true mientras el propio formPlanilla se está enviando (submit del botón
// "Cargar la planilla"): a esa navegación no hay que avisarle nada, es
// justamente la forma de guardar. beforeunload no distingue por qué se deja
// la página -- guardar, cerrar la pestaña, tocar un link -- así que hace
// falta esta bandera para no avisar en el único caso en que "salir" es lo
// que el aviso quiere evitar.
let formularioEnviandose = false;

// El aviso nativo del navegador ("¿Salir de la página?") es lo único que de
// verdad puede frenar al alumno antes de perder lo que escribió: nuestro
// propio mensaje de arriba se ve solo si mira la pantalla, pero esto salta
// aunque intente cerrar la pestaña, recargar o irse a otra URL escrita a
// mano. El texto que se le pasa a returnValue no lo respeta ningún
// navegador moderno (por seguridad, para que una página no pueda inventar
// su propio mensaje enganchoso) -- siempre muestran el suyo genérico, pero
// igual hace falta asignarlo para que el cuadro aparezca.
window.addEventListener('beforeunload', (evento) => {
    const aviso = document.getElementById('aviso-cambios-sin-guardar');
    if (formularioEnviandose || !aviso || aviso.hidden) return;

    evento.preventDefault();
    evento.returnValue = '';
});

// Vuelve los seis campos de fecha a su estado inicial: vacios, editables, sin
// ancla. Es lo que pasa al arrancar una planilla nueva y lo que pasa cuando el
// alumno borra la fecha ancla.
function liberarFechas() {
    indiceAnclaFecha = null;
    guardarIndiceAnclaEnCampoOculto();
    document.querySelectorAll('.dia-bloque input[type="date"]').forEach(input => {
        input.value = '';
        input.readOnly = false;
    });
}

// El campo oculto "indiceFechaAncla" viaja con el formulario. Sirve para el
// caso en que el servidor rechaza el envío (por ejemplo, falta el campo
// Supervisor) y vuelve a mostrar la misma página con los seis días ya
// completos (la ancla que el alumno cargó, más las cinco que se calcularon):
// sin este campo, al recargar no hay forma de saber cuál de los seis fue la
// que el alumno realmente tocó -- se veian todos con un valor por igual, y
// "el primero que tenga algo" siempre termina siendo el Lunes, sea o no el
// que el alumno eligio.
function guardarIndiceAnclaEnCampoOculto() {
    const campoOculto = document.getElementById('indice-fecha-ancla');
    if (campoOculto) {
        campoOculto.value = indiceAnclaFecha === null ? '' : String(indiceAnclaFecha);
    }
}

// Recalcula las otras cinco fechas a partir de la fecha ancla y las deja
// bloqueadas. La fecha ancla en si no se toca (sigue editable).
function recalcularDesdeAncla() {
    const inputsFecha = document.querySelectorAll('.dia-bloque input[type="date"]');
    const inputAncla = inputsFecha[indiceAnclaFecha];
    if (!inputAncla || !inputAncla.value) return;

    guardarIndiceAnclaEnCampoOculto();

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

// Si la planilla ya viene con fechas cargadas, primero se confía en el campo
// oculto "indiceFechaAncla" (ver guardarIndiceAnclaEnCampoOculto) -- es lo que
// distingue "el alumno eligió el Martes" de "el Lunes es el primero que tiene
// algo". Ese campo solo existe si esta página ya pasó por el JS antes (un
// reintento después de un error); si no hay nada ahí (primera carga, o una
// planilla vieja para editar), se cae al criterio anterior: el primer campo
// con valor.
function inicializarFechas() {
    const inputsFecha = document.querySelectorAll('.dia-bloque input[type="date"]');
    if (inputsFecha.length === 0) return;

    indiceAnclaFecha = null;

    const campoOculto = document.getElementById('indice-fecha-ancla');
    const indiceGuardado = campoOculto && campoOculto.value !== '' ? parseInt(campoOculto.value, 10) : null;

    if (indiceGuardado !== null && inputsFecha[indiceGuardado] && inputsFecha[indiceGuardado].value) {
        indiceAnclaFecha = indiceGuardado;
    } else {
        inputsFecha.forEach((input, indice) => {
            if (input.value && indiceAnclaFecha === null) {
                indiceAnclaFecha = indice;
            }
        });
    }

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

    // Un solo listener en el fieldset (delegado) alcanza para los seis días y
    // los cuatro campos del resumen: cualquier "input" real -- nunca se
    // dispara por los input.value = ... que pone el propio JS, como en el
    // autocompletado de fechas -- prende el aviso.
    const fieldset = document.getElementById('fieldsetPlanilla');
    if (fieldset) {
        fieldset.addEventListener('input', mostrarAvisoCambiosSinGuardar);
    }

    const formPlanilla = document.getElementById('formPlanilla');
    if (formPlanilla) {
        formPlanilla.addEventListener('submit', () => {
            formularioEnviandose = true;
        });
    }

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
