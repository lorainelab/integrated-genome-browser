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

IGB uses other open source software packages that are not distributed 
with the IGB source code release, including Xerces from Apache, 
and Jetty from Mortbay Consulting, which are covered by their own 
open source licenses.

IGB is Copyright (c) 2000-2005 Affymetrix, Inc.  
Research and development of IGB is supported in part by NIH grant R01HG003040.

GenoViz is hosted on SourceForge.net at 
http://sourceforge.net/projects/genoviz


----------------- DOCUMENTATION ----------------------------

An IGB user's manual is located in the 'documentation' directory.

There are demos and tutorials for the GenoViz code inside
the genoviz_sdk directory.  These are described later below.

Javadocs can also be created from the java code using the
provided 'ant' scripts.


----------------- REQUIRED RESOURCES -----------------------

IGB requires the Java 2 Platform, Standard Edition (J2SE), 
  version 1.4.2 or higher.
  http://java.sun.com/j2se/


IGB requires several additional resources.  Download the following
java 'jar' files and place them in the 'lib' directory of the 
IGB distribution:

jetty.jar  
  Version 4.1 or higher
  http://www.jettyserver.org/jetty/index.html

servlet.jar
  Version 2.2 or higher
  http://java.sun.com/products/servlet/2.2/

xerces.jar
  Version 1.3 or higher, but NOT Version 2.0 or higher.
  http://xml.apache.org/xerces-j/


(If you choose to use Apache Ant, you can skip the xerces.jar file and instead
make use of the xercesImpl.jar and xml-apis.jar files included with Ant.)


----------------- OPTIONAL RESOURCES -----------------------

This distribution contains a build.xml file which can be used along with 
'Apache Ant' to compile and run IGB.

It is possible to compile and run IGB in other ways, such as in an IDE.
If you wish to use the provided 'build.xml' file, you will need to obtain
and install Apache Ant from http://ant.apache.org

The provided build.xml file was tested with Apache Ant version 1.5.2.


----------------- COMPILING IGB ----------------------------

Download the jar files listed above in REQUIRED RESOURCES.
Place them together in any directory.

If you wish to use 'ant':

You will have to let ant know the name of the directory where
you placed the third-party jar files.  To do this, add the
flag '-Dlib.dir=...'.  For example, if the files are located in
'/home/gregg/lib', then use:
 
'ant -Dlib.dir=/home/gregg/lib jar' 
  to compile the source and create the igb.jar file.

'ant -Dlib.dir=/home/gregg/lib docs' 
  to create javadocs documentation into the 'api-docs' folder.

'ant -Dlib.dir=/home/gregg/lib clean' 
  to remove compiled classes and documentation.

The compiled code will be placed in the same directory structure 
as the java code, such that each *.class file will be placed
near the corresponding *.java file.


If you do not wish to use 'ant':

Use any IDE to compile the java sources in these
three directories, in this order:
1)  genoviz_sdk/src
2)  genometry/src
3)  igb/src


----------------- RUNNING IGB -----------------------------

You can run IGB from within ant or from the command line.
The output is easier to read if you run it from the 
command line.

Note that the following commands make use of the preferences
file in igb_src/igb_prefs.xml.  If you have your own 
preferences file(s), edit the commands below.

Running from the command line. Sample command:

java -Xmx512m -classpath lib/xercesImpl.jar:lib/xml-apis.jar:lib/servlet.jar:lib/jetty.jar:genometry/genometry.jar:genoviz_sdk/genoviz.jar:igb.jar com.affymetrix.igb.IGB -prefs igb_src/igb_prefs.xml

This sample command assumes that:

1) the genoviz.jar and genometry.jar are located in the genoviz_sdk
   and genometry subdirectories, respectively, relative to the
   top-level IGB release directory (this happens automatically
   when building via the ant 'jar' task),
2) all third-party required jars are in a subdirectory called 'lib'
   relative to the top-level IGB release directory, and
3) it is executed in the top-level IGB release directory.


Running with ant:

Use the ant task 'run' to run the program from within ant.
You must let ant know the location of the external
resources.  For example, if the files are located in
'/home/gregg/lib', then use:
 
'ant -Dlib.dir=/home/gregg/lib run'


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



