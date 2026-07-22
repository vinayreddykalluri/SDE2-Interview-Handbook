import mermaid from "https://unpkg.com/mermaid@10.4.0/dist/mermaid.esm.min.mjs";

mermaid.initialize({
  startOnLoad: false,
  securityLevel: "strict",
  theme: "base",
  fontFamily: "IBM Plex Mono, monospace",
  themeVariables: {
    background: "#fffaf0",
    primaryColor: "#f4e6d3",
    primaryTextColor: "#102a38",
    primaryBorderColor: "#c8662d",
    lineColor: "#35576a",
    secondaryColor: "#ffffff",
    tertiaryColor: "#f4e6d3"
  }
});

window.mermaid = mermaid;

async function renderMermaidDiagrams() {
  const sources = [...document.querySelectorAll("pre.mermaid-source")];
  for (const source of sources) {
    const diagram = document.createElement("div");
    diagram.className = "mermaid";
    diagram.textContent = source.textContent.trim();
    source.replaceWith(diagram);
  }

  const pending = [...document.querySelectorAll(".mermaid:not([data-processed='true'])")];
  if (pending.length === 0) {
    return;
  }

  try {
    await mermaid.run({ nodes: pending });
  } catch (error) {
    console.error("Unable to render Mermaid diagrams", error);
  }
}

if (window.document$ && typeof window.document$.subscribe === "function") {
  window.document$.subscribe(() => {
    void renderMermaidDiagrams();
  });
} else if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", () => {
    void renderMermaidDiagrams();
  }, { once: true });
} else {
  void renderMermaidDiagrams();
}
