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
PREFIX=`readlink -f $0 2>/dev/null`

# Not all readlinks support -f.  Ideally, this should run recursively to
# determine its real path.  Currently it only will resolve the first
# link.
if [ $? -ne 0 ]; then
	PREFIX=`readlink $0`
	[ $? -ne 0  ] && PREFIX=$0
fi

PREFIX=`dirname $PREFIX`

# Do our best to find java
test -z "$JAVACMD" -a -n "$JAVA_HOME" && JAVACMD=$JAVA_HOME/bin/java
test -z "$JAVACMD" -a -n "$JRE_HOME"  && JAVACMD=$JRE_HOME/bin/java
test -z "$JAVACMD" && JAVACMD=`which java` 2> /dev/null
test -z "$JAVACMD" && echo "$0: could not find java" >&2 && exit 127

# Find VM arguments 
while (( "$#" )); do # for each command line argument
        echo $1 | grep -q '^-D' # does it start with -D ? 
	if [ $? -eq 0 ]; then # if grep found -D
		VMARGS[${#VMARGS[*]}]=$1 # add to the VM arguments
	else
		ARGS[${#ARGS[*]}]=$1 # add to the ordinary options, arguments
	fi
	shift
done

# For Apple computers
if [[ `uname -s` == "Darwin" ]]; then
        # put the menu at the top of the screen, not on IGB itself
	VMARGS[${#VMARGS[*]}]="-Dapple.laf.useScreenMenuBar=true"
        # display IGB's name at the top of the screen, not "main"
	VMARGS[${#VMARGS[*]}]="-Xdock:name=Integrated Genome Browser"
        # assume 64 bit environment
	VMARGS[${#VMARGS[*]}]="-d64"
fi

# Launch IGB
IFS="
"
$JAVACMD ${VMARGS[*]} -jar $PREFIX/igb_exe.jar ${ARGS[*]}
