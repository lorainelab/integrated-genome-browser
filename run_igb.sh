
# This is an example of a shell script to run igb from jar files
# on a Unix/Linux system.
# You may need to edit it for your system.

# IGB requires java 1.5 or higher

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

if [ -f genoviz.jar ]; then
  export CLASSPATH="$CLASSPATH:genoviz.jar"
else
  export CLASSPATH="$CLASSPATH:genoviz_sdk/genoviz.jar"
fi
if [ -f genometry.jar ]; then
  export CLASSPATH="$CLASSPATH:genometry.jar"
else
  export CLASSPATH="$CLASSPATH:genometry/genometry.jar"
fi
if [ -f genometryImpl.jar ]; then
  export CLASSPATH="$CLASSPATH:genometryImpl.jar"
else
  export CLASSPATH="$CLASSPATH:genometryImpl/genometryImpl.jar"
fi
export CLASSPATH="$CLASSPATH:igb.jar"

#export CLASSPATH="$CLASSPATH:/localhome/liquid_lnf/LiquidLnF.jar"
#export VMARGS="-Dswing.defaultlaf=com.birosoft.liquid.LiquidLookAndFeel"

#export CLASSPATH="$CLASSPATH:/localhome/napkinlaf.jar"
#export VMARGS="-Dswing.defaultlaf=net.sourceforge.napkinlaf.NapkinLookAndFeel"

export MAIN=com.affymetrix.igb.IGB

export JAVA="/nfs/linux/pkg/java_pkg/jdk1.5/bin/java"

$JAVA -mx256m  $VMARGS -classpath $CLASSPATH $MAIN -prefs igb_prefs.xml

# Starting with version 4.0, most users no longer need an igb_prefs.xml file.
# But one is specified here as an example.


