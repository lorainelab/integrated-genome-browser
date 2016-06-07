# Integrated Genome Browser

The Integrated Genome Browser (IGB, pronounced ig-bee) is a fast, free, and highly interactive desktop genome browser ideal for exploring and understanding large-scale data sets from genomics. Originally developed at Affymetrix, IGB is now open source software. 

IGB is also an extensible visual analytics platform developers can use to create new interactive visualizations and visual analysis functions called IGB Apps.

To get IGB, clone this repository or download an [IGB installer for your platform](http://bioviz.org/igb/download.html).

See:

* [BioViz Web site](http://www.bioviz.org) with platform-specific installers
* Searchable on-line [User's Guide](https://wiki.transvar.org/display/igbman/Home)
* [Developers Guide](https://wiki.transvar.org/display/igbdevelopers/Home) explains IGB development and IGB Apps
* [IGB Channel on YouTube](https://www.youtube.com/channel/UC0DA2d3YdbQ55ljkRKHRBkg) with video tutorials and demos
* [IGB Jira Issue Tracker site](http://jira.transvar.org) describes development plans
* Article describing [Integrated Genome Browser: Visual analytics platform for genomics](http://bioinformatics.oxfordjournals.org/content/early/2016/04/04/bioinformatics.btw069.long) 

***

# Command-line quick start 

To build and run from the command line:

1. Clone the [team repository](https://bitbucket.org/lorainelab/integrated-genome-browser) to your desktop.  
2. Inside the local copy, check out the branch you wish to build. Check out release_candidate to get the released (or soon to be released) version of IGB.
3. Build IGB using maven. Skip tests to save time.
4. Run IGB using the run_igb script for your platform.

Ex)

```
git clone https://bitbucket.org/lorainelab/integrated-genome-browser
cd integrated-genome-browser
git checkout release_candidate
mvn clean install -DskipTests=true
run_igb.sh
```

The preceding commands:

* Create IGB executable igb_exe.jar, used by the run scripts to run IGB. Contains all dependencies required to run IGB.
* Copy IGB jar files (artifacts) to your local maven cache, useful you are developing IGB Apps.

IGB and IGB Apps use [semantic versioning](http://semver.org/). Different IGB versions can co-exist in your local maven cache.

***

# To get help

* Post to the [Biostars question and answer site](https://www.biostars.org/p/new/post/?tag_val=igb")
* Visit the [Bioviz help page](http://bioviz.org/igb/help.html)

***

# Developing IGB Apps

IGB runs in an OSGi container, which supports adding and removing pluggable Apps while IGB is running. 
For OSGi tutorials written by IGB Developers, see: 

* Stackleader.com [blog posts on OSGI](https://blog.stackleader.com/tags/osgi/)

***

# To contribute

Use fork-and-branch workflow:

1. Fork the [team repository](http://www.bitbucket.org/lorainelab/integrated-genome-browser).
2. Create branches specific to the changes you want to make, push to your fork.
3. Issue pull requests from branches on your fork to the team repository's master branch.

See:

* Forking Workflow [tutorial](https://www.atlassian.com/git/tutorials/comparing-workflows/forking-workflow) by Atlassian
* Blog post titled [Using the Fork-and-Branch Git Workflow](http://blog.scottlowe.org/2015/01/27/using-fork-branch-git-workflow/)