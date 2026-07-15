document.addEventListener("DOMContentLoaded", () => {
    // ==========================================
    // 1. Light/Dark Theme Switcher Logic
    // ==========================================
    const themeToggle = document.getElementById("theme-toggle");
    
    // Check local storage for previous theme choice
    const savedTheme = localStorage.getItem("theme");
    if (savedTheme === "light") {
        document.body.classList.add("light-theme");
    }

    if (themeToggle) {
        themeToggle.addEventListener("click", () => {
            document.body.classList.toggle("light-theme");
            
            // Save state
            if (document.body.classList.contains("light-theme")) {
                localStorage.setItem("theme", "light");
            } else {
                localStorage.setItem("theme", "dark");
            }
        });
    }

    // ==========================================
    // 2. Horizontal Scroll Deck Arrow Controls
    // ==========================================
    const deck = document.getElementById("showcase-deck");
    const prevBtn = document.getElementById("slide-prev");
    const nextBtn = document.getElementById("slide-next");

    if (deck && prevBtn && nextBtn) {
        prevBtn.addEventListener("click", () => {
            // Scroll left by 320px (one card + gap offset)
            deck.scrollBy({ left: -320, behavior: "smooth" });
        });

        nextBtn.addEventListener("click", () => {
            // Scroll right by 320px
            deck.scrollBy({ left: 320, behavior: "smooth" });
        });
    }

    // ==========================================
    // 3. Zoom Lightbox Modal Logic
    // ==========================================
    const lightbox = document.getElementById("lightbox-modal");
    const lightboxImg = document.getElementById("lightbox-img");
    const lightboxCap = document.getElementById("lightbox-caption");
    const lightboxClose = document.getElementById("lightbox-close");
    const zoomableFrames = document.querySelectorAll(".zoomable-image");

    zoomableFrames.forEach(frame => {
        frame.addEventListener("click", () => {
            const innerImg = frame.querySelector(".phone-screen");
            const captionText = frame.getAttribute("data-caption");

            if (innerImg && lightbox && lightboxImg) {
                lightbox.style.display = "block";
                lightboxImg.src = innerImg.src;
                if (lightboxCap) lightboxCap.textContent = captionText || "";
                
                // Add class to prevent background scrolling
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
        // Clicking outside the image closes it
        lightbox.addEventListener("click", (e) => {
            if (e.target === lightbox) {
                closeLightbox();
            }
        });
    }

    // Close on Escape key press
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            closeLightbox();
        }
    });

    // ==========================================
    // 4. Scroll Reveal Animation for Feature Cards
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
