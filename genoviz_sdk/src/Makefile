#
# Copyright (c) 1997-2000 Neomorphic, Inc.
# Copyright (c) 2001-2004 Affymetrix, Inc.
#

JAVAC = javac $(JAVACARGS)
# Build the .class files in the same directories as the .java sources
CLASSDIR=../src

JAVADOC = javadoc -J-mx64m

PKGROOTS = \
	com.affymetrix.genoviz \
	test.affymetrix.genoviz \

PACKAGES = \
	com.affymetrix.genoviz.awt \
	com.affymetrix.genoviz.awt.command \
	com.affymetrix.genoviz.beans \
	com.affymetrix.genoviz.bioviews \
	com.affymetrix.genoviz.datamodel \
	com.affymetrix.genoviz.event \
	com.affymetrix.genoviz.glyph \
	com.affymetrix.genoviz.parser \
	com.affymetrix.genoviz.pseudoswing \
	com.affymetrix.genoviz.util \
	com.affymetrix.genoviz.widget \
	com.affymetrix.genoviz.widget.neoassembler \
	com.affymetrix.genoviz.widget.neomap \
	com.affymetrix.genoviz.widget.neoqualler \
	com.affymetrix.genoviz.widget.neoseq \
	com.affymetrix.genoviz.widget.neotracer \
	com.affymetrix.genoviz.widget.tieredmap \

.PHONY: all $(PKGROOTS) incremental doc docs clean docs-clean genoviz-clean
.INTERMEDIATE: $(PKGROOTS)

SOURCES = $(shell find $(subst .,/,$(PKGROOTS)) -name \*.java)

all: genoviz.jar

$(CLASSDIR):
	@if test ! -d $@; then \
		mkdir $@; \
	fi;

$(PKGROOTS): $(CLASSDIR) $(SOURCES)
	$(JAVAC) -d $(CLASSDIR) `find $(subst .,/,$(PKGROOTS)) -name \*.java`

genoviz.jar: $(PKGROOTS)
	cd $(CLASSDIR); \
	jar cf ../$@ \
		`find com -name \*\.class` ;

DOCDIR = ../doc/api

doc: docs

# creates javadocs in directory doc/api.
docs:
	@if test ! -d $(DOCDIR); then \
		mkdir $(DOCDIR); \
	fi;
	$(JAVADOC) \
		-header "<b>GenoViz by <a href='http://www.affymetrix.com' target='_top'>Affymetrix</a></b>" \
                -bottom "Copyright &copy; 2001-2005 Affymetrix, Inc." \
		-public -d $(DOCDIR) \
		-windowtitle "GenoViz Documentation - Affymetrix, Inc." \
		com.affymetrix.genoviz.awt \
		com.affymetrix.genoviz.awt.command \
		com.affymetrix.genoviz.beans \
		com.affymetrix.genoviz.bioviews \
		com.affymetrix.genoviz.datamodel \
		com.affymetrix.genoviz.event \
		com.affymetrix.genoviz.glyph \
		com.affymetrix.genoviz.parser \
		com.affymetrix.genoviz.pseudoswing \
		com.affymetrix.genoviz.util \
		com.affymetrix.genoviz.widget \
		com.affymetrix.genoviz.widget.neoassembler \
		com.affymetrix.genoviz.widget.neomap \
		com.affymetrix.genoviz.widget.neoqualler \
		com.affymetrix.genoviz.widget.neoseq \
		com.affymetrix.genoviz.widget.neotracer \
		com.affymetrix.genoviz.widget.tieredmap \
		2>&1 | grep -v "missing .serial tag"

docs-clean:
	-rm -rf doc/api/

clean:
	-find $(CLASSDIR) -name \*\.class -exec rm {} \;
