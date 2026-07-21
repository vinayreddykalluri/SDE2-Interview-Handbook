(() => {
  "use strict";

  const STORAGE_KEYS = {
    theme: "sde2-handbook-theme-v1",
    progress: "sde2-handbook-progress-v1"
  };

  const state = {
    volumes: [],
    completed: loadCompleted(),
    stage: "all",
    query: "",
    searchSelection: -1
  };

  const elements = {
    root: document.documentElement,
    volumeList: document.querySelector("#volume-list"),
    curriculumSearch: document.querySelector("#curriculum-search"),
    filterButtons: [...document.querySelectorAll(".filter-button")],
    completedCount: document.querySelector("#completed-count"),
    volumeCount: document.querySelector("#volume-count"),
    progressFill: document.querySelector("#progress-fill"),
    resetProgress: document.querySelector("#reset-progress"),
    searchDialog: document.querySelector("#search-dialog"),
    searchTriggers: [...document.querySelectorAll(".search-trigger")],
    searchClose: document.querySelector(".dialog-close"),
    globalSearch: document.querySelector("#global-search"),
    searchResults: document.querySelector("#search-results"),
    themeToggle: document.querySelector(".theme-toggle"),
    themeIcon: document.querySelector(".theme-icon"),
    menuToggle: document.querySelector(".menu-toggle"),
    primaryNav: document.querySelector("#primary-nav"),
    toast: document.querySelector("#toast"),
    copyCommand: document.querySelector(".copy-command")
  };

  const staticDestinations = [
    {
      id: "DOC",
      title: "Complete searchable documentation",
      summary: "All chapters, diagrams, exercises, questions, and revision sheets.",
      url: "docs/",
      type: "Documentation",
      keywords: ["docs", "search", "chapters", "handbook"]
    },
    {
      id: "CODE",
      title: "Java implementation library",
      summary: "Semantic Java source, compiler validation, and smoke tests.",
      url: "https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/tree/master/examples/java",
      type: "Source code",
      keywords: ["java", "code", "examples", "github"]
    },
    {
      id: "PRINT",
      title: "PDF and DOCX downloads",
      summary: "Per-volume and combined printable handbook artifacts.",
      url: "docs/downloads/",
      type: "Downloads",
      keywords: ["pdf", "docx", "print", "download"]
    },
    {
      id: "PLAN",
      title: "Structured study plan",
      summary: "Turn the curriculum into an interview-preparation schedule.",
      url: "docs/study-plan/",
      type: "Learning path",
      keywords: ["study", "plan", "schedule", "revision"]
    }
  ];

  initializeTheme();
  initializeNavigation();
  initializeSearch();
  initializeProgress();
  initializeCopyButton();
  initializeReveal();
  loadVolumes();

  async function loadVolumes() {
    try {
      const response = await fetch("content/volumes.json");
      if (!response.ok) {
        throw new Error(`Curriculum request failed with status ${response.status}`);
      }

      state.volumes = await response.json();
      renderStats();
      renderVolumes();
      renderSearchResults("");
    } catch (error) {
      console.error(error);
      elements.volumeList.innerHTML = `
        <div class="empty-state">
          <h3>Curriculum data unavailable</h3>
          <p>Open the <a href="docs/">full documentation</a> or reload this page.</p>
        </div>
      `;
    }
  }

  function renderStats() {
    const chapters = state.volumes.reduce((total, volume) => total + volume.chapters, 0);
    const examples = state.volumes.reduce((total, volume) => total + volume.codeExamples, 0);
    setStat("volumes", state.volumes.length);
    setStat("chapters", chapters);
    setStat("examples", examples);
    elements.volumeCount.textContent = String(state.volumes.length);
    updateProgress();
  }

  function setStat(name, value) {
    const target = document.querySelector(`[data-stat="${name}"]`);
    if (target) {
      target.textContent = String(value);
    }
  }

  function renderVolumes() {
    const visible = state.volumes.filter(matchesCurrentFilter);

    if (!visible.length) {
      elements.volumeList.innerHTML = `
        <div class="empty-state">
          <h3>No matching volume</h3>
          <p>Try a broader concept or switch back to all stages.</p>
        </div>
      `;
      return;
    }

    elements.volumeList.innerHTML = visible.map(volumeTemplate).join("");
    observeReveals(elements.volumeList);
  }

  function matchesCurrentFilter(volume) {
    const stageMatches = state.stage === "all" || volume.stage === state.stage;
    if (!stageMatches) {
      return false;
    }

    const query = normalize(state.query);
    if (!query) {
      return true;
    }

    const searchable = [
      volume.id,
      volume.roman,
      volume.title,
      volume.stage,
      volume.level,
      volume.summary,
      ...volume.keywords
    ].join(" ");

    return normalize(searchable).includes(query);
  }

  function volumeTemplate(volume) {
    const complete = state.completed.has(volume.id);
    const detailId = `volume-detail-${volume.id}`;
    const packageName = `volume${volume.id}`;
    const keywords = volume.keywords
      .slice(0, 7)
      .map(keyword => `<span>${escapeHtml(keyword)}</span>`)
      .join("");

    return `
      <article class="volume-row reveal${complete ? " is-complete" : ""}" data-volume="${volume.id}" data-stage="${escapeHtml(volume.stage)}">
        <button class="volume-summary" type="button" aria-expanded="false" aria-controls="${detailId}">
          <span class="volume-id">${escapeHtml(volume.roman)}</span>
          <span class="volume-title-block">
            <small>${escapeHtml(volume.stage)} / VOL ${volume.id}</small>
            <strong>${escapeHtml(volume.title)}</strong>
            <p>${escapeHtml(volume.summary)}</p>
          </span>
          <span class="volume-meta">
            <span>Chapters<strong>${volume.chapters}</strong></span>
            <span>Java files<strong>${volume.codeExamples}</strong></span>
          </span>
          <span class="volume-control">
            <span class="volume-state-dot" aria-hidden="true"></span>
            <span class="volume-plus" aria-hidden="true">+</span>
          </span>
        </button>

        <div class="volume-detail" id="${detailId}" hidden>
          <div>
            <p class="volume-description">${escapeHtml(volume.summary)}</p>
            <div class="volume-keywords" aria-label="Covered concepts">${keywords}</div>
          </div>
          <dl class="volume-facts">
            <div><dt>Stage</dt><dd>${escapeHtml(volume.stage)}</dd></div>
            <div><dt>Level</dt><dd>${escapeHtml(volume.level)}</dd></div>
            <div><dt>Suggested effort</dt><dd>${escapeHtml(volume.duration)}</dd></div>
            <div><dt>Implementation set</dt><dd>${volume.codeExamples} Java file${volume.codeExamples === 1 ? "" : "s"}</dd></div>
          </dl>
          <div class="volume-actions">
            <a class="volume-link volume-link-primary" href="docs/${encodeURIComponent(volume.slug)}/">Open full volume &rarr;</a>
            <a class="volume-link" href="https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/tree/master/examples/java/src/main/java/io/github/vinayreddykalluri/interviewhandbook/${packageName}">Browse source</a>
            <button class="complete-button" type="button" data-complete="${volume.id}" aria-pressed="${complete}">
              ${complete ? "Completed" : "Mark complete"}
            </button>
          </div>
        </div>
      </article>
    `;
  }

  elements.volumeList.addEventListener("click", event => {
    const summary = event.target.closest(".volume-summary");
    if (summary) {
      const row = summary.closest(".volume-row");
      const detail = row.querySelector(".volume-detail");
      const opening = !row.classList.contains("is-open");
      row.classList.toggle("is-open", opening);
      summary.setAttribute("aria-expanded", String(opening));
      detail.hidden = !opening;
      return;
    }

    const completeButton = event.target.closest("[data-complete]");
    if (completeButton) {
      toggleComplete(completeButton.dataset.complete);
    }
  });

  elements.curriculumSearch.addEventListener("input", event => {
    state.query = event.target.value;
    renderVolumes();
  });

  elements.filterButtons.forEach(button => {
    button.addEventListener("click", () => {
      setStage(button.dataset.stage);
    });
  });

  document.querySelectorAll("[data-stage-link]").forEach(link => {
    link.addEventListener("click", () => {
      setStage(link.dataset.stageLink);
    });
  });

  function setStage(stage) {
    state.stage = stage;
    elements.filterButtons.forEach(button => {
      button.classList.toggle("is-active", button.dataset.stage === stage);
    });
    renderVolumes();
  }

  function toggleComplete(id) {
    if (state.completed.has(id)) {
      state.completed.delete(id);
    } else {
      state.completed.add(id);
    }

    saveCompleted();
    renderVolumes();
    updateProgress();
  }

  function initializeProgress() {
    elements.resetProgress.addEventListener("click", () => {
      if (!state.completed.size) {
        showToast("Progress is already empty");
        return;
      }

      state.completed.clear();
      saveCompleted();
      renderVolumes();
      updateProgress();
      showToast("Local progress reset");
    });
  }

  function updateProgress() {
    const total = state.volumes.length || 19;
    const validCompleted = [...state.completed].filter(id =>
      !state.volumes.length || state.volumes.some(volume => volume.id === id)
    ).length;
    const percentage = Math.round((validCompleted / total) * 100);
    elements.completedCount.textContent = String(validCompleted);
    elements.progressFill.style.width = `${percentage}%`;
  }

  function loadCompleted() {
    try {
      const value = JSON.parse(localStorage.getItem(STORAGE_KEYS.progress) || "[]");
      return new Set(Array.isArray(value) ? value : []);
    } catch {
      return new Set();
    }
  }

  function saveCompleted() {
    try {
      localStorage.setItem(STORAGE_KEYS.progress, JSON.stringify([...state.completed]));
    } catch {
      showToast("Progress could not be saved in this browser");
    }
  }

  function initializeTheme() {
    const saved = readStorage(STORAGE_KEYS.theme);
    const preferred = window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    setTheme(saved || preferred);

    elements.themeToggle.addEventListener("click", () => {
      const next = elements.root.dataset.theme === "dark" ? "light" : "dark";
      setTheme(next);
      writeStorage(STORAGE_KEYS.theme, next);
    });
  }

  function setTheme(theme) {
    elements.root.dataset.theme = theme;
    elements.themeIcon.textContent = theme === "dark" ? "DAY" : "NIGHT";
    elements.themeToggle.setAttribute(
      "aria-label",
      theme === "dark" ? "Switch to light theme" : "Switch to dark theme"
    );
    document.querySelector('meta[name="theme-color"]').setAttribute(
      "content",
      theme === "dark" ? "#101917" : "#f2eddf"
    );
  }

  function initializeNavigation() {
    elements.menuToggle.addEventListener("click", () => {
      const opening = !elements.primaryNav.classList.contains("is-open");
      elements.primaryNav.classList.toggle("is-open", opening);
      elements.menuToggle.setAttribute("aria-expanded", String(opening));
    });

    elements.primaryNav.addEventListener("click", () => {
      elements.primaryNav.classList.remove("is-open");
      elements.menuToggle.setAttribute("aria-expanded", "false");
    });
  }

  function initializeSearch() {
    elements.searchTriggers.forEach(trigger => {
      trigger.addEventListener("click", openSearch);
    });

    elements.searchClose.addEventListener("click", closeSearch);
    elements.searchDialog.addEventListener("click", event => {
      if (event.target === elements.searchDialog) {
        closeSearch();
      }
    });

    elements.globalSearch.addEventListener("input", event => {
      state.searchSelection = -1;
      renderSearchResults(event.target.value);
    });

    elements.globalSearch.addEventListener("keydown", event => {
      const results = [...elements.searchResults.querySelectorAll(".search-result")];
      if (!results.length) {
        return;
      }

      if (event.key === "ArrowDown") {
        event.preventDefault();
        state.searchSelection = Math.min(state.searchSelection + 1, results.length - 1);
        updateSearchSelection(results);
      } else if (event.key === "ArrowUp") {
        event.preventDefault();
        state.searchSelection = Math.max(state.searchSelection - 1, 0);
        updateSearchSelection(results);
      } else if (event.key === "Enter" && state.searchSelection >= 0) {
        event.preventDefault();
        results[state.searchSelection].click();
      }
    });

    document.addEventListener("keydown", event => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        openSearch();
      } else if (event.key === "/" && document.activeElement.tagName !== "INPUT") {
        event.preventDefault();
        openSearch();
      }
    });
  }

  function openSearch() {
    if (!elements.searchDialog.open) {
      elements.searchDialog.showModal();
    }
    elements.globalSearch.value = "";
    state.searchSelection = -1;
    renderSearchResults("");
    requestAnimationFrame(() => elements.globalSearch.focus());
  }

  function closeSearch() {
    if (elements.searchDialog.open) {
      elements.searchDialog.close();
    }
  }

  function renderSearchResults(query) {
    const normalizedQuery = normalize(query);
    const volumeResults = state.volumes.map(volume => ({
      id: volume.id,
      title: volume.title,
      summary: volume.summary,
      url: `docs/${volume.slug}/`,
      type: `Volume ${volume.id}`,
      keywords: [volume.stage, volume.level, ...volume.keywords]
    }));

    const results = [...staticDestinations, ...volumeResults]
      .filter(item => {
        if (!normalizedQuery) {
          return true;
        }
        return normalize([item.title, item.summary, item.type, ...item.keywords].join(" "))
          .includes(normalizedQuery);
      })
      .slice(0, 12);

    if (!results.length) {
      elements.searchResults.innerHTML = `
        <div class="empty-state">
          <h3>No result</h3>
          <p>Try a pattern name, data structure, runtime component, or output format.</p>
        </div>
      `;
      return;
    }

    elements.searchResults.innerHTML = results.map(item => `
      <a class="search-result" href="${escapeHtml(item.url)}">
        <span class="search-result-index">${escapeHtml(item.id)}</span>
        <span>
          <strong>${escapeHtml(item.title)}</strong>
          <small>${escapeHtml(item.summary)}</small>
        </span>
        <span class="search-result-type">${escapeHtml(item.type)}</span>
      </a>
    `).join("");
  }

  function updateSearchSelection(results) {
    results.forEach((result, index) => {
      result.classList.toggle("is-selected", index === state.searchSelection);
    });
    results[state.searchSelection]?.scrollIntoView({ block: "nearest" });
  }

  function initializeCopyButton() {
    elements.copyCommand.addEventListener("click", async () => {
      try {
        await navigator.clipboard.writeText(elements.copyCommand.dataset.copy);
        showToast("Clone command copied");
      } catch {
        showToast("Copy unavailable - select the command manually");
      }
    });
  }

  function initializeReveal() {
    observeReveals(document);
  }

  function observeReveals(container) {
    const targets = [...container.querySelectorAll(".reveal:not(.is-visible)")];
    if (!targets.length) {
      return;
    }

    if (!("IntersectionObserver" in window)) {
      targets.forEach(target => target.classList.add("is-visible"));
      return;
    }

    const observer = new IntersectionObserver(entries => {
      entries.forEach((entry, index) => {
        if (entry.isIntersecting) {
          window.setTimeout(() => entry.target.classList.add("is-visible"), index * 35);
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.08 });

    targets.forEach(target => observer.observe(target));
  }

  let toastTimer;
  function showToast(message) {
    window.clearTimeout(toastTimer);
    elements.toast.textContent = message;
    elements.toast.classList.add("is-visible");
    toastTimer = window.setTimeout(() => {
      elements.toast.classList.remove("is-visible");
    }, 2400);
  }

  function normalize(value) {
    return String(value).trim().toLowerCase();
  }

  function escapeHtml(value) {
    const entities = {
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#039;"
    };
    return String(value).replace(/[&<>"']/g, character => entities[character]);
  }

  function readStorage(key) {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  }

  function writeStorage(key, value) {
    try {
      localStorage.setItem(key, value);
    } catch {
      // Theme persistence is optional.
    }
  }
})();
