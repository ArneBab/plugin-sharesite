SOURCES:=$(shell find src/ -name *.java) $(shell find src/ -name *.l10n)

all: dist/.dist-updated

check-syntax: src
	ant

check: src build.xml
	ant

dist/.dist-updated: build.xml build.txt $(SOURCES)
	@mkdir -p dist/
	ant clean && ant dist && touch $@


