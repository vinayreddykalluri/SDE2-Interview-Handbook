.PHONY: install serve build-site build-pdf build-docx build-all validate clean

PYTHON := python

install:
	$(PYTHON) -m pip install -r requirements.txt

serve:
	mkdocs serve

build-site:
	mkdocs build --clean

build-pdf:
	$(PYTHON) scripts/build_pdf.py

build-docx:
	$(PYTHON) scripts/build_docx.py

build-all:
	$(PYTHON) scripts/build_all.py

validate:
	$(PYTHON) scripts/validate_structure.py
	$(PYTHON) scripts/validate_links.py

clean:
	rm -rf site output/pdf/*.pdf output/docx/*.docx output/combined/*.pdf output/combined/*.docx
