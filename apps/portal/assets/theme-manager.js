(function () {
  "use strict";

  if (window.SDE2Theme) return;

  const STORAGE_KEY = "sde2-study-path-theme";
  const MODES = ["system", "light", "dark"];
  const DARK_MEDIA = window.matchMedia("(prefers-color-scheme: dark)");
  const THEME_COLORS = {
    light: "#f4efe3",
    dark: "#0d1718"
  };

  function normalizeMode(value) {
    return MODES.includes(value) ? value : "system";
  }

  function readMode() {
    try {
      return normalizeMode(localStorage.getItem(STORAGE_KEY));
    } catch {
      return "system";
    }
  }

  function resolveTheme(mode) {
    if (mode === "system") return DARK_MEDIA.matches ? "dark" : "light";
    return mode;
  }

  function updateThemeColor(theme) {
    const themeColor = document.querySelector('meta[name="theme-color"]');
    if (themeColor) themeColor.setAttribute("content", THEME_COLORS[theme]);
  }

  function updateMaterialSurface(theme) {
    if (!document.body || !document.body.hasAttribute("data-md-color-scheme")) return;
    document.body.setAttribute("data-md-color-scheme", theme === "dark" ? "slate" : "default");
    document.body.setAttribute("data-md-color-primary", theme === "dark" ? "black" : "slate");
    document.body.setAttribute("data-md-color-accent", "amber");
  }

  function updateControls(mode, theme) {
    document.querySelectorAll("[data-theme-selector]").forEach(function (selector) {
      selector.value = mode;
      selector.setAttribute(
        "aria-label",
        "Color theme: " + mode + (mode === "system" ? " (currently " + theme + ")" : "")
      );
    });

    document.querySelectorAll("[data-theme-status]").forEach(function (status) {
      status.textContent = mode === "system" ? "System · " + theme : mode;
    });
  }

  function applyTheme(nextMode, options) {
    const mode = normalizeMode(nextMode);
    const theme = resolveTheme(mode);
    const persist = !options || options.persist !== false;

    document.documentElement.dataset.themeMode = mode;
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme;
    updateThemeColor(theme);
    updateMaterialSurface(theme);
    updateControls(mode, theme);

    if (persist) {
      try {
        localStorage.setItem(STORAGE_KEY, mode);
      } catch {
        // Theme persistence is optional when storage is unavailable.
      }
    }

    document.dispatchEvent(new CustomEvent("sde2themechange", {
      detail: { mode: mode, theme: theme }
    }));
  }

  function connectControls() {
    document.querySelectorAll("[data-theme-selector]").forEach(function (selector) {
      if (selector.dataset.themeConnected === "true") return;
      selector.dataset.themeConnected = "true";
      selector.addEventListener("change", function () {
        applyTheme(selector.value);
      });
    });
    applyTheme(readMode(), { persist: false });
  }

  const initialMode = readMode();
  applyTheme(initialMode, { persist: false });

  if (!document.body) {
    const bodyObserver = new MutationObserver(function () {
      if (!document.body) return;
      bodyObserver.disconnect();
      applyTheme(readMode(), { persist: false });
    });
    bodyObserver.observe(document.documentElement, { childList: true });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", connectControls, { once: true });
  } else {
    connectControls();
  }

  const handleSystemThemeChange = function () {
    if (readMode() === "system") applyTheme("system", { persist: false });
  };
  if (typeof DARK_MEDIA.addEventListener === "function") {
    DARK_MEDIA.addEventListener("change", handleSystemThemeChange);
  } else if (typeof DARK_MEDIA.addListener === "function") {
    DARK_MEDIA.addListener(handleSystemThemeChange);
  }

  window.addEventListener("storage", function (event) {
    if (event.key === STORAGE_KEY) applyTheme(normalizeMode(event.newValue), { persist: false });
  });

  window.SDE2Theme = {
    apply: applyTheme,
    current: function () {
      const mode = readMode();
      return { mode: mode, theme: resolveTheme(mode) };
    }
  };
}());
