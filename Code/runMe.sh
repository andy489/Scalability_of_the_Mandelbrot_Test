#!/bin/bash

_CMD_JAVA=`which java`;

$_CMD_JAVA -Xmx8G -cp ./out/production/:./lib/commons-math3-3.6.1.jar:./lib/commons-cli-1.4.jar ManPar "$@" 


