#!/bin/bash

_CMD_JAVAC=`which javac`

$_CMD_JAVAC -cp ./out/production/:./lib/commons-cli-1.4.jar:./lib/commons-math3-3.6.1.jar -d ./out/production/ src/*.java
