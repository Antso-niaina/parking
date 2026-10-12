(function () {
    const filter = document.querySelector("[data-zone-filter]");
    if (!filter) return;
    filter.addEventListener("change", () => {
        const zone = filter.value;
        document.querySelectorAll("[data-zone-block]").forEach((block) => {
            const id = block.getAttribute("data-zone-block");
            block.hidden = zone !== "" && id !== zone;
        });
    });
})();
