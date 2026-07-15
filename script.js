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
    // 2. 3D Screenshot Stack Controller
    // ==========================================
    const stackCards = document.querySelectorAll(".stack-card");
    const stackCaption = document.getElementById("stack-caption");
    const prevBtn = document.getElementById("stack-prev");
    const nextBtn = document.getElementById("stack-next");
    
    let activeIdx = 0;
    const totalCards = stackCards.length;

    function updateStack() {
        if (stackCards.length === 0) return;
        
        stackCards.forEach((card, i) => {
            // Reset position classes
            card.className = "stack-card";
            
            // Calculate circular offset
            let diff = (i - activeIdx + totalCards) % totalCards;
            
            if (diff === 0) {
                card.classList.add("active");
                // Update active caption in description box
                const captionText = card.getAttribute("data-caption");
                if (captionText && stackCaption) {
                    stackCaption.textContent = captionText;
                }
            } else if (diff === 1) {
                card.classList.add("next");
            } else if (diff === 2) {
                card.classList.add("next-2");
            } else if (diff === 3) {
                card.classList.add("next-3");
            } else if (diff === totalCards - 1) {
                card.classList.add("hidden-left");
            } else {
                card.classList.add("hidden");
            }
        });
    }

    // Controls listeners
    if (prevBtn && nextBtn) {
        prevBtn.addEventListener("click", () => {
            activeIdx = (activeIdx - 1 + totalCards) % totalCards;
            updateStack();
        });

        nextBtn.addEventListener("click", () => {
            activeIdx = (activeIdx + 1) % totalCards;
            updateStack();
        });
    }

    // Interactive card clicks
    stackCards.forEach((card, i) => {
        card.addEventListener("click", (e) => {
            let diff = (i - activeIdx + totalCards) % totalCards;
            
            if (diff === 0) {
                // If front active card is clicked, trigger Lightbox modal zoom
                openLightbox(card);
            } else {
                // If background card is clicked, advance stack to that card
                activeIdx = i;
                updateStack();
            }
        });
    });

    // Initialize once
    updateStack();

    // ==========================================
    // 3. Zoom Lightbox Modal Logic
    // ==========================================
    const lightbox = document.getElementById("lightbox-modal");
    const lightboxImg = document.getElementById("lightbox-img");
    const lightboxCap = document.getElementById("lightbox-caption");
    const lightboxClose = document.getElementById("lightbox-close");
    const heroScreenshot = document.querySelector(".hero-screenshot-wrapper");

    // Also support zooming hero screenshot card
    if (heroScreenshot) {
        heroScreenshot.style.cursor = "zoom-in";
        heroScreenshot.addEventListener("click", () => {
            const innerImg = heroScreenshot.querySelector("img");
            if (innerImg && lightbox && lightboxImg) {
                lightbox.style.display = "block";
                lightboxImg.src = innerImg.src;
                if (lightboxCap) lightboxCap.textContent = innerImg.alt || "Home Dashboard Screen";
                document.body.style.overflow = "hidden";
            }
        });
    }

    function openLightbox(cardElement) {
        const img = cardElement.querySelector(".stack-img");
        const captionText = cardElement.getAttribute("data-caption");

        if (img && lightbox && lightboxImg) {
            lightbox.style.display = "block";
            lightboxImg.src = img.src;
            if (lightboxCap) lightboxCap.textContent = captionText || "";
            
            // Add class to prevent background scrolling
            document.body.style.overflow = "hidden";
        }
    }

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
        lightbox.addEventListener("click", (e) => {
            if (e.target === lightbox) {
                closeLightbox();
            }
        });
    }

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
