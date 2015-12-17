# Integrated Genome Browser

The Integrated Genome Browser (IGB, pronounced ig-bee) is a fast, free, and flexible desktop genome browser. It is also a platform for developers to create new visualizations and visual analysis functions for genomics.

To get a copy, you can clone this repository or visit the [IGB Download](http://bioviz.org/igb/download.html) page at [BioViz.org](http://www.bioviz.org).

For documentation, visit:

* [BioViz Web site](http://www.bioviz.org) with platform-specific installers
* Searchable on-line [User's Guide](https://wiki.transvar.org/display/igbman/Home) explains IGB features
* [Developers Guide](https://wiki.transvar.org/display/igbdevelopers/Home) with links to Javadocs, explains IGB development workflow
* [IGB Channel on YouTube](https://www.youtube.com/channel/UC0DA2d3YdbQ55ljkRKHRBkg) with video tutorials and demos

For information about development plans, visit the [IGB Issue Tracker site](http://jira.transvar.org).

# Command-line quick start 

To build and run from the command line:

* clone this repo 
* navigate to your cloned repository
* checkout the branch you wish to build, for example

`git checkout igb_8_6` 

* not strictly required, but we suggest cleaning up any untracked files after checking out a new branch

`git clean -d -f`

* Use maven to build IGB. Skip tests to save time.

`mvn clean install -DskipTests=true`

This builds a igb_exe.jar, which you can run like this:

`java -Xmx4g -jar igb_exe.jar`

Use the -Xmx option to control how much memory the java process can consume. IGB runs best if you give it 4 Gb or more memory.

# To get help

* Ask on the [Biostars question and answer site](https://www.biostars.org/p/new/post/?tag_val=igb")
* Visit the [Bioviz help page](http://bioviz.org/igb/help.html)

