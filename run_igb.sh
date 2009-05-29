#!/bin/bash
#
# $Id$
#
# Simple bash launcher for IGB.  This launcher should be compatible with
# all UNIX-like systems with a bash interperter.
#
# Ways to Specify Java:
#  - Set JAVA_HOME to point to an installed JDK
#  - Set JRE_HOME to point to an installed JRE
#  - Set JAVACMD to point to a working java executable
#  - Have a java executable in your path
#
# Specifying options to the underlying VM.
#  - Specify on the command line using -J<vmarg>
#
# Specifying options to IGB
#  - Specify directly on command line

# Find out where we are installed
PREFIX=`readlink -f $0`
PREFIX=`dirname $PREFIX`

# File Locations
JARDIR="$PREFIX/dist"
LIBDIR="$PREFIX/ext"
ICON="$PREFIX/igb/resources/com/affymetrix/igb/igb.gif"

# Do our best to find java
test -z "$JAVACMD" -a -n "$JAVA_HOME" && JAVACMD=$JAVA_HOME/bin/java
test -z "$JAVACMD" -a -n "$JRE_HOME"  && JAVACMD=$JRE_HOME/bin/java
test -z "$JAVACMD" && JAVACMD=`which java` 2> /dev/null
test -z "$JAVACMD" && echo "$0: could not find java" >&2 && exit 127

# Sort VM arguments from program arguments
while (( "$#" )); do
	echo $1 | grep -q '^-J'
	if [ $? -eq 0 ]; then
		VMARGS[${#VMARGS[*]}]=`echo $1 | sed -e 's/^-J//'`
	else
		ARGS[${#ARGS[*]}]=$1
	fi
	shift
done

# Use 1GiB RAM per default 
echo $VMARGS[*] | grep -q '\-Xmx\|\-mx' || VMARGS[${#VMARGS[*]}]="-Xmx1024m"

# Some Apple Specific settings. Should there be a way to override these?
if [[ `uname -s` == "Darwin" ]]; then
	VMARGS[${#VMARGS[*]}]="-Dapple.laf.useScreenMenuBar=true"
	# TODO: Allow spaces in program name
	VMARGS[${#VMARGS[*]}]="-Xdock:name=Integrated Genome Browser"
	VMARGS[${#VMARGS[*]}]="-Xdock:icon=$ICON"
fi

# Launch IGB
IFS="
"
$JAVACMD ${VMARGS[*]} -jar $PREFIX/igb_exe.jar ${ARGS[*]}
