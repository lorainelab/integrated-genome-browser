/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
*
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*   Distributions from Affymetrix, Inc., place this in the
*   IGB_LICENSE.html file.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/
package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.*;
import com.affymetrix.genometryImpl.parsers.*;
import com.affymetrix.genometryImpl.util.*;
import com.affymetrix.igb.Application;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.util.LocalUrlCacher;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public final class QuickLoadServerModel {
  private static final SynonymLookup LOOKUP = SynonymLookup.getDefaultLookup();

  private final SingletonGenometryModel gmodel;

  private static final Pattern tab_regex = Pattern.compile("\t");

  private String root_url;
  private final List<String> genome_names = new ArrayList<String>();

  private final Map<AnnotatedSeqGroup,String> group2name = new HashMap<AnnotatedSeqGroup,String>();
  private final Map<String,Boolean> genome2init = new HashMap<String,Boolean>();

  // A map from String genome name to a List of filenames on the server for that group
  private final Map<String,List<String>> genome2file_names = new HashMap<String,List<String>>();

  private static final Map<String,QuickLoadServerModel> url2quickload = new HashMap<String,QuickLoadServerModel>();


  
  /*public void clear() {
    genome_names.clear();
    group2name.clear();
    genome2init.clear();
    genome2file_names.clear();
    url2quickload.clear();
  }*/

  public QuickLoadServerModel(SingletonGenometryModel gmodel, String url) {
    this.gmodel = gmodel;
    root_url = url;
    if (! root_url.endsWith("/")) {
      root_url = root_url + "/";
    }
    loadGenomeNames();
  }


  public static QuickLoadServerModel getQLModelForURL(SingletonGenometryModel gmodel, URL url) {
    String ql_http_root = url.toExternalForm();
    if (! ql_http_root.endsWith("/")) {
      ql_http_root = ql_http_root + "/";
    }
    QuickLoadServerModel ql_server = url2quickload.get(ql_http_root);
    if (ql_server == null) {
      ql_server = new QuickLoadServerModel(gmodel, ql_http_root);
      url2quickload.put(ql_http_root, ql_server);
      LocalUrlCacher.loadSynonyms(LOOKUP, ql_http_root+"synonyms.txt");
    }
    return ql_server;
  }


  private static boolean getCacheAnnots() {
    //return UnibrowPrefsUtil.getBooleanParam(PREF_QUICKLOAD_CACHE_ANNOTS, CACHE_ANNOTS_DEFAULT);
		return true;
  }

  private String getRootUrl() { return root_url; }

  public List<String> getGenomeNames() { return genome_names; }

  private AnnotatedSeqGroup getSeqGroup(String genome_name) {
	  return gmodel.addSeqGroup(LOOKUP.findMatchingSynonym(gmodel.getSeqGroupNames(), genome_name));
  }

  /**
   *  Returns the list of String filenames that this QuickLoad server has
   *  for the genome with the given name.
   *  The list may (rarely) be empty, but never null.
   */
  public List<String> getFilenames(String genome_name) {
	  genome_name = LOOKUP.findMatchingSynonym(genome_names, genome_name);
		if (genome2init.get(genome_name) != Boolean.TRUE) {
			initGenome(genome_name);
			loadAnnotationNames(genome_name);
		}
    List<String> filenames = genome2file_names.get(genome_name);
    if (filenames == null) return Collections.<String>emptyList();
    else return filenames;
  }
  
  private void initGenome(String genome_name) {
		Application.getApplicationLogger().fine("initializing data for genome: " + genome_name);
		boolean seq_init = loadSeqInfo(genome_name);
		boolean annot_init = loadAnnotationNames(genome_name);
		if (seq_init && annot_init) {
			genome2init.put(genome_name, Boolean.TRUE);
		}
		List<String> file_names = genome2file_names.get(genome_name);
		if (file_names != null) {
			file_names.clear();
		}
  }

  /**
   *  Determines the list of annotation files available in the genome directory.
   *  Looks for ~genome_dir/annots.txt file which lists annotation files
   *  available in same directory.  Returns true or false depending on
   *  whether the file is sucessfully loaded.
   *  You can retrieve the filenames with {@link #getFilenames(String)}
   */
  private boolean loadAnnotationNames(String genome_name) {
	  genome_name = LOOKUP.findMatchingSynonym(genome_names, genome_name);
		String genome_root = root_url + genome_name + "/";
		Application.getApplicationLogger().fine("loading list of available annotations for genome: " + genome_name);
		String filename = genome_root + IGBConstants.annotsTxt;

		// Make a new list of filenames, in case this is being re-initialized
		// If this search fails, then we're just returning an empty list.
		List<String> file_names = new ArrayList<String>();
		genome2file_names.put(genome_name, file_names);

		InputStream istr = null;
		BufferedReader br = null;
		try {
			istr = LocalUrlCacher.getInputStream(filename, getCacheAnnots());
			if (istr == null) {
				// Search failed.  getInputStream has already logged warnings about this.
				return false;
			}
			br = new BufferedReader(new InputStreamReader(istr));

			String line;

			while ((line = br.readLine()) != null) {
				String[] fields = tab_regex.split(line);
				if (fields.length >= 1) {
					String annot_file_name = fields[0];
					if (annot_file_name == null || annot_file_name.length()==0) {
						continue;
					}
					file_names.add(annot_file_name);
				}
			}
			return true;
		} catch (Exception ex) {
			System.out.println("Couldn't find or couldn't process file " + filename);
			ex.printStackTrace();
			return false;
		} finally {
			GeneralUtils.safeClose(istr);
			GeneralUtils.safeClose(br);
		}
  }

  private boolean loadSeqInfo(String genome_name) {
		String liftAll = IGBConstants.liftAllLft;
		String modChromInfo = IGBConstants.modChromInfoTxt;
	  genome_name = LOOKUP.findMatchingSynonym(genome_names, genome_name);
    boolean success = false;
    String genome_root = root_url + genome_name + "/";
    Application.getApplicationLogger().fine("loading list of chromosomes for genome: " + genome_name);
    InputStream lift_stream = null;
    InputStream cinfo_stream = null;
    try {

      Application.getApplicationLogger().fine("lift URL: " + genome_root + liftAll);
      String lift_path = genome_root + liftAll;
      try {
				// don't warn about this file, since we'll look for modChromInfo file
        lift_stream = LocalUrlCacher.getInputStream(lift_path, getCacheAnnots(), null, true);
      }
      catch (Exception ex) {
				// exception can be ignored, since we'll look for modChromInfo file.
        Application.getApplicationLogger().fine("couldn't find " + liftAll +", looking instead for " + modChromInfo);
        lift_stream = null;
      }
      if (lift_stream == null) {
				String cinfo_path = genome_root + modChromInfo;
        try {
          cinfo_stream = LocalUrlCacher.getInputStream(cinfo_path,  getCacheAnnots(), null, false);
        }
        catch (Exception ex) {
          System.err.println("ERROR: could find neither " + lift_path + " nor " + cinfo_path);
					ex.printStackTrace();
          cinfo_stream = null;
        }
      }

      boolean annot_contigs = false;
      if (lift_stream != null) {
        LiftParser.parse(lift_stream, gmodel, genome_name, annot_contigs);
        success = true;
      }
      else if (cinfo_stream != null) {
        ChromInfoParser.parse(cinfo_stream, gmodel, genome_name);
        success = true;
      }
    }
    catch (Exception ex) {
      ErrorHandler.errorPanel("ERROR", "Error loading data for genome '"+ genome_name +"'", ex);
    }
    finally {
			GeneralUtils.safeClose(lift_stream);
			GeneralUtils.safeClose(cinfo_stream);
    }
    return success;
  }


  private void loadGenomeNames() {
		String contentsTxt = IGBConstants.contentsTxt;
		InputStream istr = null;
		InputStreamReader ireader = null;
		BufferedReader br = null;
    try {
      try {
        istr = LocalUrlCacher.getInputStream(root_url + contentsTxt, getCacheAnnots());
      } catch (Exception e) {
        System.out.println("ERROR: Couldn't open '"+root_url+ contentsTxt +"\n:  "+e.toString());
        istr = null; // dealt with below
      }
      if (istr == null) {
        System.out.println("Could not load QuickLoad contents from\n" + root_url + contentsTxt);
        return;
      }
      ireader = new InputStreamReader(istr);
      br = new BufferedReader(ireader);
      String line;
      while ((line = br.readLine()) != null) {
        AnnotatedSeqGroup group = null;
        String[] fields = tab_regex.split(line);
        if (fields.length >= 1) {
          String genome_name = fields[0];
					genome_name = genome_name.trim();
					if (genome_name.length() == 0) {
						System.out.println("Found blank QuickLoad genome -- skipping");
						continue;
					}
          group = this.getSeqGroup(genome_name);  // returns existing group if found, otherwise creates a new group
          genome_names.add(genome_name);
          group2name.put(group, genome_name);
        }
        // if quickload server has description, and group is new or doesn't yet have description, add description to group
        if ((fields.length >= 2) && (group.getDescription() == null)) {
          group.setDescription(fields[1]);
        }
      }
    }
    catch (Exception ex) {
      ErrorHandler.errorPanel("ERROR", "Error loading genome names", ex);
    }
		finally {
			GeneralUtils.safeClose(istr);
			GeneralUtils.safeClose(ireader);
			GeneralUtils.safeClose(br);
		}
  }

    @Override
  public String toString() {
    return "QuickLoadServerModel: url='" + getRootUrl() + "'";
  }
}
