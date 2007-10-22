----------------- GENOVIZ ----------------------------

The GenoViz project includes three components:
  GenoViz Graphical SDK
  Genometry
  IGB: Integrated Genome Browser

The GenoViz SDK provides re-usable components for genomics 
data visualization.

Genometry provides a unified data model to represent reltionships 
between biological sequences.

The Integrated Genome Browser (IGB, pronounced ig-bee) is an application 
intended for visualization and exploration of genomes and corresponding 
annotations from multiple data sources.

This source code is released under the Common Public License, v1.0, 
an OSI approved open source license.

See http://www.affymetrix.com/support/developer/tools/igbsource_terms.affx

IGB uses other open source software packages that may be distributed 
with the IGB source code release, including Xerces from Apache, 
Jetty from Mortbay Consulting, Vector Graphics from FreeHEP
and Fusion from Affymetrix.  
Those packages are covered by their own open source licenses.

IGB is Copyright (c) 2000-2007 Affymetrix, Inc.  
Research and development of IGB is supported in part by NIH grant R01HG003040.

GenoViz is hosted on SourceForge.net at 
http://sourceforge.net/projects/genoviz


----------------- DOCUMENTATION ----------------------------

An IGB user's manual is located in the 'documentation' directory
of the source-file release.  It is not distributed with the
jar-file release, but is available here:
http://www.affymetrix.com/support/developer/tools/IGB_User_Guide.pdf

There are demos and tutorials for the GenoViz code inside
the genoviz_sdk directory.  These are described later below.

Javadocs can also be created from the java code using the
provided 'ant' scripts.

See also
http://sourceforge.net/docman/?group_id=129420

----------------- REQUIRED RESOURCES -----------------------

IGB requires the Java 2 Platform, Standard Edition (J2SE), 
  version 1.5 or higher.
  http://java.sun.com/j2se/


IGB requires several additional resources.  These are included 
in the 'ext' directory, and you do not need to download them
separately.

org.mortbay.jetty.jar  
  Version 4.1 or higher
  http://www.jettyserver.org/jetty/index.html

servlet.jar
  Version 2.2 or higher
  http://java.sun.com/products/servlet/2.2/

xerces.jar
(or xml-apis.jar, xercesImpl.jar and commons-logging.jar)
  Version 1.3 or higher, but NOT Version 2.0 or higher.
  http://xml.apache.org/xerces-j/

freehep-*.jar
  Several files from the FreeHEP Vector Graphics library
  which allow the printing of EPS files.

----------------- OPTIONAL RESOURCES -----------------------

This distribution contains a build.xml file which can be used along with 
'Apache Ant' to compile and run IGB.

It is possible to compile and run IGB in other ways, such as in an IDE.
If you wish to use the provided 'build.xml' file, you will need to obtain
and install Apache Ant from http://ant.apache.org

The provided build.xml file was tested with Apache Ant version 1.5.2.


This distribution may include the Java look and feel Graphics Repository
Copyright 1994-2006 Sun Microsystems, Inc.
http://java.sun.com/developer/techDocs/hi/repository/

The file, if present, will be named jlfgr-1_0.jar, in the "ext" directory.
The license file is included in the "ext" directory.
IGB can be used with or without this jar file.


----------------- COMPILING IGB ----------------------------


If you wish to use 'ant':
 
'ant jar' 
  to compile the source and create the three jar files:
  igb.jar genoviz_sdk/genoviz.jar and genometry/genometry.jar

'ant docs' 
  to create javadocs documentation into the 'api-docs' folder.

'ant clean' 
  to remove compiled classes and documentation.

The compiled code will be placed in the same directory structure 
as the java code, such that each *.class file will be placed
near the corresponding *.java file.


If you do not wish to use 'ant':

Use any IDE to compile the java sources in these
directories, in this order:
1)  genoviz_sdk/src
2)  genometry/src
3)  genometryImpl/src
4)  igb/src


----------------- RUNNING IGB -----------------------------

You can run IGB from within ant or from the command line.
The output is easier to read if you run it from the 
command line.

Note that the following commands make use of the preferences
file in igb/src/igb_prefs.xml.  If you have your own 
preferences file(s), edit the commands below.
(Few users need an igb_prefs.xml file with IGB 4.02 or higher.)


Running with ant:

Use the ant task 'run' to run the program from within ant.
 
'ant run'


Running from the command line. Sample command:

java -Xmx512m -classpath ext/xercesImpl.jar:ext/xml-apis.jar:ext/javax.servlet.jar:ext/org.mortbay.jetty.jar:ext/commons-logging.jar:genometry/genometry.jar:genometryImpl/genometryImpl.jar:genoviz_sdk/genoviz.jar:igb.jar com.affymetrix.igb.IGB -prefs igb/src/igb_prefs.xml

This sample command assumes that:

1) genoviz.jar, genometry.jar, genometryImpl.jar are located in the genoviz_sdk,
   genometryImpl and genometry subdirectories, respectively, relative to the
   top-level IGB release directory (this happens automatically
   when building via the ant 'jar' task),
2) all third-party required jars are in a subdirectory called 'ext'
   relative to the top-level IGB release directory, and
3) it is executed in the top-level IGB release directory.



----------------- RUNNING GENOVIZ TUTORIALS ------------------

IGB uses the GenoViz for much of the graphical interface.
To learn the basics of the GenoViz, there are tutorials and
demo's inside the genoviz_sdk directory.

The demo code is not compiled by the main ant build.xml file
mentioned above.

To use the demos, move into the genoviz_sdk directory and type
either 'make' (to build the code with the Makefile) 
or 'ant all' (to build the code with ant).

Then use your web browser to go to the file
genoviz_sdk/index.html

Run 'ant clean' or 'make clean' when finished viewing
the demos, to remove compiled code.



