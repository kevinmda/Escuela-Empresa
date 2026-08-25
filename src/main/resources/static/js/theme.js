(function () {
    const root = document.documentElement;
    const savedTheme = localStorage.getItem('theme');
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const initialTheme = savedTheme || (prefersDark ? 'dark' : 'light');

    root.dataset.theme = initialTheme;

    function actualizarBoton() {
        const button = document.getElementById('theme-toggle');
        if (!button) return;

        const darkMode = root.dataset.theme === 'dark';
        button.textContent = darkMode ? 'Modo claro' : 'Modo oscuro';
        button.setAttribute('aria-label', darkMode ? 'Activar modo claro' : 'Activar modo oscuro');
        button.setAttribute('title', darkMode ? 'Activar modo claro' : 'Activar modo oscuro');
    }

    document.addEventListener('DOMContentLoaded', function () {
        actualizarBoton();
        const button = document.getElementById('theme-toggle');
        if (!button) return;

        button.addEventListener('click', function () {
            const nextTheme = root.dataset.theme === 'dark' ? 'light' : 'dark';
            root.dataset.theme = nextTheme;
            localStorage.setItem('theme', nextTheme);
            actualizarBoton();
        });
    });
})();
