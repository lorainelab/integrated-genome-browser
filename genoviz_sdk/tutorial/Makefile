#
# Copyright (c) 1997-2000 Neomorphic, Inc.
# Copyright (c) 2001-2004 Affymetrix, Inc.
#

JAVAC = javac $(JAVACARGS)

JARS = genoviz.jar
CLASSPATH=:$(JARS):.:

PRODUCT := $(notdir $(CURDIR))

CLASSDIR := ..
SOURCES := $(shell find . -name \*.java | xargs ls -t)
CLASSES := $(subst ./,$(CLASSDIR)/$(PRODUCT)/,$(SOURCES:.java=.class))

.PHONY: all clean

# This is stricktly speaking more correct and more pedantic.
#$(CLASSDIR)/$(PRODUCT)/%.class: %.java
#	cd $(CLASSDIR) && CLASSPATH=$(CLASSPATH) $(JAVAC) $(PRODUCT)/$?

# This is a shortcut.
# It causes one invocation of javac on all the .java files.
$(CLASSDIR)/$(PRODUCT)/%.class: %.java
	cd $(CLASSDIR) && CLASSPATH=$(CLASSPATH) $(JAVAC) $(subst ./,$(PRODUCT)/,$(SOURCES))

all: $(CLASSES)

clean:
	find . -name \*\.class -exec rm {} \;
