package com.gene.luceneindexing;

import java.io.IOException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    public void start(BundleContext bundleContext) throws Exception {
        System.out.println("**************************");
        String docsPath = System.getenv("lucene_index_dir");
        if (docsPath == null) {
            docsPath = System.getProperty("lucene_index_dir");
        }
        if (docsPath == null) {
            System.err.println("please set the \"lucene_index_dir\" environment variable to the directory to be indexed. "
                    + "Optionally, also set the \"dump\" environment variable to something to trial run the indexing.");
        } else {
            String dumpString = System.getenv("dump");
            if (dumpString == null) {
                dumpString = System.getProperty("dump");
            }
            boolean dump = dumpString != null && dumpString.toLowerCase().startsWith("y");
            System.out.println("Indexing directory " + docsPath + ", dump = " + dump + ", classpath=" + System.getProperty("java.class.path"));
            try {
                new IndexFiles().createIndex(docsPath, dump);
            } catch (IOException e) {
                System.out.println("!!!!!!!!!!!!!!!!!!!!!");
                e.printStackTrace(System.out);
                System.out.println("Lucene Indexing caught a " + e.getClass() + "\n with message: " + e.getMessage());
            }
        }
        bundleContext.getBundle(0).stop();
    }

    public void stop(BundleContext bundleContext) throws Exception {
    }
}
