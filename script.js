document.addEventListener("DOMContentLoaded", () => {
    // ==========================================
    // 1. Zoom Lightbox Modal Logic
    // ==========================================
    const lightbox = document.getElementById("lightbox-modal");
    const lightboxImg = document.getElementById("lightbox-img");
    const lightboxCap = document.getElementById("lightbox-caption");
    const lightboxClose = document.getElementById("lightbox-close");
    
    const heroScreenshot = document.querySelector(".hero-screenshot-wrapper");
    const showcaseCards = document.querySelectorAll(".showcase-card");

    // Click handler for Hero Screenshot Mockup
    if (heroScreenshot) {
        heroScreenshot.style.cursor = "zoom-in";
        heroScreenshot.addEventListener("click", () => {
            const innerImg = heroScreenshot.querySelector("img");
            if (innerImg && lightbox && lightboxImg) {
                lightbox.style.display = "block";
                lightboxImg.src = innerImg.src;
                if (lightboxCap) {
                    lightboxCap.innerHTML = `<strong>Home Dashboard</strong> — Class countdowns and attendance metrics.`;
                }
                document.body.style.overflow = "hidden";
            }
        });
    }

    // Click handler for Scroll Showcase Cards
    showcaseCards.forEach(card => {
        card.addEventListener("click", () => {
            const img = card.querySelector(".phone-screen");
            const title = card.querySelector(".card-info h3");
            const desc = card.querySelector(".card-info p");

            if (img && lightbox && lightboxImg) {
                lightbox.style.display = "block";
                lightboxImg.src = img.src;
                if (lightboxCap && title && desc) {
                    lightboxCap.innerHTML = `<strong>${title.textContent}</strong> — ${desc.textContent}`;
                }
                document.body.style.overflow = "hidden";
            }
        });
    });

    // Close Lightbox triggers
    const closeLightbox = () => {
        if (lightbox) {
            lightbox.style.display = "none";
            document.body.style.overflow = "auto";
        }
    };

    if (lightboxClose) {
        lightboxClose.addEventListener("click", closeLightbox);
    }

    if (lightbox) {
        // Close modal when clicking on the blurred background backdrop
        lightbox.addEventListener("click", (e) => {
            if (e.target === lightbox) {
                closeLightbox();
            }
        });
    }

    // Escape key closes modal
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            closeLightbox();
        }
    });

    // ==========================================
    // 2. Scroll Reveal Animation for Feature Cards
    // ==========================================
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
        card.style.transform = "translateY(25px)";
        card.style.transition = `all 0.6s cubic-bezier(0.16, 1, 0.3, 1) ${index * 0.08}s`;
        
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
