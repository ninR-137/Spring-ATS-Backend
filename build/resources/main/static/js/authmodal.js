

function setupAuthModal() {
    const modal = document.getElementById('auth-modal');
    console.log('Auth modal element:', modal);
    
    if(!modal) return;

    modal.addEventListener('click', (e) => {
        console.log('Modal click event:', e.target);
    });
}