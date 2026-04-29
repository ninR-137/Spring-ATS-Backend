document.addEventListener('DOMContentLoaded', function() {
    const themeToggle = document.querySelector('.theme-toggle');
    const menuToggle = document.querySelector('.menu-toggle');
    const navLinks = document.querySelector('.nav-links-mob');
    const navOverlay = document.querySelector('.nav-overlay');
    const body = document.body;

    if (!themeToggle) {
        console.error('Theme toggle element not found');
        return;
    }

    const savedTheme = localStorage.getItem('theme');
    const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;


    if (savedTheme === 'dark' || (!savedTheme && systemPrefersDark)) {
        body.classList.add('dark-mode');
        console.log('Dark mode applied');
    } else {
        console.log('Light mode applied');
    }

    themeToggle.addEventListener('click', () => {
                console.log('Toggle clicked');
                body.classList.toggle('dark-mode');

                if (body.classList.contains('dark-mode')) {
                    localStorage.setItem('theme', 'dark');
                    console.log('Switched to dark mode');
                } else {
                    localStorage.setItem('theme', 'light');
                    console.log('Switched to light mode');
                }
            });

            if (menuToggle && navLinks && navOverlay) {
                menuToggle.addEventListener('click', (e) => {
                    e.stopPropagation();
                    navLinks.classList.toggle('active');
                    navOverlay.classList.toggle('active');
                    menuToggle.classList.toggle('active');
                    console.log('Mobile menu toggled');
                });
            }

            if (navLinks) {
                navLinks.addEventListener('click', (e) => {
                    if (e.target.tagName === 'A') {
                        navLinks.classList.remove('active');
                        navOverlay.classList.remove('active');
                        menuToggle.classList.remove('active');
                    }
                });
            }

            if (navOverlay) {
                navOverlay.addEventListener('click', () => {
                    navLinks.classList.remove('active');
                    navOverlay.classList.remove('active');
                    menuToggle.classList.remove('active');
                });
            }
        });