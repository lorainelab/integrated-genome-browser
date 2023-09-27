#!/bin/bash

JAVA_OPTS="\
--add-opens java.base/java.net=ALL-UNNAMED \
--add-opens java.base/java.lang.ref=ALL-UNNAMED \
--add-opens java.base/java.lang=ALL-UNNAMED \
--add-opens java.base/java.security=ALL-UNNAMED \
--add-opens java.base/java.util=ALL-UNNAMED \
--add-opens java.base/java.nio=ALL-UNNAMED \
--add-exports java.base/sun.reflect.annotation=ALL-UNNAMED \
--add-opens java.prefs/java.util.prefs=ALL-UNNAMED \
--add-opens java.desktop/javax.swing.plaf.basic=ALL-UNNAMED \
--add-opens java.desktop/javax.swing.text=ALL-UNNAMED \
--add-opens java.desktop/javax.swing=ALL-UNNAMED \
--add-opens java.desktop/java.awt=ALL-UNNAMED \
--add-opens java.desktop/java.awt.event=ALL-UNNAMED \
--add-opens java.desktop/sun.awt.X11=ALL-UNNAMED \
--add-opens java.desktop/javax.swing.plaf.synth=ALL-UNNAMED \
--add-opens java.desktop/com.sun.java.swing.plaf.gtk=ALL-UNNAMED \
--add-opens java.desktop/sun.awt.shell=ALL-UNNAMED \
--add-opens java.desktop/sun.awt.im=ALL-UNNAMED \
--add-exports java.desktop/sun.awt=ALL-UNNAMED \
--add-exports java.desktop/java.awt.peer=ALL-UNNAMED \
--add-exports java.desktop/com.sun.beans.editors=ALL-UNNAMED \
--add-exports java.desktop/sun.swing=ALL-UNNAMED \
--add-exports java.desktop/sun.awt.im=ALL-UNNAMED \
--add-exports java.desktop/com.sun.java.swing.plaf.motif=ALL-UNNAMED \
--add-exports java.desktop/com.apple.eio=ALL-UNNAMED \
--add-opens jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
--add-opens jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
--add-opens jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED \
--add-opens jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
--add-opens jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED \
--add-opens jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED \
--add-opens jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED \
--add-opens jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
--add-opens jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED \
--add-opens jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
--add-opens jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
--add-modules jdk.jshell \
--add-opens jdk.jshell/jdk.jshell=ALL-UNNAMED \
--add-exports jdk.jdeps/com.sun.tools.classfile=ALL-UNNAMED \
--add-exports jdk.jdeps/com.sun.tools.javap=ALL-UNNAMED \
--add-exports jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED \
--add-exports java.management/sun.management=ALL-UNNAMED"

java $JAVA_OPTS -jar igb_exe.jar

