.PHONY: bootstrap install doctor serve serve-web build-site build-book-web build-books build-pdf build-docx build-all sync-book-catalog check-book-catalog validate validate-all validate-layout validate-web validate-code validate-pdfs validate-deployment verify clean

BOOK_DIR := books/java-sde2-interview-preparation-series

SYSTEM_PYTHON ?= python3
PYTHON ?= .venv/bin/python

bootstrap:
	bash tooling/automation/bootstrap_macos.sh

install:
	test -x "$(PYTHON)" || $(SYSTEM_PYTHON) -m venv .venv
	$(PYTHON) -m pip install --upgrade pip
	$(PYTHON) -m pip install -r tooling/requirements/authoring.txt

doctor:
	$(SYSTEM_PYTHON) tooling/automation/check_local_environment.py

serve:
	$(PYTHON) -m mkdocs serve

serve-web: build-site
	$(PYTHON) -m http.server 8000 --directory site

build-site:
	$(PYTHON) tooling/automation/build_site.py

build-book-web:
	$(PYTHON) tooling/automation/build_book_web_library.py --site-dir site/books

# Full series rebuild: every focused volume, then the index (which reads
# dist/manifest.json and must run last), then the master book. Resumable --
# rerun after an interruption and it picks up where it stopped.
build-books:
	cd $(BOOK_DIR) && $(CURDIR)/$(PYTHON) scripts/build_all_volumes.py
	cd $(BOOK_DIR) && $(CURDIR)/$(PYTHON) scripts/build_series.py --index-only
	cd $(BOOK_DIR) && $(CURDIR)/$(PYTHON) scripts/build_book.py --only all
	$(MAKE) validate-pdfs

sync-book-catalog:
	$(PYTHON) tooling/automation/sync_book_catalog.py

check-book-catalog:
	$(PYTHON) tooling/automation/sync_book_catalog.py --check

build-pdf:
	$(PYTHON) tooling/automation/build_pdf.py

build-docx:
	$(PYTHON) tooling/automation/build_docx.py

build-all:
	$(PYTHON) tooling/automation/build_all.py

# `validate` is the contributor-facing target and must run without a JDK.
# Java validation needs javac, and hard-failing here meant anyone without a
# JDK installed could not run the documented pre-PR command at all. Skip it
# with a loud warning instead; `validate-all` is the strict path CI uses.
validate:
	$(PYTHON) tooling/automation/validate_repository_layout.py
	$(PYTHON) tooling/automation/validate_structure.py
	$(PYTHON) tooling/automation/validate_links.py
	@command -v javac >/dev/null 2>&1 \
		&& $(PYTHON) tooling/automation/validate_java_examples.py \
		|| echo "WARNING: javac not found - skipping Java example validation. Install JDK 21 and run 'make validate-all' before opening a PR."
	$(PYTHON) tooling/automation/validate_web.py
	$(PYTHON) tooling/automation/validate_deployment.py

validate-all:
	$(PYTHON) tooling/automation/validate_repository_layout.py
	$(PYTHON) tooling/automation/validate_structure.py
	$(PYTHON) tooling/automation/validate_links.py
	$(PYTHON) tooling/automation/validate_java_examples.py
	$(PYTHON) tooling/automation/validate_web.py
	$(PYTHON) tooling/automation/validate_deployment.py
	$(MAKE) validate-pdfs

validate-pdfs:
	cd $(BOOK_DIR) && $(CURDIR)/$(PYTHON) scripts/validate_pdfs.py --quick

validate-layout:
	$(PYTHON) tooling/automation/validate_repository_layout.py

validate-web:
	$(PYTHON) tooling/automation/validate_web.py

validate-code:
	$(PYTHON) tooling/automation/validate_java_examples.py

validate-deployment:
	$(PYTHON) tooling/automation/validate_deployment.py

verify: validate build-site

clean:
	rm -rf site output/pdf/*.pdf output/docx/*.docx output/combined/*.pdf output/combined/*.docx
