
# This is an example of a shell script to run igb from jar files
# on a Unix/Linux system.
# You may need to edit it for your system.

export CLASSPATH="ext/xercesImpl.jar"
export CLASSPATH="$CLASSPATH:ext/xml-apis.jar"
export CLASSPATH="$CLASSPATH:ext/javax.servlet.jar"
export CLASSPATH="$CLASSPATH:ext/org.mortbay.jetty.jar"
export CLASSPATH="$CLASSPATH:ext/commons-logging.jar"
export CLASSPATH="$CLASSPATH:ext/affx_fusion.jar"
export CLASSPATH="$CLASSPATH:ext/jlfgr-1_0.jar"
export CLASSPATH="$CLASSPATH:ext/freehep-base.jar"
export CLASSPATH="$CLASSPATH:ext/freehep-graphics2d.jar"
export CLASSPATH="$CLASSPATH:ext/freehep-graphicsio.jar"
export CLASSPATH="$CLASSPATH:ext/freehep-graphicsio-gif.jar"
export CLASSPATH="$CLASSPATH:ext/freehep-graphicsio-ps.jar"

export CLASSPATH="$CLASSPATH:genoviz.jar"
export CLASSPATH="$CLASSPATH:genometry.jar"
export CLASSPATH="$CLASSPATH:igb.jar"

export MAIN=com.affymetrix.igb.IGB

java -mx256m -classpath $CLASSPATH $MAIN -prefs igb_prefs.xml

# Starting with version 4.0, most users no longer need an igb_prefs.xml file.
# But one is specified here as an example.


