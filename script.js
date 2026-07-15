document.addEventListener("DOMContentLoaded", () => {
    // ==========================================
    // 1. Interactive Timetable Day Switcher (Matches Real App Screenshots)
    // ==========================================
    const dayCols = document.querySelectorAll(".mock-day-col");
    const classItem = document.getElementById("mock-class-item");
    const classTitle = document.getElementById("mock-class-title");
    const classSubtitle = document.getElementById("mock-class-subtitle");
    const classAttendance = document.getElementById("mock-class-attendance");
    const timelineDot = document.querySelector(".mock-timeline-dot");

    const dayData = {
        MON: { title: "DSa (Data Structures)", subtitle: "09:00 AM - 09:50 AM • Room A18 • Indu", attendance: "100%", color: "#10b981", timelineOffset: "20%" },
        TUE: { title: "ML (Machine Learning)", subtitle: "10:00 AM - 10:50 AM • Room A18 • Hari", attendance: "80%", color: "#ec4899", timelineOffset: "45%" },
        WED: { title: "Quant (Quantitative)", subtitle: "11:00 AM - 11:50 AM • Room A17 • Pathak", attendance: "OVER", color: "#3b82f6", timelineOffset: "70%" },
        THU: { title: "ic253 (Intelligent Systems)", subtitle: "09:00 AM - 09:50 AM • Room A17 • Admin", attendance: "90%", color: "#a855f7", timelineOffset: "20%" },
        FRI: { title: "ep301 (Embedded Systems)", subtitle: "11:00 AM - 11:50 AM • Room A17 • Staff", attendance: "85%", color: "#3b82f6", timelineOffset: "70%" },
        SAT: { title: "Self Study & Lab tasks", subtitle: "No scheduled lectures today", attendance: "FREE", color: "#10b981", timelineOffset: "50%" }
    };

    dayCols.forEach(col => {
        col.addEventListener("click", () => {
            const day = col.getAttribute("data-day");
            const data = dayData[day];
            if (!data) return;

            // Swap active classes
            dayCols.forEach(c => c.classList.remove("col-active"));
            col.classList.add("col-active");

            // Play clean fade animation on card transition
            classItem.style.opacity = "0";
            classItem.style.transform = "scale(0.98) translateY(5px)";
            
            setTimeout(() => {
                classTitle.textContent = data.title;
                classSubtitle.textContent = data.subtitle;
                classAttendance.textContent = data.attendance;
                
                // Update color accent bar
                const colorBar = classItem.querySelector(".mock-class-color");
                if (colorBar) colorBar.style.backgroundColor = data.color;
                
                // Update attendance indicator color depending on percent/state
                if (data.attendance === "OVER") {
                    classAttendance.style.color = "#94a3b8";
                    classAttendance.style.backgroundColor = "rgba(255, 255, 255, 0.05)";
                } else if (data.attendance === "FREE") {
                    classAttendance.style.color = "#10b981";
                    classAttendance.style.backgroundColor = "rgba(16, 185, 129, 0.1)";
                } else {
                    const pct = parseInt(data.attendance);
                    if (pct >= 90) {
                        classAttendance.style.color = "#10b981";
                        classAttendance.style.backgroundColor = "rgba(16, 185, 129, 0.1)";
                    } else {
                        classAttendance.style.color = "#3b82f6";
                        classAttendance.style.backgroundColor = "rgba(59, 130, 246, 0.1)";
                    }
                }

                // Smooth slide timeline dot
                if (timelineDot) timelineDot.style.left = data.timelineOffset;

                classItem.style.opacity = "1";
                classItem.style.transform = "scale(1) translateY(0)";
            }, 180);
        });
    });

    // ==========================================
    // 2. Wallpaper Adaptive Contrast Simulator
    // ==========================================
    const wpOpts = document.querySelectorAll(".wp-opt");
    const glassPhone = document.getElementById("hero-glass-phone");

    wpOpts.forEach(btn => {
        btn.addEventListener("click", () => {
            const wallpaper = btn.getAttribute("data-wp");
            
            // Highlight selected wallpaper option dot
            wpOpts.forEach(opt => opt.classList.remove("active"));
            btn.classList.add("active");

            // Change background wallpaper on the phone card
            glassPhone.setAttribute("data-wallpaper", wallpaper);

            // Display feedback Toast
            const isLight = wallpaper === "light-marble" || wallpaper === "bright-sunset";
            showAndroidToast(
                isLight 
                    ? `Wallpaper light colors detected. Widgets inverting to dark high-contrast text.` 
                    : `Wallpaper dark colors detected. Widgets loading in neon/white layouts.`
            );
        });
    });

    // ==========================================
    // 3. Study Mode & Spam Notification Blocker
    // ==========================================
    const dndToggle = document.getElementById("mock-dnd-toggle");
    const shieldEffect = document.getElementById("dnd-shield-effect");
    const btnSpam = document.getElementById("btn-spam-trigger");
    const notifContainer = document.getElementById("mock-notification-container");
    const toast = document.getElementById("android-toast");

    let toastTimeout;
    function showAndroidToast(message) {
        clearTimeout(toastTimeout);
        toast.textContent = message;
        toast.classList.add("show");
        toastTimeout = setTimeout(() => {
            toast.classList.remove("show");
        }, 3200);
    }

    // Toggle active shield
    dndToggle.addEventListener("change", () => {
        if (dndToggle.checked) {
            shieldEffect.classList.add("active");
            showAndroidToast("Study Mode Active: Silencing All Incoming Alerts 🛡️");
        } else {
            shieldEffect.classList.remove("active");
            showAndroidToast("Study Mode Inactive: Restoring Device Ringtones.");
        }
    });

    // Simulated Spam Messages
    const spamMessages = [
        { sender: "Promo Bot", msg: "Special discount! Claim within 5 mins!" },
        { sender: "Clash Wars Guild", msg: "Urgent: Join the war node now!" },
        { sender: "BestDeals", msg: "Cheap subscriptions! Tap here." }
    ];
    let spamIdx = 0;

    btnSpam.addEventListener("click", () => {
        const item = spamMessages[spamIdx];
        spamIdx = (spamIdx + 1) % spamMessages.length;

        // Create notification card element
        const notif = document.createElement("div");
        notif.className = "mock-notification";
        notif.innerHTML = `
            <h4>💬 ${item.sender}</h4>
            <p>${item.msg}</p>
        `;

        notifContainer.appendChild(notif);

        // Check if DND is active
        if (dndToggle.checked) {
            // Block/silence animation sequence
            setTimeout(() => {
                notif.style.transform = "translateY(-10px) scale(0.95)";
                notif.style.borderColor = "var(--color-green)";
                notif.style.boxShadow = "0 0 15px rgba(16, 185, 129, 0.4)";
                
                // Flash the shield in response
                shieldEffect.style.borderColor = "#fff";
                shieldEffect.style.boxShadow = "0 0 35px rgba(16, 185, 129, 0.9)";
                
                setTimeout(() => {
                    shieldEffect.style.borderColor = "var(--color-green)";
                    shieldEffect.style.boxShadow = "0 0 20px rgba(16, 185, 129, 0.4)";
                    
                    notif.classList.add("fade-out");
                    setTimeout(() => notif.remove(), 400);
                }, 300);

                showAndroidToast(`[Blocked] Spam alert from '${item.sender}' silenced.`);
            }, 600);
        } else {
            // DND inactive: Play vibration/disturbance animation
            setTimeout(() => {
                glassPhone.style.transform = "rotate(1deg) scale(1.01)";
                setTimeout(() => {
                    glassPhone.style.transform = "rotate(-1deg) scale(1.01)";
                    setTimeout(() => {
                        glassPhone.style.transform = "rotate(0deg) scale(1)";
                    }, 50);
                }, 50);

                // Normal message auto dismiss
                setTimeout(() => {
                    notif.classList.add("fade-out");
                    setTimeout(() => notif.remove(), 400);
                }, 3000);
            }, 500);
        }
    });

    // ==========================================
    // 4. Secret Glass Lab Customizer Sliders
    // ==========================================
    const sliderBlur = document.getElementById("slider-blur");
    const sliderDisplacement = document.getElementById("slider-displacement");
    const sliderThickness = document.getElementById("slider-thickness");
    
    const valBlur = document.getElementById("val-blur");
    const valDisplacement = document.getElementById("val-displacement");
    const valThickness = document.getElementById("val-thickness");
    
    const glassCard = document.getElementById("glass-preview-card");
    
    function updateGlassStyle() {
        const blur = sliderBlur.value;
        const displacement = sliderDisplacement.value;
        const thickness = sliderThickness.value;
        
        // Update slider display text
        valBlur.textContent = `${blur}px`;
        valDisplacement.textContent = displacement;
        valThickness.textContent = `${thickness}px`;
        
        // Apply CSS styles to target card dynamically
        glassCard.style.backdropFilter = `blur(${blur}px) saturate(180%)`;
        glassCard.style.webkitBackdropFilter = `blur(${blur}px) saturate(180%)`;
        glassCard.style.borderRadius = `${thickness}px`;
        
        // Simulate refraction/displacement with shadow spread and specular intensity variables
        const shadowOpacity = Math.min(0.8, 0.2 + parseFloat(displacement) * 0.5);
        const shadowSpread = Math.round(displacement * 20);
        glassCard.style.boxShadow = `0 ${shadowSpread}px 40px rgba(0, 0, 0, ${shadowOpacity}), inset 0 1px 1px rgba(255, 255, 255, ${parseFloat(displacement) * 0.4})`;
    }
    
    if (sliderBlur && sliderDisplacement && sliderThickness) {
        sliderBlur.addEventListener("input", updateGlassStyle);
        sliderDisplacement.addEventListener("input", updateGlassStyle);
        sliderThickness.addEventListener("input", updateGlassStyle);
        
        // Initialize once
        updateGlassStyle();
    }
    
    // ==========================================
    // 5. Interactive 3D Glass Tilt Effect
    // ==========================================
    if (glassCard) {
        glassCard.addEventListener("mousemove", (e) => {
            const rect = glassCard.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;
            
            const centerX = rect.width / 2;
            const centerY = rect.height / 2;
            
            const rotateX = ((centerY - y) / centerY) * 12;
            const rotateY = ((x - centerX) / centerX) * 12;
            
            glassCard.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale3d(1.02, 1.02, 1.02)`;
        });
        
        glassCard.addEventListener("mouseleave", () => {
            glassCard.style.transform = "perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)";
        });
    }
    
    // ==========================================
    // 6. Scroll Reveal Animation for Feature Cards
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
                observer.unobserve(entry.target);
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
