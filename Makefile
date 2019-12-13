SOURCES:=$(shell find src/ -name *.java) $(shell find src/ -name *.l10n)

all: dist/.dist-updated

check-syntax: $(SOURCES)
	ant

check: build.xml $(SOURCES)
	ant

dist/.dist-updated: build.xml build.txt $(SOURCES)
	@mkdir -p dist/
	ant clean && ant dist && touch $@


