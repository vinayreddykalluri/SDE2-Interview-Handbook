.PHONY: bootstrap install doctor serve serve-web build-site build-pdf build-docx build-all validate validate-layout validate-web validate-code validate-deployment verify clean

SYSTEM_PYTHON ?= python3
PYTHON ?= .venv/bin/python

bootstrap:
	bash scripts/bootstrap_macos.sh

install:
	test -x "$(PYTHON)" || $(SYSTEM_PYTHON) -m venv .venv
	$(PYTHON) -m pip install --upgrade pip
	$(PYTHON) -m pip install -r requirements.txt

doctor:
	$(SYSTEM_PYTHON) scripts/check_local_environment.py

serve:
	$(PYTHON) -m mkdocs serve

serve-web: build-site
	$(PYTHON) -m http.server 8000 --directory site

build-site:
	$(PYTHON) scripts/build_site.py

build-pdf:
	$(PYTHON) scripts/build_pdf.py

build-docx:
	$(PYTHON) scripts/build_docx.py

build-all:
	$(PYTHON) scripts/build_all.py

validate:
	$(PYTHON) scripts/validate_repository_layout.py
	$(PYTHON) scripts/validate_structure.py
	$(PYTHON) scripts/validate_links.py
	$(PYTHON) scripts/validate_java_examples.py
	$(PYTHON) scripts/validate_web.py
	$(PYTHON) scripts/validate_deployment.py

validate-layout:
	$(PYTHON) scripts/validate_repository_layout.py

validate-web:
	$(PYTHON) scripts/validate_web.py

validate-code:
	$(PYTHON) scripts/validate_java_examples.py

validate-deployment:
	$(PYTHON) scripts/validate_deployment.py

verify: validate build-site

clean:
	rm -rf site output/pdf/*.pdf output/docx/*.docx output/combined/*.pdf output/combined/*.docx
