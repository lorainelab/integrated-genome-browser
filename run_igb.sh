#!/bin/bash
#
# $Id$
#
# Simple bash launcher for IGB.  This launcher should be compatible with
# all UNIX-like systems with bash 2.0 and above.
#
# Ways to Specify Java:
#  - Set JAVA_HOME to point to an installed JDK
#  - Set JRE_HOME to point to an installed JRE
#  - Set JAVACMD to point to a working java executable
#  - Have a java executable in your path
#
# Specifying options to the underlying VM.
#  - Put options in VMARGS variable (app specific)
#  - Specify on the command line using -J<vmarg>
#
# Specifying options to IGB
#  - Specify directly on command line


# Do our best to find java
if [ -z "$JAVACMD" ]; then
	if [ -n "$JAVA_HOME" ]; then
		JAVACMD=$JAVA_HOME/bin/java
	elif [ -n "$JRE_HOME" ]; then
		JAVACMD=$JRE_HOME/bin/java
	else
		JAVACMD=`which java`
	fi
fi

if [ -z "$JAVACMD" ]; then
	echo "Error: could not find java"
	exit 127
fi

# Sort VM arguments from program arguments
while (( "$#" )); do
	echo $1 | grep -q '^-J'
	if [ $? -eq 0 ]; then
		ARG=`echo $1 | sed -e 's/^-J//'`
		VMARGS="$VMARGS $ARG"
	else
		ARGS="$ARGS $1"
	fi
	shift
done

# Find out where the jar files reside
DIR=`readlink -f $0`
DIR=`dirname $DIR`

# Use 1GiB RAM per default 
echo $VMARGS | grep -q '\-Xmx\|\-mx' || VMARGS="$VMARGS -Xmx1024m"

# Some Apple Specific settings. Should there be a way to override these?
if [[ `uname -s` == "Darwin" ]]; then
	VMARGS="$VMARGS -Dapple.laf.useScreenMenuBar=true"
	# TODO: Allow spaces in program name
	VMARGS="$VMARGS -Xdock:name=IGB"
	VMARGS="$VMARGS -Xdock:icon=$DIR/igb/resources/com/affymetrix/igb/affychip.gif"
fi

# Launch IGB
echo $JAVACMD $VMARGS -jar $DIR/igb_exe.jar $ARGS
$JAVACMD $VMARGS -jar $DIR/igb_exe.jar $ARGS
