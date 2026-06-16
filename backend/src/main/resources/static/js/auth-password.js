(function () {
    "use strict";

    function initPasswordToggles(root) {
        var scope = root || document;
        var wraps = scope.querySelectorAll(".password-input-wrap");
        for (var i = 0; i < wraps.length; i++) {
            var wrap = wraps[i];
            if (wrap.dataset.toggleInit === "true") {
                continue;
            }
            wrap.dataset.toggleInit = "true";
            var input = wrap.querySelector("input");
            var toggle = wrap.querySelector(".password-toggle");
            if (!input || !toggle) {
                continue;
            }
            toggle.addEventListener("click", function (event) {
                event.preventDefault();
                var btn = event.currentTarget;
                var field = btn.parentElement.querySelector("input");
                if (!field) {
                    return;
                }
                var show = field.type === "password";
                field.type = show ? "text" : "password";
                btn.setAttribute("aria-label", show ? "Hide password" : "Show password");
                btn.textContent = show ? "\u{1F648}" : "\u{1F441}\uFE0F";
            });
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", function () {
            initPasswordToggles(document);
        });
    } else {
        initPasswordToggles(document);
    }

    window.AuthPassword = { initPasswordToggles: initPasswordToggles };
})();
