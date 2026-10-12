(function () {
    const sidebar = document.querySelector("[data-sidebar]");
    const backdrop = document.querySelector("[data-backdrop]");
    const toggles = document.querySelectorAll("[data-sidebar-toggle]");

    function setOpen(open) {
        if (!sidebar) return;
        sidebar.classList.toggle("is-open", open);
        if (backdrop) backdrop.classList.toggle("is-open", open);
        document.body.style.overflow = open ? "hidden" : "";
    }

    toggles.forEach((btn) => btn.addEventListener("click", () => setOpen(!sidebar.classList.contains("is-open"))));
    if (backdrop) backdrop.addEventListener("click", () => setOpen(false));

    document.querySelectorAll("[data-chart-fill]").forEach((el) => {
        const raw = Number(el.getAttribute("data-chart-fill"));
        const pct = Number.isFinite(raw) ? Math.max(0, Math.min(100, raw)) : 0;
        el.style.width = pct + "%";
    });

    document.querySelectorAll("[data-bar-group]").forEach((group) => {
        const fills = [...group.querySelectorAll("[data-bar-value]")];
        const max = Math.max(1, ...fills.map((el) => Number(el.getAttribute("data-bar-value")) || 0));
        fills.forEach((el) => {
            const value = Number(el.getAttribute("data-bar-value")) || 0;
            el.style.width = Math.round((value / max) * 100) + "%";
        });
    });

    const profile = document.querySelector("[data-profile-menu]");
    if (profile) {
        const btn = profile.querySelector("[data-profile-toggle]");
        const panel = profile.querySelector("[data-profile-panel]");
        if (btn && panel) {
            btn.addEventListener("click", () => {
                const open = panel.hasAttribute("hidden");
                panel.toggleAttribute("hidden", !open);
                btn.setAttribute("aria-expanded", String(open));
            });
            document.addEventListener("click", (e) => {
                if (!profile.contains(e.target)) {
                    panel.setAttribute("hidden", "");
                    btn.setAttribute("aria-expanded", "false");
                }
            });
        }
    }
})();
