#!/bin/bash

JAVA_OPTS="\
--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED \
--add-opens java.base/java.net=ALL-UNNAMED" 

# Update the java command
#java $JAVA_OPTS --module-path $MODULE_PATH_ARG --add-modules javafx.controls,javafx.graphics,javafx.media,javafx.swing,javafx.fxml,javafx.web -jar igb_exe.jar

java $JAVA_OPTS -jar igb_exe.jar
#java $JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=127.0.0.1:5005 -jar igb_exe.jar