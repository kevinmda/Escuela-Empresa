// Aviso de cookies: el sitio solo usa cookies necesarias (sesión y "recordarme",
// ver SecurityConfig.java), no de publicidad ni seguimiento, así que el aviso es
// puramente informativo: un único botón "Entendido", sin aceptar/rechazar.
(function () {
    var CLAVE = 'aviso-cookies-visto';

    document.addEventListener('DOMContentLoaded', function () {
        if (localStorage.getItem(CLAVE) === '1') return;

        var aviso = document.createElement('div');
        aviso.className = 'aviso-cookies';
        aviso.setAttribute('role', 'region');
        aviso.setAttribute('aria-label', 'Aviso de cookies');

        var texto = document.createElement('p');
        texto.className = 'aviso-cookies-texto';
        texto.innerHTML = 'Usamos cookies necesarias para mantener tu sesión iniciada y, si ' +
            'lo activás, "recordarme". No se usan para publicidad ni seguimiento. ' +
            '<a href="/terminos-y-condiciones">Más información</a>.';

        var boton = document.createElement('button');
        boton.type = 'button';
        boton.className = 'btn-primary aviso-cookies-boton';
        boton.textContent = 'Entendido';
        boton.addEventListener('click', function () {
            localStorage.setItem(CLAVE, '1');
            aviso.remove();
        });

        aviso.appendChild(texto);
        aviso.appendChild(boton);
        document.body.appendChild(aviso);
    });
})();
