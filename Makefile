all: dist/freenet.jar

check-syntax: src
	ant -Dtest.skip=true

check: src build.xml
	ant

dist/freenet.jar: src build.xml
	ant -Dtest.skip=true


