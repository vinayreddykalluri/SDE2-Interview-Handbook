.PHONY: bootstrap install doctor serve serve-web build-site build-book-web build-pdf build-docx build-all sync-book-catalog check-book-catalog validate validate-layout validate-web validate-code validate-deployment verify clean

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

validate:
	$(PYTHON) tooling/automation/validate_repository_layout.py
	$(PYTHON) tooling/automation/validate_structure.py
	$(PYTHON) tooling/automation/validate_links.py
	$(PYTHON) tooling/automation/validate_java_examples.py
	$(PYTHON) tooling/automation/validate_web.py
	$(PYTHON) tooling/automation/validate_deployment.py

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
