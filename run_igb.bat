REM This is an EXAMPLE of a shell script to run igb from jar files
REM on a Windows system.

REM You may need to edit it for your system.



REM This file assumes you have the 'java' program in your PATH.
REM If not, see http://java.sun.com/j2se/1.4.2/install-windows.html
REM for instructions on setting the PATH variable.


REM The genoviz.jar and genometry.jar files are stored in different
REM places in the jar-file distribution and the source-file distribution.
REM Both possible paths are listed below.

set CLASSPATH=%CLASSPATH%;ext\xercesImpl.jar
set CLASSPATH=%CLASSPATH%;ext\xml-apis.jar
set CLASSPATH=%CLASSPATH%;ext\javax.servlet.jar
set CLASSPATH=%CLASSPATH%;ext\org.mortbay.jetty.jar
set CLASSPATH=%CLASSPATH%;ext\commons-logging.jar
set CLASSPATH=%CLASSPATH%;ext\affx_fusion.jar
set CLASSPATH=%CLASSPATH%;ext\jlfgr-1_0.jar
set CLASSPATH=%CLASSPATH%;genoviz_sdk\genoviz.jar
set CLASSPATH=%CLASSPATH%;genoviz.jar
set CLASSPATH=%CLASSPATH%;genometry\genometry.jar
set CLASSPATH=%CLASSPATH%;genometry.jar
set CLASSPATH=%CLASSPATH%;igb.jar
set CLASSPATH=%CLASSPATH%;ext\freehep-base.jar
set CLASSPATH=%CLASSPATH%;ext\freehep-graphics2d.jar
set CLASSPATH=%CLASSPATH%;ext\freehep-graphicsio.jar
set CLASSPATH=%CLASSPATH%;ext\freehep-graphicsio-gif.jar
set CLASSPATH=%CLASSPATH%;ext\freehep-graphicsio-ps.jar


set MAIN=com.affymetrix.igb.IGB
java -mx256m -classpath "%CLASSPATH%" "%MAIN%"

