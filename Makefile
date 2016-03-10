all: dist/

check-syntax: src
	ant

check: src build.xml
	ant

dist/: src build.xml build.txt
	ant dist


