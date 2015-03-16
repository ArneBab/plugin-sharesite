#!/bin/sh

# It is assumed the Java Compiler from OpenJDK 7 is used.
# The produced jar file will run on both Java 6 and Java 7.

rm plugins/ShareWiki/*.class plugins/ShareWiki/*/*.class
rm ShareWiki*.jar

javac -cp ../freenet-ext.jar:../freenet.jar -source 1.6 -target 1.6 plugins/ShareWiki/*.java plugins/ShareWiki/*/*.java
zip -r ShareWiki.jar *
