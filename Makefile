.PHONY: install serve serve-web build-site build-pdf build-docx build-all validate validate-layout validate-web validate-code verify clean

PYTHON ?= python3

install:
	$(PYTHON) -m pip install -r requirements.txt

serve:
	mkdocs serve

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

validate-layout:
	$(PYTHON) scripts/validate_repository_layout.py

validate-web:
	$(PYTHON) scripts/validate_web.py

validate-code:
	$(PYTHON) scripts/validate_java_examples.py

verify: validate build-site

clean:
	rm -rf site output/pdf/*.pdf output/docx/*.docx output/combined/*.pdf output/combined/*.docx
