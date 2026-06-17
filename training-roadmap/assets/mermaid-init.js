(function () {
  const config = {
    startOnLoad: false,
    securityLevel: "loose",
    theme: "base",
    flowchart: {
      htmlLabels: true,
      curve: "basis",
      nodeSpacing: 48,
      rankSpacing: 74,
      padding: 12
    },
    sequence: {
      mirrorActors: false,
      messageAlign: "center",
      boxMargin: 12
    },
    themeVariables: {
      background: "#ffffff",
      primaryColor: "#e8f0ff",
      primaryTextColor: "#172033",
      primaryBorderColor: "#1d4ed8",
      lineColor: "#475569",
      secondaryColor: "#e7f7ef",
      tertiaryColor: "#fff4df",
      noteBkgColor: "#fff4df",
      noteTextColor: "#172033",
      fontFamily: "Arial, Helvetica, sans-serif"
    }
  };

  function markFailure(message) {
    document.querySelectorAll(".mermaid").forEach((node) => {
      node.classList.add("mermaid-failed");
      node.setAttribute("data-render-error", message);
    });
  }

  async function render() {
    if (!window.mermaid) {
      markFailure("Mermaid runtime is not available");
      return;
    }
    try {
      window.mermaid.initialize(config);
      await window.mermaid.run({ querySelector: ".mermaid" });
    } catch (error) {
      markFailure(error && error.message ? error.message : "Mermaid render failed");
      console.error(error);
    }
  }

  function onReady(callback) {
    if (document.readyState === "loading") {
      document.addEventListener("DOMContentLoaded", callback, { once: true });
    } else {
      callback();
    }
  }

  function loadRuntime() {
    const currentScript = document.currentScript;
    const currentSrc = currentScript && currentScript.src ? currentScript.src : "";
    const base = currentSrc.slice(0, currentSrc.lastIndexOf("/") + 1);
    const script = document.createElement("script");
    script.src = base + "vendor/mermaid.min.js";
    script.onload = function () {
      onReady(render);
    };
    script.onerror = function () {
      markFailure("Could not load local Mermaid runtime");
    };
    document.head.appendChild(script);
  }

  if (window.mermaid) {
    onReady(render);
  } else {
    loadRuntime();
  }
})();
