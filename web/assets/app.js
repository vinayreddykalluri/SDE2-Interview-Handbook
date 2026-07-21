const JOURNEY_URL = "content/interview-path.json";
const PROGRESS_KEY = "sde2-interview-journey-progress-v1";
const THEME_KEY = "sde2-handbook-theme";

let stages = [];
let completedStages = new Set(loadStoredArray(PROGRESS_KEY));
let searchIndex = [];
let activeSearchResult = -1;
let allStagesExpanded = false;

const stageList = document.querySelector("#stage-list");
const journeySearch = document.querySelector("#journey-search");
const completedCount = document.querySelector("#completed-count");
const stageCount = document.querySelector("#stage-count");
const progressFill = document.querySelector("#progress-fill");
const continueButton = document.querySelector("#continue-stage");
const expandButton = document.querySelector("#expand-stages");
const resetButton = document.querySelector("#reset-progress");
const primaryNav = document.querySelector("#primary-nav");
const menuButton = document.querySelector(".menu-toggle");
const themeButton = document.querySelector(".theme-toggle");
const searchDialog = document.querySelector("#search-dialog");
const globalSearch = document.querySelector("#global-search");
const searchResults = document.querySelector("#search-results");
const toast = document.querySelector("#toast");

function loadStoredArray(key) {
  try {
    const value = JSON.parse(localStorage.getItem(key) || "[]");
    return Array.isArray(value) ? value : [];
  } catch {
    return [];
  }
}

function createElement(tag, className, text) {
  const element = document.createElement(tag);
  if (className) element.className = className;
  if (text !== undefined) element.textContent = text;
  return element;
}

function persistProgress() {
  try {
    localStorage.setItem(PROGRESS_KEY, JSON.stringify(Array.from(completedStages)));
  } catch {
    showToast("Progress could not be saved");
  }
}

function stageSearchText(stage) {
  return [
    stage.id,
    stage.phase,
    stage.category,
    stage.title,
    stage.summary,
    stage.outcome
  ].concat(
    stage.tasks,
    stage.topics,
    stage.resources.flatMap(function (resource) {
      return [resource.label, resource.note, resource.type];
    })
  ).join(" ").toLowerCase();
}

function buildStageCard(stage, index, firstIncomplete) {
  const isComplete = completedStages.has(stage.id);
  const details = createElement("details", "stage-card");
  details.id = "stage-" + stage.id;
  details.dataset.stageId = stage.id;
  details.dataset.stageSearch = stageSearchText(stage);
  details.style.animationDelay = Math.min(index * 30, 240) + "ms";
  details.open = stage.id === firstIncomplete || (!firstIncomplete && index === 0);

  const summary = createElement("summary");
  summary.append(createElement("span", "stage-index", stage.id));

  const summaryCopy = createElement("span", "stage-summary-copy");
  summaryCopy.append(
    createElement("span", "stage-kicker", stage.phase + " / " + stage.category),
    createElement("strong", "stage-title", stage.title)
  );

  const status = createElement("span", "stage-status" + (isComplete ? " is-complete" : ""), isComplete ? "Complete" : "Ready to start");
  status.dataset.stageStatus = stage.id;

  summary.append(
    summaryCopy,
    createElement("span", "stage-duration", stage.duration),
    status,
    createElement("span", "stage-toggle-icon", "+")
  );

  const content = createElement("div", "stage-content");

  const outcome = createElement("div", "stage-outcome");
  outcome.append(
    createElement("h3", "", "Exit outcome"),
    createElement("p", "", stage.outcome),
    createElement("p", "stage-summary", stage.summary)
  );

  const checklistBlock = createElement("div");
  checklistBlock.append(createElement("h3", "", "Preparation checklist"));
  const checklist = createElement("ol", "stage-checklist");
  stage.tasks.forEach(function (task) {
    checklist.append(createElement("li", "", task));
  });
  checklistBlock.append(checklist);

  const topics = createElement("div", "stage-topics");
  topics.setAttribute("aria-label", "Topics covered");
  stage.topics.forEach(function (topic) {
    topics.append(createElement("span", "", topic));
  });

  const resources = createElement("div", "stage-resources");
  stage.resources.forEach(function (resource) {
    const link = createElement("a", "stage-resource");
    link.href = resource.href;
    link.append(
      createElement("span", "", resource.type),
      createElement("strong", "", resource.label),
      createElement("small", "", resource.note)
    );
    resources.append(link);
  });

  const completionLabel = createElement("label", "stage-complete-control");
  const checkbox = createElement("input");
  checkbox.type = "checkbox";
  checkbox.dataset.completeStage = stage.id;
  checkbox.checked = isComplete;
  completionLabel.append(checkbox, document.createTextNode(" I can produce the exit outcome without notes"));

  content.append(outcome, checklistBlock, topics, resources, completionLabel);
  details.append(summary, content);
  return details;
}

function renderStages() {
  stageList.replaceChildren();

  if (!stages.length) {
    stageList.append(createElement("div", "empty-state", "No journey stages are available."));
    return;
  }

  const firstIncompleteStage = stages.find(function (stage) {
    return !completedStages.has(stage.id);
  });
  const firstIncomplete = firstIncompleteStage ? firstIncompleteStage.id : null;

  stages.forEach(function (stage, index) {
    stageList.append(buildStageCard(stage, index, firstIncomplete));
  });

  updateProgress();
  applyJourneyFilter();
}

function updateProgress() {
  const validIds = new Set(stages.map(function (stage) { return stage.id; }));
  completedStages = new Set(Array.from(completedStages).filter(function (id) {
    return validIds.has(id);
  }));

  const complete = completedStages.size;
  const total = stages.length || 1;
  completedCount.textContent = String(complete);
  stageCount.textContent = String(stages.length);
  progressFill.style.width = Math.round((complete / total) * 100) + "%";

  document.querySelectorAll("[data-stage-status]").forEach(function (element) {
    const isComplete = completedStages.has(element.dataset.stageStatus);
    element.textContent = isComplete ? "Complete" : "Ready to start";
    element.classList.toggle("is-complete", isComplete);
  });

  continueButton.textContent = complete === stages.length ? "Review from start" : "Continue next";
}

function setStageCompletion(stageId, checked) {
  if (checked) {
    completedStages.add(stageId);
  } else {
    completedStages.delete(stageId);
  }
  persistProgress();
  updateProgress();
  showToast(checked ? "Stage marked complete" : "Stage reopened");
}

function applyJourneyFilter() {
  const query = (journeySearch.value || "").trim().toLowerCase();
  let visibleCount = 0;

  document.querySelectorAll(".stage-card").forEach(function (card) {
    const matches = !query || card.dataset.stageSearch.includes(query);
    card.hidden = !matches;
    if (matches) {
      visibleCount += 1;
      if (query) card.open = true;
    }
  });

  const existingEmpty = stageList.querySelector(".empty-state.filter-empty");
  if (!visibleCount && !existingEmpty) {
    const empty = createElement("div", "empty-state filter-empty", "No stage matches that search.");
    stageList.append(empty);
  } else if (visibleCount && existingEmpty) {
    existingEmpty.remove();
  }
}

function findStageCard(stageId) {
  return Array.from(document.querySelectorAll(".stage-card")).find(function (card) {
    return card.dataset.stageId === stageId;
  });
}

function openStage(stageId) {
  journeySearch.value = "";
  applyJourneyFilter();
  const card = findStageCard(stageId);
  if (!card) return;
  card.open = true;
  card.scrollIntoView({ behavior: "smooth", block: "start" });
}

function continueJourney() {
  const next = stages.find(function (stage) {
    return !completedStages.has(stage.id);
  }) || stages[0];
  if (next) openStage(next.id);
}

function toggleAllStages() {
  allStagesExpanded = !allStagesExpanded;
  document.querySelectorAll(".stage-card:not([hidden])").forEach(function (card) {
    card.open = allStagesExpanded;
  });
  expandButton.textContent = allStagesExpanded ? "Collapse all" : "Expand all";
}

function buildSearchIndex() {
  const fixedDestinations = [
    { type: "Start", label: "Readiness matrix", note: "Assess current interview readiness", href: "docs/backend-interview/readiness-matrix/" },
    { type: "Plan", label: "12-week roadmap", note: "Follow the complete preparation schedule", href: "docs/backend-interview/roadmap/" },
    { type: "Practice", label: "Question bank and rubric", note: "Run scored interview simulations", href: "docs/backend-interview/10-practice/question-bank-and-rubric/" },
    { type: "Review", label: "Structured revision system", note: "Retain material through repeated recall", href: "docs/backend-interview/revision-system/" },
    { type: "Code", label: "Java example library", note: "Open the separate runnable source tree", href: "https://github.com/vinayreddykalluri/SDE2-Interview-Handbook/tree/master/examples/java" }
  ];

  const stageEntries = [];
  stages.forEach(function (stage) {
    stageEntries.push({
      type: "Stage " + stage.id,
      label: stage.title,
      note: stage.outcome,
      href: "#stage-" + stage.id,
      stageId: stage.id,
      keywords: stageSearchText(stage)
    });

    stage.resources.forEach(function (resource) {
      stageEntries.push({
        type: resource.type,
        label: resource.label,
        note: resource.note,
        href: resource.href,
        keywords: stage.title + " " + stage.category + " " + stage.topics.join(" ")
      });
    });
  });

  searchIndex = fixedDestinations.concat(stageEntries);
}

function matchedSearchResults(query) {
  const normalized = query.trim().toLowerCase();
  if (!normalized) return searchIndex.slice(0, 8);
  return searchIndex.filter(function (item) {
    return [item.type, item.label, item.note, item.keywords || ""].join(" ").toLowerCase().includes(normalized);
  }).slice(0, 10);
}

function renderGlobalSearch() {
  const matches = matchedSearchResults(globalSearch.value);
  activeSearchResult = matches.length ? 0 : -1;
  searchResults.replaceChildren();

  if (!matches.length) {
    searchResults.append(createElement("div", "empty-state", "No matching stage or resource."));
    return;
  }

  matches.forEach(function (item, index) {
    const result = createElement("a", "search-result" + (index === activeSearchResult ? " is-active" : ""));
    result.href = item.href;
    result.dataset.searchPosition = String(index);
    if (item.stageId) result.dataset.openStage = item.stageId;
    result.append(
      createElement("span", "", item.type),
      createElement("strong", "", item.label),
      createElement("small", "", item.note)
    );
    searchResults.append(result);
  });
}

function moveSearchSelection(direction) {
  const results = Array.from(searchResults.querySelectorAll(".search-result"));
  if (!results.length) return;
  activeSearchResult = (activeSearchResult + direction + results.length) % results.length;
  results.forEach(function (result, index) {
    result.classList.toggle("is-active", index === activeSearchResult);
  });
  results[activeSearchResult].scrollIntoView({ block: "nearest" });
}

function openSearch() {
  renderGlobalSearch();
  if (typeof searchDialog.showModal === "function") {
    searchDialog.showModal();
    requestAnimationFrame(function () { globalSearch.focus(); });
  }
}

function closeSearch() {
  if (searchDialog.open) searchDialog.close();
}

function showToast(message) {
  toast.textContent = message;
  toast.classList.add("is-visible");
  window.clearTimeout(showToast.timeout);
  showToast.timeout = window.setTimeout(function () {
    toast.classList.remove("is-visible");
  }, 1800);
}

function applyTheme(theme) {
  document.documentElement.dataset.theme = theme;
  themeButton.textContent = theme === "dark" ? "Light" : "Dark";
  themeButton.setAttribute("aria-label", "Switch to " + (theme === "dark" ? "light" : "dark") + " theme");
  try {
    localStorage.setItem(THEME_KEY, theme);
  } catch {
    // Theme persistence is optional.
  }
}

function initializeTheme() {
  let storedTheme = null;
  try {
    storedTheme = localStorage.getItem(THEME_KEY);
  } catch {
    // Use system preference.
  }
  const systemTheme = window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  applyTheme(storedTheme || systemTheme);
}

async function initializeJourney() {
  try {
    const response = await fetch(JOURNEY_URL);
    if (!response.ok) throw new Error("Journey request failed with " + response.status);
    stages = await response.json();
    if (!Array.isArray(stages)) throw new Error("Journey data must be an array");
    renderStages();
    buildSearchIndex();
    document.querySelector("[data-stat='stages']").textContent = stages.length + " steps";
  } catch (error) {
    const fallback = createElement("div", "empty-state");
    fallback.append(
      document.createTextNode("The journey map could not load. "),
      Object.assign(createElement("a", "", "Open the Backend SDE-2 handbook."), { href: "docs/backend-interview/" })
    );
    stageList.replaceChildren(fallback);
    console.error(error);
  }
}

stageList.addEventListener("change", function (event) {
  const checkbox = event.target.closest("[data-complete-stage]");
  if (checkbox) setStageCompletion(checkbox.dataset.completeStage, checkbox.checked);
});

journeySearch.addEventListener("input", applyJourneyFilter);
continueButton.addEventListener("click", continueJourney);
expandButton.addEventListener("click", toggleAllStages);

resetButton.addEventListener("click", function () {
  if (!window.confirm("Reset all locally stored journey progress?")) return;
  completedStages.clear();
  persistProgress();
  renderStages();
  showToast("Progress reset");
});

menuButton.addEventListener("click", function () {
  const open = primaryNav.classList.toggle("is-open");
  menuButton.setAttribute("aria-expanded", String(open));
  menuButton.textContent = open ? "Close" : "Menu";
});

primaryNav.addEventListener("click", function () {
  primaryNav.classList.remove("is-open");
  menuButton.setAttribute("aria-expanded", "false");
  menuButton.textContent = "Menu";
});

themeButton.addEventListener("click", function () {
  applyTheme(document.documentElement.dataset.theme === "dark" ? "light" : "dark");
});

document.querySelectorAll(".search-trigger").forEach(function (button) {
  button.addEventListener("click", openSearch);
});

document.querySelector(".dialog-close").addEventListener("click", closeSearch);
globalSearch.addEventListener("input", renderGlobalSearch);

globalSearch.addEventListener("keydown", function (event) {
  if (event.key === "ArrowDown") {
    event.preventDefault();
    moveSearchSelection(1);
  } else if (event.key === "ArrowUp") {
    event.preventDefault();
    moveSearchSelection(-1);
  } else if (event.key === "Enter") {
    event.preventDefault();
    const active = searchResults.querySelector(".search-result.is-active");
    if (active) active.click();
  }
});

searchResults.addEventListener("click", function (event) {
  const result = event.target.closest("[data-open-stage]");
  if (!result) return;
  event.preventDefault();
  closeSearch();
  openStage(result.dataset.openStage);
});

document.addEventListener("keydown", function (event) {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
    event.preventDefault();
    openSearch();
  } else if (event.key === "Escape") {
    closeSearch();
  }
});

document.querySelectorAll("[data-copy]").forEach(function (button) {
  button.addEventListener("click", async function () {
    try {
      await navigator.clipboard.writeText(button.dataset.copy);
      showToast("Clone command copied");
    } catch {
      showToast("Copy failed");
    }
  });
});

const observedSections = Array.from(document.querySelectorAll("main section[id]"));
const sectionLinks = Array.from(primaryNav.querySelectorAll('a[href^="#"]'));

if ("IntersectionObserver" in window) {
  const sectionObserver = new IntersectionObserver(function (entries) {
    const visible = entries.filter(function (entry) {
      return entry.isIntersecting;
    }).sort(function (a, b) {
      return b.intersectionRatio - a.intersectionRatio;
    })[0];

    if (!visible) return;
    sectionLinks.forEach(function (link) {
      link.classList.toggle("is-active", link.getAttribute("href") === "#" + visible.target.id);
    });
  }, { rootMargin: "-25% 0px -60%", threshold: [0.1, 0.4] });

  observedSections.forEach(function (section) {
    sectionObserver.observe(section);
  });
}

initializeTheme();
initializeJourney();
