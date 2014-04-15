#!/bin/sh

# Ann's environment only!
IGB_WORKSPACE=$HOME/src/genovi/trunk
ant clean
ant
cd full_dist
unzip LuceneIndexing.zip
wget http://mirrors.gigenet.com/apache//felix/org.apache.felix.main.distribution-4.2.1.tar.gz
tar xvfz org.apache.felix.main.distribution-4.2.1.tar.gz
cp felix-framework-4.2.1/bin/felix.jar bin/.
rm -rf org.apache.felix.main.distribution-4.2.1*
ant -Denv.lucene_index_dir=$HOME/src/genomes/pub/quickload/A_thaliana_Jun_2009/TAIR10_TE.bed.gz > ../log.txt
cd ..