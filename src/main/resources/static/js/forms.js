(function () {
    document.querySelectorAll("form[data-confirm]").forEach((form) => {
        form.addEventListener("submit", (e) => {
            const message = form.getAttribute("data-confirm") || "Confirmer cette action ?";
            if (!window.confirm(message)) {
                e.preventDefault();
            }
        }, true);
    });

    document.querySelectorAll("form[data-once]").forEach((form) => {
        form.addEventListener("submit", (e) => {
            if (e.defaultPrevented) return;
            const submit = form.querySelector("[type=submit]");
            if (submit) {
                submit.disabled = true;
                submit.classList.add("is-loading");
            }
        });
    });

    document.querySelectorAll("[data-toggle-password]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const input = document.getElementById(btn.getAttribute("data-toggle-password"));
            if (!input) return;
            const show = input.type === "password";
            input.type = show ? "text" : "password";
            btn.setAttribute("aria-pressed", String(show));
            const label = show ? "Masquer le mot de passe" : "Afficher le mot de passe";
            btn.setAttribute("aria-label", label);
            btn.setAttribute("title", label);
        });
    });
})();
