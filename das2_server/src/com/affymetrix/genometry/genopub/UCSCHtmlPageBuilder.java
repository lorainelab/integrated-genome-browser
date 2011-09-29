package com.affymetrix.genometry.genopub;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

/**Static methods for building an html document that serves data to the UCSC Genome Browser.*/
public class UCSCHtmlPageBuilder {
	
	private static final Pattern FILE_SEPARATOR = Pattern.compile(File.separator);
	public static final String NAME_HTML_DOC = "genopubUCSC.html";

	/**Makes and saves an html doc containing links that load resorces into the UCSC genome browser.
	 * */
	public static File buildUCSCTreeDoc( LinkedHashMap<String, ArrayList<Annotation>> nestedAnnotations, File userDir, File treeMenuFilesDir) {
		String html;
		boolean debug = false;
		
		//any annotations to link?
		if (nestedAnnotations.size() ==0){
			html = ucscNoFilesToLink;
		}
		else {
			//fetch header
			StringBuilder sb = new StringBuilder(htmlTreeHeader);
			//recurse through hashmap looking for annotations
			String workingSpecies = null;
			String workingBuild = null;
			ArrayList<String> openFolders = new ArrayList<String>();
			ArrayList<String> toOpen = new ArrayList<String>();
			ArrayList<String> toClose = new ArrayList<String>();
			ArrayList<String> toKeep = new ArrayList<String>();
			boolean folderOpen = false;
			for (String key: nestedAnnotations.keySet()){
				if (debug) System.out.println("\nNesting "+key);
				//split on /
				String[] tokens = FILE_SEPARATOR.split(key);
				String species = tokens[0];
				String build = tokens[1];
				//1st anno?
				if (workingSpecies == null){
					appendFirstSpeciesAndBuild(sb, species, build);
					workingSpecies = species;
					workingBuild = build;
				}
				//new build? might be new species too
				else if (build.equals(workingBuild) == false){
					//close open folder?
					if (folderOpen){
						folderOpen = false;
						sb.append("\t\t</ul>\n\t\t</li>\n");
					}
					//close old build
					sb.append("\t</ul>\n\t</li>\n");
					//open new species?
					if (workingSpecies.equals(species) == false){
						//close old species
						sb.append("</ul>\n</li>\n");
						//open new species
						sb.append("<li>");
						sb.append(species);
						sb.append("\n\t<ul rel='open'>\n");
						workingSpecies = species;
					}
					//open new build
					sb.append("\t<li>");
					sb.append(build);
					sb.append("\n\t\t<ul rel='open'>\n");
					workingBuild = build;
				}
				//need to make any folders?
				if (tokens.length > 2){
					toOpen.clear();
					toClose.clear();
					toKeep.clear();
					
					//scan for those to close
					//for each open folder see if it is still in the current anno
					for (int i=0; i< openFolders.size(); i++){
						String openFolder = openFolders.get(i);
						boolean notFound = true;
						//for each current anno folders
						for (int j=2; j< tokens.length; j++){
							String tokenFolder = tokens[j]+j;
							if (openFolder.equals(tokenFolder)){
								toKeep.add(openFolder);
								notFound = false;
								break;
							}
						}
						if (notFound) {
							toClose.add(openFolder);
							//close
							sb.append("\t\t</ul>\n\t\t</li>\n");
						}
					}
					
					//scan for those to open
					//for each anno folder not found in open folders
					for (int j=2; j< tokens.length; j++){
						String tokenFolder = tokens[j]+j;
						boolean notFound = true;
						//for each open folder
						for (int i=0; i< openFolders.size(); i++){
							String openFolder = openFolders.get(i);
							if (openFolder.equals(tokenFolder)){
								notFound = false;
								break;
							}
						}
						if (notFound) {
							toOpen.add(tokenFolder);
							//open folder
							sb.append("\t\t<li>");
							sb.append(tokens[j]);
							sb.append("\n\t\t\t<ul>\n");
						}
					}
					
					if (debug) {
						System.out.println("\topenFolders "+openFolders);
						System.out.println("\ttoClose     "+toClose);
						System.out.println("\ttoOpen      "+toOpen);
						System.out.println("\ttoKeep      "+toKeep);
					}
					
					//reset lists
					toKeep.addAll(toOpen);
					openFolders.clear(); 
					openFolders.addAll(toKeep);
					
					
				}
				else if (debug) System.out.println("\nIn root, making and closing none.");
				
				//make annotations
				for (Annotation anno: nestedAnnotations.get(key)) makeAnnotationLink(sb, anno);
			}
			//close open folders, genome build, species
			int num = openFolders.size();
			for (int i=0; i< num; i++) sb.append("\t\t</ul>\n\t\t</li>\n");
			sb.append("\t</ul>\n\t</li>\n");
			sb.append("</ul>\n</li>\n");
			
			sb.append(htmlTreeTail);
			html = sb.toString();
		}
		if (debug) System.out.println("\n"+html);
		File doc = new File (userDir, NAME_HTML_DOC);
		try {
			PrintWriter out = new PrintWriter( new FileWriter (doc));
			out.println(html);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//copy over supporting files
		copyTreeMenuFiles(userDir, treeMenuFilesDir);
		
		return doc;
	}
	
	private static void copyTreeMenuFiles(File outputDir, File treeMenuFilesDir){
		File[] filesToCopy = treeMenuFilesDir.listFiles();
		for (File f: filesToCopy){
			File copy = new File (outputDir, f.getName());
			Util.copy(f, copy);
		}
	}
	
	private static void appendFirstSpeciesAndBuild(StringBuilder sb, String species, String build){
		//open new species
		sb.append("<li>");
		sb.append(species);
		sb.append("\n\t<ul rel='open'>\n");
		//open new genome build
		sb.append("\t<li>");
		sb.append(build);
		sb.append("\n\t\t<ul rel='open'>\n");

	}
	
	private static void makeAnnotationLink(StringBuilder sb, Annotation anno){
		sb.append("\t\t<li>");
		sb.append("<a href='");
		sb.append(anno.getUcscHttpURL());
		sb.append("' target='_blank'>");
		sb.append(anno.getName());
		sb.append(" ");
		sb.append(anno.getNumber());
		sb.append("</a>");
		sb.append("</li>\n");
	}



	private static final String htmlTreeHeader = 
		"<html>\n"+
		"<head>\n"+
		"<script type=\"text/javascript\" src=\"simpletreemenu.js\">\n"+
		"/***********************************************\n"+
		"* Simple Tree Menu- Dynamic Drive DHTML code library (www.dynamicdrive.com)\n"+
		"* This notice MUST stay intact for legal use\n"+
		"* Visit Dynamic Drive at http://www.dynamicdrive.com/ for full source code\n"+
		"* http://www.dynamicdrive.com/dynamicindex1/navigate1.htm\n"+
		"***********************************************/\n"+
		"</script>\n"+
		"<link rel=\"stylesheet\" type=\"text/css\" href=\"simpletree.css\" />\n"+
		"</head>\n"+
		"<body>\n"+
		"<h4>Click folders to expand branches, click links to load particular data tracks in the UCSC Genome Browser:</h4>\n"+
		"<a href=\"javascript:ddtreemenu.flatten('treemenu2', 'expand')\">Expand</a> | <a href=\"javascript:ddtreemenu.flatten('treemenu2', 'contact')\">Collapse</a>\n"+
		"<ul id=\"treemenu2\" class=\"treeview\">\n";

	
	private static final String htmlTreeTail =
		"</ul>\n"+
		"<script type=\"text/javascript\">\n"+
		"ddtreemenu.createTree(\"treemenu2\", false, 14)\n"+
		"</script>\n"+
		"</body>\n"+
		"</html>\n";
	
	private static final String ucscNoFilesToLink = "No xxx.bw, xxx.bb, or xxx.bam files to link!";
	
}
