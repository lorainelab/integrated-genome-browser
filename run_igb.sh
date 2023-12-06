#!/bin/bash

JAVA_OPTS="\
--add-opens=java.desktop/java.awt.event=ALL-UNNAMED \
--add-opens=java.desktop/sun.font=ALL-UNNAMED \
--add-opens=java.desktop/java.awt=ALL-UNNAMED \
--add-opens=java.desktop/sun.awt=ALL-UNNAMED \
--add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.base/java.util=ALL-UNNAMED \
--add-opens=java.desktop/javax.swing=ALL-UNNAMED \
--add-opens=java.desktop/sun.swing=ALL-UNNAMED \
--add-opens=java.desktop/javax.swing.plaf.basic=ALL-UNNAMED \
--add-opens=java.desktop/java.awt.peer=ALL-UNNAMED \
--add-opens=java.desktop/javax.swing.text.html=ALL-UNNAMED \
--add-exports=java.desktop/sun.font=ALL-UNNAMED \
--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED \
--add-exports=java.desktop/com.apple.laf=ALL-UNNAMED \
--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED \
--add-exports java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED \
--add-exports java.desktop/com.apple.laf=ALL-UNNAMED \
--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED \
--add-opens java.base/java.net=ALL-UNNAMED"

# Check if the script was passed the -d or --debug argument
if [[ $* == *-d* ]] || [[ $* == *--debug* ]]; then
  # Enable debugging with the specified port and suspend
  JAVA_OPTS="$JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=127.0.0.1:5005"
fi

java $JAVA_OPTS -jar igb_exe.jar
