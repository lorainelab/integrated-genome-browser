# Integrated Genome Browser

The Integrated Genome Browser (IGB, pronounced ig-bee) is a fast, free, and flexible desktop genome browser originally developed at Affymetrix and now public domain and open sources. 

IGB is also a platform for developers to create new visualizations and visual analysis functions for genomics.

To get a copy, you clone this repository or visit the [IGB Download](http://bioviz.org/igb/download.html) page at [BioViz.org](http://www.bioviz.org).

See:

* [BioViz Web site](http://www.bioviz.org) with platform-specific installers
* Searchable on-line [User's Guide](https://wiki.transvar.org/display/igbman/Home) explains IGB features
* [Developers Guide](https://wiki.transvar.org/display/igbdevelopers/Home) explains how to develop IGB and IGB Apps
* [IGB Channel on YouTube](https://www.youtube.com/channel/UC0DA2d3YdbQ55ljkRKHRBkg) with video tutorials and demos
* [IGB Issue Tracker site](http://jira.transvar.org) describes development plans.
* [Integrated Genome Browser: Visual analytics platform for genomics](http://bioinformatics.oxfordjournals.org/content/early/2016/04/04/bioinformatics.btw069.long) - article appearing in the journal Bioinformatics
# Command-line quick start 

To build and run from the command line:

* clone this repo 
* navigate to your cloned repository
* checkout the branch you wish to build

`git checkout release_candidate` 

The release_candidate branch usually corresponds to the version of IGB available to users for download from [BioViz.org](http://www.bioviz.org). 

* not strictly required, but we suggest cleaning up any untracked files after checking out a new branch

`git clean -d -f`

* Use maven to build IGB. Skip tests to save time.

`mvn clean install -DskipTests=true`

Then you can run IGB using scripts provided in the repository. On Unix systems, execute:

`run_igb.sh` 

On Windows, run:

`run_igb.bat`

# To get help

* Post to the [Biostars question and answer site](https://www.biostars.org/p/new/post/?tag_val=igb")
* Visit the [Bioviz help page](http://bioviz.org/igb/help.html)

# Developing IGB Apps

IGB runs in an OSGi container, which supports adding and removing pluggable Apps while IGB is running. 
For a tutorial on OSGi written by IGB Developers, see: 

* Stackleader.com [blog posts on OSGI](https://blog.stackleader.com/tags/osgi/)

# To contribute

IGB development uses the Fork-and-Branch workflow. See:

* Forking Workflow [tutorial](https://www.atlassian.com/git/tutorials/comparing-workflows/forking-workflow) by Atlassian
* Blog post titled [Using the Fork-and-Branch Git Workflow](http://blog.scottlowe.org/2015/01/27/using-fork-branch-git-workflow/)