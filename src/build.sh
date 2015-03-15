#!/bin/sh

# It is assumed the Java Compiler from OpenJDK 7 is used.
# The produced jar file will run on both Java 6 and Java 7.

rm plugins/ShareLink/*.class plugins/ShareLink/*/*.class
rm ShareLink*.jar

javac -cp ../freenet-ext.jar:../freenet.jar -source 1.6 -target 1.6 plugins/ShareLink/*.java plugins/ShareLink/*/*.java
zip -r ShareLink.jar *
