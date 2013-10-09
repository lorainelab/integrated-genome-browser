package com.affymetrix.genometryImpl.das2.tutorial;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;
import com.affymetrix.genometryImpl.das2.*;

/**
 * Example code for working with DAS/2 Servers
 * @author davidnix
 */
public class Das2Demo {

	//server info
	private String genoPubDas2UrlString = "http://bioserver.hci.utah.edu:8080/DAS2DB/genome";
	//private String classicDas2UrlString = "http://netaffxdas.affymetrix.com/das2/genome";
	private String name = "UofUBioInfoCore";
	private String userName = "guest";
	private String password = "guest";	


	//constructor
	public Das2Demo(){

		try {
			String das2UrlString = genoPubDas2UrlString;

			//set Authenticator for possible digest or basic authentication response from the Das2 server, some require this some don't, best to play it safe
			Authenticator.setDefault(new MyAuthenticator(userName, password));

			//create a Das2ServerInfo object and load the data
			Das2ServerInfo dsi = new Das2ServerInfo(das2UrlString, name, true);

			//get a list of available species
			Map<String, Das2Source> das2Sources = dsi.getSources();
			System.out.println("\nSpecies:");
			for (String speciesName : das2Sources.keySet()) {
				System.out.println("\t"+speciesName);
			}

			//get a list of available genome builds offered for each species
			System.out.println("\nSpecies and genome builds:");
			for (String speciesName : das2Sources.keySet()) {
				System.out.println("\t"+speciesName);
				Das2Source ds = das2Sources.get(speciesName);
				Map<String, Das2VersionedSource> das2VersionedSources = ds.getVersions();
				for (String genomeBuild : das2VersionedSources.keySet()) {
					System.out.println("\t\t"+genomeBuild);
				}
			}
			
			//work with versionedSource H_sapiens_Mar_2006
			Das2Source sourceHSapiens = das2Sources.get(das2UrlString+ "/H_sapiens");
			Das2VersionedSource versionedSourceHSapiensMar2006 = sourceHSapiens.getVersions().get(das2UrlString +"/H_sapiens_Mar_2006");
			
			//get a list of available segments (aka chromosomes) for H_sapiens_Mar_2006, assuming that a Das2VersionedSource for this genome was found			
			Map<String,Das2Region> segmentsHSapiensMar2006 = versionedSourceHSapiensMar2006.getSegments();
			System.out.println("\nChromosomes for H_sapiens_Mar_2006:");
			for (String chromosome : segmentsHSapiensMar2006.keySet()) {
				System.out.println("\t"+chromosome);
			}
			
			//get a list of available types (aka data tracks, data sets)
			Map<String,Das2Type> typesHSapiensMar2006 = versionedSourceHSapiensMar2006.getTypes();
			System.out.println("\nTypes/ datasets for H_sapiens_Mar_2006:");
			for (String typeName : typesHSapiensMar2006.keySet()) {
				System.out.println("\t"+typeName);
			}
			
			//work with an example dataset, the http://bioserver.hci.utah.edu:8080/DAS2DB/genome/H_sapiens_Mar_2006/ENCODE/USeq/CTCF+Broad/Peak+Calls/All+Peaks+Summed+Scores type dataset
			Das2Type dataset = typesHSapiensMar2006.get("http://bioserver.hci.utah.edu:8080/DAS2DB/genome/H_sapiens_Mar_2006/ENCODE/USeq/CTCF+Broad/Peak+Calls/All+Peaks+Summed+Scores");
			
			//print the dataset's properties, if any
			Map<String,String> properties = dataset.getProps();
			System.out.println("\nProperties for example dataset:");
			for (String key : properties.keySet()) {
				System.out.println("\t"+key+" = "+properties.get(key));
			}
			
			//print what formats the dataset can be returned as, this isn't really used and can be left out of a feature request
			Map<String,String> formats = dataset.getFormats();
			System.out.println("\nFormats for example dataset:");
			for (String key : formats.keySet()) {
				System.out.println("\t"+key+" = "+properties.get(key));
			}
			
			//download some of the data to file
			
			//   1st build a feature request
			
			//String query_part = DetermineQueryPart(region, overlap_filter, dataset.getURI(), null);
           // String feature_query = request_root + "?" + query_part;
			
			


		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class MyAuthenticator extends Authenticator {
		String userName;
		String password;
		public MyAuthenticator (String userName, String password){
			this.userName = userName;
			this.password = password;
		}
		public PasswordAuthentication getPasswordAuthentication () {
			return new PasswordAuthentication (userName, password.toCharArray());
		}
	}

	//main
	public static void main(String[] args) {
		new Das2Demo();

	}

	/**Fetches a BufferedReader from a url, zip/gz OK.*/
	public static BufferedReader fetchBufferedReader(URL url) throws IOException{
		BufferedReader in = null;
		InputStream is = url.openStream();
		String name = url.toString();
		if (name.endsWith(".gz")) {
			in = new BufferedReader(new InputStreamReader(new GZIPInputStream(is)));
		}
		else if (name.endsWith(".zip")){
			ZipInputStream zis = new ZipInputStream(is);
			zis.getNextEntry();
			in = new BufferedReader(new InputStreamReader(zis));
		}
		else in = new BufferedReader(new InputStreamReader(is));
		return in;
	}

}
