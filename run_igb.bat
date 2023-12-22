@echo off

REM Set JAVA_OPTS for debug mode
set JAVA_OPTS=^
--add-opens=java.desktop/sun.swing=ALL-UNNAMED ^
--add-opens=java.desktop/sun.awt.shell=ALL-UNNAMED ^
--add-opens=java.desktop/java.awt.event=ALL-UNNAMED ^
--add-opens=java.desktop/sun.awt.windows=ALL-UNNAMED ^
--add-opens=java.desktop/sun.java2d=ALL-UNNAMED ^
--add-opens=java.desktop/sun.font=ALL-UNNAMED ^
--add-opens=java.desktop/java.awt=ALL-UNNAMED ^
--add-opens=java.desktop/sun.awt=ALL-UNNAMED ^
--add-opens=java.base/java.lang=ALL-UNNAMED ^
--add-opens=java.base/java.util=ALL-UNNAMED ^
--add-opens=java.desktop/javax.swing=ALL-UNNAMED ^
--add-opens=java.desktop/sun.swing=ALL-UNNAMED ^
--add-opens=java.desktop/javax.swing.plaf.basic=ALL-UNNAMED ^
--add-opens=java.desktop/java.awt.peer=ALL-UNNAMED ^
--add-opens=java.desktop/javax.swing.text.html=ALL-UNNAMED ^
--add-exports=java.desktop/sun.font=ALL-UNNAMED ^
--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED ^
--add-exports=java.desktop/com.apple.laf=ALL-UNNAMED ^
--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED ^
--add-exports java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED ^
--add-exports java.desktop/com.apple.laf=ALL-UNNAMED ^
--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED ^
--add-opens java.base/java.net=ALL-UNNAMED

REM Check for the debug flag
set DEBUG_FLAG=
for %%i in (%*) do (
    if "%%i"=="-d" (
        set DEBUG_FLAG=true
    ) else if "%%i"=="--debug" (
        set DEBUG_FLAG=true
    )
)

REM If the debug flag is set, add debug options to JAVA_OPTS
if defined DEBUG_FLAG (
    set "JAVA_OPTS=%JAVA_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=127.0.0.1:5005"
)

REM Run the Java program with JAVA_OPTS
java %JAVA_OPTS% -jar igb_exe.jar
