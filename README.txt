
----------------- GENOVIZ ----------------------------

The GenoViz project includes:

  GenoViz Graphical SDK (genoviz_sdk)
  Genometry Genomics Data models (genometryImpl)
  IGB: Integrated Genome Browser (igb, plugins, common, igbcommander)
  DAS2 Classic and GenoPub (das2_server)

The GenoViz SDK provides re-usable components for genomics 
data visualization.

Genometry provides a unified data model representing relationships 
between biological sequences, annotations, etc.

The Integrated Genome Browser (IGB, pronounced ig-bee) is an application 
intended for visualization and exploration of genomes and corresponding 
annotations from multiple data sources.

This source code is released under the Common Public License, v1.0, 
an OSI approved open source license.

See http://genoviz.sourceforge.net

IGB uses other open source software packages that may be distributed 
with the IGB source code release. Some of these include Xerces from Apache, 
Jetty from Mortbay Consulting, Vector Graphics from FreeHEP,
and Fusion from Affymetrix.  

Those packages are covered by their own open source licenses.

IGB is Copyright (c) 2000-2007 Affymetrix, Inc.  

Research and development of IGB was supported in part by NIH grant R01HG003040
(PI Gregg Helt) and NSF grant 0820371 (PI Ann Loraine).

GenoViz is hosted on SourceForge.net at 
http://sourceforge.net/projects/genoviz


----------------- DOCUMENTATION ----------------------------

An IGB user's guide is available on-line here:

http://wiki.transvar.org/confluence/display/igbman/Home

There are demos and tutorials for the GenoViz code inside
the genoviz_sdk directory.  These are described later below.

Javadocs can also be created from the java code using the
provided 'ant' scripts.

----------------- REQUIRED RESOURCES -----------------------

IGB requires the Java 2 Platform, Standard Edition (J2SE), 
version 1.6 or higher.
  http://java.sun.com/j2se/

IGB requires several additional resources.  These are included 
in the 'ext' directory, and you do not need to download them
separately.

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

'ant ext'
  to compile all source code and package it, together
  with all external resources, in a single executable file:
  igb_exe.jar

'ant jar' 
  to compile the source and create independent jar files and
  bundle files 
  
'ant docs' 
  to create javadocs documentation into the 'doc/api' folder.

'ant clean' 
  to remove compiled classes and documentation.

'ant cbc'
  to remove compiled bundle files 

The compiled code will be placed in the same directory structure 
as the java code, such that each *.class file will be placed
near the corresponding *.java file. 

----------------- RUNNING IGB -----------------------------


You can run IGB from within ant or from the command line.
The output is easier to read if you run it from the 
command line.

Running with ant:

Use the ant task 'run' to run the program from within ant.
'ant run'

Running from the command line. Sample command:
java -Xmx512m -classpath igb_exe.jar com.affymetrix.igb.main.Main


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

