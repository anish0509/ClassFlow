document.addEventListener("DOMContentLoaded", () => {
    // Scroll Reveal Animation for Feature Cards
    const featureCards = document.querySelectorAll(".feature-card");
    
    const observerOptions = {
        threshold: 0.1,
        rootMargin: "0px 0px -50px 0px"
    };
    
    const revealObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach((entry) => {
            if (entry.isIntersecting) {
                entry.target.classList.add("reveal-active");
                revealObserver.unobserve(entry.target);
            }
        });
    }, observerOptions);
    
    featureCards.forEach((card, index) => {
        card.style.opacity = "0";
        card.style.transform = "translateY(30px)";
        card.style.transition = `all 0.6s cubic-bezier(0.16, 1, 0.3, 1) ${index * 0.1}s`;
        
        revealObserver.observe(card);
    });
    
    const styleSheet = document.createElement("style");
    styleSheet.innerText = `
        .feature-card.reveal-active {
            opacity: 1 !important;
            transform: translateY(0) !important;
        }
    `;
    document.head.appendChild(styleSheet);
});
