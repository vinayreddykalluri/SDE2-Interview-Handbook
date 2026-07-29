const BOOKS_URL = "content/books.json";

let books = [];
let segments = [];
let bookRelease = null;
let searchIndex = [];
let activeSearchResult = -1;
let activeBookFilter = "all";

const bookList = document.querySelector("#book-grid");
const bookSearch = document.querySelector("#book-search");
const bookSummary = document.querySelector("#book-library-summary");
const bookStartPath = document.querySelector("#book-start-path");
const bookFilterButtons = document.querySelectorAll("[data-book-filter]");
const bookReleaseLink = document.querySelector("#book-release-link");
const masterBookLink = document.querySelector("#master-book-link");
const seriesIndexLink = document.querySelector("#series-index-link");
const footerPdfIndex = document.querySelector("#footer-pdf-index");
const primaryNav = document.querySelector("#primary-nav");
const menuButton = document.querySelector(".menu-toggle");
const searchDialog = document.querySelector("#search-dialog");
const globalSearch = document.querySelector("#global-search");
const searchResults = document.querySelector("#search-results");
const toast = document.querySelector("#toast");

function createElement(tag, className, text) {
  const element = document.createElement(tag);
  if (className) element.className = className;
  if (text !== undefined) element.textContent = text;
  return element;
}

function bookSearchText(book) {
  return [
    book.id,
    book.step,
    book.pathLabel,
    book.segmentCode,
    book.segmentTitle,
    book.track,
    book.title,
    book.shortTitle,
    book.subtitle,
    book.purpose
  ].concat(
    book.outcomes || [],
    (book.chapterPreview || []).map(function (chapter) { return chapter.title; })
  ).join(" ").toLowerCase();
}

function formatCount(value) {
  return Number(value || 0).toLocaleString("en-US");
}

function buildSegmentPath() {
  bookStartPath.replaceChildren();
  segments.forEach(function (segment) {
    const item = createElement("li", "book-path-step");
    const link = createElement("a");
    link.href = segment.startHref;
    link.append(
      createElement("span", "", segment.code + " / " + segment.bookCount + " BOOKS"),
      createElement("strong", "", segment.title),
      createElement("small", "", "Start with " + segment.code + " 01")
    );
    item.append(link);
    bookStartPath.append(item);
  });
}

function buildBookCard(book) {
  const article = createElement("article", "book-card");
  article.id = "book-" + book.id;
  article.dataset.bookSearch = bookSearchText(book);
  article.dataset.bookTrack = book.track;

  const meta = createElement("div", "book-card-meta");
  meta.append(
    createElement("span", "book-step", book.segmentCode),
    createElement("span", "book-pages", "BOOK " + book.segmentPosition + " OF " + book.segmentBookCount + " · " + book.pageCount + " PDF PAGES")
  );

  const title = createElement("h3", "", book.shortTitle);
  const subtitle = createElement("p", "book-subtitle", book.subtitle);
  const trackLabel = book.publicationStatus === "planned"
    ? book.track + " · ROADMAP EDITION"
    : book.track + " · FULL EDITION";
  const track = createElement("p", "book-track", trackLabel);
  const purpose = createElement("p", "book-purpose", book.purpose);
  const webStats = createElement(
    "p",
    "book-web-stats",
    formatCount(book.webDocumentCount) + " web chapters · " +
      formatCount(book.wordCount) + " words · " +
      formatCount(book.codeExampleCount) + " code entries"
  );

  const contents = createElement("details", "book-contents");
  const contentsSummary = createElement("summary", "", "Preview this book");
  const contentsBody = createElement("div", "book-contents-body");
  const chapterList = createElement("ol", "book-chapter-preview");
  (book.chapterPreview || []).forEach(function (chapter) {
    chapterList.append(createElement("li", "", chapter.title));
  });
  contentsBody.append(chapterList);

  if ((book.outcomes || []).length) {
    contentsBody.append(createElement("strong", "book-outcomes-title", "You will be able to"));
    const outcomes = createElement("ul", "book-outcomes");
    book.outcomes.slice(0, 2).forEach(function (outcome) {
      outcomes.append(createElement("li", "", outcome));
    });
    contentsBody.append(outcomes);
  }

  const supporting = createElement("div", "book-related-reading");
  supporting.append(createElement("strong", "", "Included with the web book"));
  const codeLink = createElement("a", "", "Open the code index →");
  codeLink.href = book.codeHref;
  const sourceLink = createElement("a", "", "Review or contribute to the source →");
  sourceLink.href = book.sourceHref;
  supporting.append(codeLink, sourceLink);
  contentsBody.append(supporting);
  contents.append(contentsSummary, contentsBody);

  const actions = createElement("div", "book-card-actions");
  const read = createElement("a", "button button-primary", book.publicationStatus === "planned" ? "Open roadmap" : "Continue on web");
  read.href = book.fullBookHref;
  read.setAttribute("aria-label", "Continue with " + book.shortTitle + " on the web");
  const download = createElement("a", "button button-secondary", "Download PDF");
  download.href = book.pdfHref;
  download.setAttribute("aria-label", "Download " + book.shortTitle + " PDF, " + book.pageCount + " pages");
  actions.append(read, download);

  article.append(meta, track, title, subtitle, purpose, webStats, contents, actions);
  return article;
}

function renderBooks() {
  const query = (bookSearch.value || "").trim().toLowerCase();
  const matches = books.filter(function (book) {
    const matchesQuery = !query || bookSearchText(book).includes(query);
    const matchesTrack = activeBookFilter === "all" || book.track === activeBookFilter;
    return matchesQuery && matchesTrack;
  });

  bookList.replaceChildren();
  if (!matches.length) {
    bookList.append(createElement("div", "empty-state", "No study-path book matches that search."));
  } else {
    matches.forEach(function (book) {
      bookList.append(buildBookCard(book));
    });
  }

  const filtered = query || activeBookFilter !== "all";
  bookSummary.textContent = filtered
    ? matches.length + " of " + books.length + " books match the current segment or search."
    : books.length + " books across three segments. Choose Java, DSA, or System Design, then follow its book order.";
}

function selectBookFilter(filter) {
  activeBookFilter = filter;
  bookFilterButtons.forEach(function (button) {
    const isActive = button.dataset.bookFilter === filter;
    button.classList.toggle("is-active", isActive);
    button.setAttribute("aria-pressed", String(isActive));
  });
  renderBooks();
}

function buildSearchIndex() {
  const fixedDestinations = [
    { type: "Library", label: "Complete Java SDE-2 learning library", note: "Choose Java, DSA, or System Design", href: "books/" },
    { type: "Java", label: "Java Engineering", note: "Begin JAVA 01 with language foundations", href: "books/01-java-foundations-for-problem-solving/" },
    { type: "DSA", label: "Data Structures and Algorithms", note: "Begin DSA 01 with complexity", href: "books/02-time-and-space-complexity/" },
    { type: "System Design", label: "System Design and Backend", note: "Begin SD 01 with backend and design foundations", href: "books/18f-design-backend-testing-and-security/" },
    { type: "Practice", label: "Interview practice", note: "Use question banks, mocks, rubrics, and review logs", href: "docs/backend-interview/10-practice/" },
    { type: "Reference", label: "Topic reference", note: "Look up a concise explanation without changing the study order", href: "docs/" },
    { type: "About", label: "About the curriculum", note: "Understand the web, PDF, and open-source publishing model", href: "#about" },
    { type: "Code", label: "Java implementation indexes", note: "Open code from the relevant web book", href: "books/" }
  ];

  const bookEntries = books.map(function (book) {
    return {
      type: book.segmentCode,
      label: book.shortTitle,
      note: formatCount(book.webDocumentCount) + " web chapters · " + formatCount(book.codeExampleCount) + " code entries · PDF included",
      href: book.fullBookHref,
      keywords: bookSearchText(book)
    };
  });

  searchIndex = fixedDestinations.concat(bookEntries);
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
    searchResults.append(createElement("div", "empty-state", "No matching book or topic."));
    return;
  }

  matches.forEach(function (item, index) {
    const result = createElement("a", "search-result" + (index === activeSearchResult ? " is-active" : ""));
    result.href = item.href;
    result.dataset.searchPosition = String(index);
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

async function initializeBooks() {
  try {
    const response = await fetch(BOOKS_URL);
    if (!response.ok) throw new Error("Book catalog request failed with " + response.status);
    const catalog = await response.json();
    if (!catalog || !Array.isArray(catalog.books) || !Array.isArray(catalog.segments) || !catalog.release) {
      throw new Error("Book catalog has an invalid shape");
    }
    books = catalog.books;
    segments = catalog.segments;
    bookRelease = catalog.release;
    bookReleaseLink.href = bookRelease.url;
    masterBookLink.href = bookRelease.master.pdfHref;
    seriesIndexLink.href = bookRelease.index.pdfHref;
    footerPdfIndex.href = bookRelease.index.pdfHref;
    document.querySelector("[data-book-stat='books']").textContent = books.length + " books";
    document.querySelector("[data-book-stat='pdfs']").textContent = bookRelease.totalPdfCount + " PDFs";
    document.querySelector("[data-book-stat='documents']").textContent =
      formatCount(books.reduce(function (total, book) { return total + Number(book.webDocumentCount || 0); }, 0)) + " chapters";
    document.querySelector("[data-book-stat='code']").textContent =
      formatCount(books.reduce(function (total, book) { return total + Number(book.codeExampleCount || 0); }, 0)) + " code entries";
    buildSegmentPath();
    renderBooks();
    buildSearchIndex();
  } catch (error) {
    const fallback = createElement("div", "empty-state");
    fallback.append(
      document.createTextNode("The study-path catalog could not load. "),
      Object.assign(createElement("a", "", "Open the complete web path."), { href: "books/" })
    );
    bookList.replaceChildren(fallback);
    bookSummary.textContent = "The catalog is temporarily unavailable.";
    console.error(error);
  }
}

bookSearch.addEventListener("input", renderBooks);
bookFilterButtons.forEach(function (button) {
  button.addEventListener("click", function () {
    selectBookFilter(button.dataset.bookFilter || "all");
  });
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

const sectionLinks = Array.from(primaryNav.querySelectorAll('a[href^="#"]'));
const trackedSections = sectionLinks.map(function (link) {
  return document.querySelector(link.getAttribute("href"));
}).filter(Boolean);
let navUpdateScheduled = false;

function updateActiveNavigation() {
  const headerHeight = document.querySelector(".site-header").offsetHeight;
  const probe = window.scrollY + headerHeight + (window.innerHeight * 0.18);
  let activeId = trackedSections[0] ? trackedSections[0].id : "";
  trackedSections.forEach(function (section) {
    if (section.offsetTop <= probe) activeId = section.id;
  });
  sectionLinks.forEach(function (link) {
    link.classList.toggle("is-active", link.getAttribute("href") === "#" + activeId);
  });
  navUpdateScheduled = false;
}

window.addEventListener("scroll", function () {
  if (navUpdateScheduled) return;
  navUpdateScheduled = true;
  window.requestAnimationFrame(updateActiveNavigation);
}, { passive: true });

initializeBooks();
updateActiveNavigation();
