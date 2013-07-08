#!/bin/bash

# compile getem with all libraries included in classpath
javac -Xlint:unchecked -classpath .:getem/* getem/*.java

# create jar file with manifest
jar cfm getem.jar Manifest.txt getem/

