function setHeaderHeight() {
    const nav = document.querySelector('.nav');
    const h = (nav && nav.offsetHeight) ? nav.offsetHeight : 64;
    document.documentElement.style.setProperty('--header-height', h + 'px');
}

function handleInternalAnchors() {
    document.querySelectorAll('a[href^="#"]').forEach(a => {
        a.addEventListener('click', (e) => {
            const href = a.getAttribute('href');
            if (!href || href === '#') return;
            const target = document.querySelector(href);
            if (!target) return;
            // if link is external target (has target=_blank) skip
            if (a.target === '_blank') return;
            e.preventDefault();
            const headerOffset = parseInt(getComputedStyle(document.documentElement).getPropertyValue('--header-height')) || 64;
            const top = target.getBoundingClientRect().top + window.pageYOffset - headerOffset - 8;
            window.scrollTo({ top, behavior: 'smooth' });
            // close mobile nav if open
            const mob = document.querySelector('.nav-links-mob.open');
            if (mob) mob.classList.remove('open');
            const overlay = document.querySelector('.nav-overlay');
            if (overlay) overlay.classList.remove('open');
        });
    });
}

function setupMenuToggle() {
    const toggle = document.querySelector('.menu-toggle');
    const mob = document.querySelector('.nav-links-mob');
    const overlay = document.querySelector('.nav-overlay');
    if (!toggle || !mob) return;
    toggle.addEventListener('click', () => {
        mob.classList.toggle('open');
        if (overlay) overlay.classList.toggle('open');
        const expanded = toggle.getAttribute('aria-expanded') === 'true';
        toggle.setAttribute('aria-expanded', (!expanded).toString());
    });
    if (overlay) overlay.addEventListener('click', () => {
        mob.classList.remove('open');
        overlay.classList.remove('open');
        toggle.setAttribute('aria-expanded', 'false');
    });
}

document.addEventListener('DOMContentLoaded', () => {
    setHeaderHeight();
    handleInternalAnchors();
    setupMenuToggle();
    setupAuthModal();
});
window.addEventListener('resize', () => setHeaderHeight());

// /* Modal handling */
// function openModal(modal){
//     if(!modal) return;
//     modal.classList.add('open');
//     modal.setAttribute('aria-hidden','false');
// }
// function closeModal(modal){
//     if(!modal) return;
//     modal.classList.remove('open');
//     modal.setAttribute('aria-hidden','true');
// }
// function setupModal(){
//     document.querySelectorAll('.open-modal').forEach(btn=>{
//         btn.addEventListener('click', (e)=>{
//             e.preventDefault();
//             const selector = btn.dataset.target || '#auth-modal';
//             const modal = document.querySelector(selector);
//             if(modal) openModal(modal);
//         });
//     });
//     const modal = document.querySelector('#auth-modal');
//     if(!modal) return;
//     modal.addEventListener('click', (e)=>{
//         const action = e.target.dataset.action;
//         if(action === 'close' || e.target.classList.contains('auth-overlay')){
//             closeModal(modal);
//         }
//     });
//     modal.querySelectorAll('[data-action="close"]').forEach(el=>el.addEventListener('click', ()=>closeModal(modal)));
//     // tabs
//     modal.querySelectorAll('.tab-btn').forEach(btn=>{
//         btn.addEventListener('click', ()=>{
//             modal.querySelectorAll('.tab-btn').forEach(b=>b.classList.remove('active'));
//             btn.classList.add('active');
//             const tab = btn.dataset.tab;
//             modal.querySelectorAll('.auth-form').forEach(f=>{
//                 if(f.dataset.form === tab){ f.hidden = false } else { f.hidden = true }
//             });
//         });
//     });
// }
