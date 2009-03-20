
# This is an example of a shell script to run igb from jar files
# on a Unix/Linux system.
# You may need to edit it for your system.

# IGB requires java 1.5 or higher

export JAVA="`which java`"

$JAVA -mx1024m -jar igb_exe.jar

# Starting with version 4.0, most users no longer need an igb_prefs.xml file.
# But one is specified here as an example.

#$JAVA -mx1024m -jar igb_exe.jar -prefs /home/ed/igb_prefs.xml

