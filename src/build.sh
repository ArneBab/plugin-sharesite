#!/bin/sh

# It is assumed the Java Compiler from OpenJDK 7 is used.
# The produced jar file will run on both Java 6 and Java 7.

rm plugins/Sharesite/*.class plugins/Sharesite/*/*.class
rm Sharesite*.jar

javac -cp ../freenet-ext.jar:../freenet.jar -source 1.6 -target 1.6 plugins/Sharesite/*.java plugins/Sharesite/*/*.java
zip -r Sharesite.jar *
