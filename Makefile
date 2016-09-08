SOURCES:=$(shell find src/ -name *.java) $(shell find src/ -name *.l10n)

all: dist/

check-syntax: src
	ant

check: src build.xml
	ant

dist/: src build.xml build.txt $(SOURCES)
	ant dist


