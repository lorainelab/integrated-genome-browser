set TOMCAT_HOME=C:\apache\tomcat5.5
set USER_NAME=%1
set PASSWORD=%2
SET REALM=Das2

java -classpath %TOMCAT_HOME%\server\lib\catalina.jar;%TOMCAT_HOME%\webapps\DAS2\WEB-INF\lib\commons-logging.jar org.apache.catalina.realm.RealmBase -a MD5 %USER_NAME%:%REALM%:%PASSWORD%