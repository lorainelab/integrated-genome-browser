package com.affymetrix.genometryImpl.parsers.useq;
import com.affymetrix.genometryImpl.parsers.useq.data.*;

import java.io.*;
import java.util.zip.*;
import java.util.*;
import java.util.regex.*;

/**Class for parsing USeq binary files for DAS2 requests and writing the data to stream. A USeqArchive is created upon request for a USeq data file
 * this should be cached to speed up subsequent retrieval.
 * 
 * @author david.nix@hci.utah.edu*/
public class USeqArchive {

	private File zipFile;
	private ZipFile zipArchive;
	private ArchiveInfo archiveInfo;
	private ZipEntry archiveReadMeEntry;
	private HashMap<String, DataRange[]> chromStrandRegions = new HashMap<String, DataRange[]> ();
	//DAS2 does not support stranded requests at this time so leave false.
	private boolean maintainStrandedness = false;

	public USeqArchive (File zipFile) throws Exception{
		this.zipFile = zipFile;
		parseZipFile();
	}

	/**Fetches from the zip archive the files that intersect the unstranded range request and writes them to the stream.
	 * @return	false if no files found*/
	public boolean writeSlicesToStream (OutputStream outputStream, String chromosome, int beginningBP, int endingBP, boolean closeStream) throws IOException{
		//fetch any overlapping entries
		ArrayList<ZipEntry> entries = fetchZipEntries(chromosome, beginningBP, endingBP);
		if (entries == null) return false;
		//add readme
		entries.add(0, archiveReadMeEntry);
		ZipOutputStream out = new ZipOutputStream(outputStream);
		int count;
		byte data[] = new byte[2048];
		int numEntries = entries.size();
		SliceInfo sliceInfo = null;
		//for each entry
		for (int i=0; i< numEntries; i++){
			//get input stream to read entry
			ZipEntry entry = entries.get(i);			
			BufferedInputStream bis = new BufferedInputStream (zipArchive.getInputStream(entry));
			//is this entirely contained or needing to be split?, skip first entry which is the readme file
			if (i!=0) sliceInfo = new SliceInfo(entry.getName());
			if (i == 0 || sliceInfo.isContainedBy(beginningBP, endingBP)){
				out.putNextEntry(entry);
				//read in and write out, wish there was a way of just copying it directly
				while ((count = bis.read(data, 0, 2048))!= -1)  out.write(data, 0, count);
				//close entry
				out.closeEntry();
			}
			//slice the slice
			else sliceAndWriteEntry(beginningBP, endingBP, sliceInfo, bis, out);
			
			//close input entry input stream
			bis.close();
		}
		//close streams?
		if (closeStream) {
			out.close();
			outputStream.close();
		}
		return true;
	}

	private void sliceAndWriteEntry(int beginningBP, int endingBP, SliceInfo sliceInfo, BufferedInputStream bis, ZipOutputStream out) throws IOException {
		String dataType = sliceInfo.getBinaryType();
		DataInputStream dis = new DataInputStream(bis);
		//Position
		if (USeqUtilities.POSITION.matcher(dataType).matches()) {
			PositionData d = new PositionData(dis, sliceInfo);
			if (d.trim(beginningBP, endingBP)) d.write(out, true);
		}
		//PositionScore
		else if (USeqUtilities.POSITION_SCORE.matcher(dataType).matches()) {
			PositionScoreData d = new PositionScoreData(dis, sliceInfo);
			if (d.trim(beginningBP, endingBP)) d.write(out, true);
		}
		//PositionText
		else if (USeqUtilities.POSITION_TEXT.matcher(dataType).matches()) {
			PositionTextData d = new PositionTextData(dis, sliceInfo);
			if (d.trim(beginningBP, endingBP)) d.write(out, true);
		}
		//PositionScoreText
		else if (USeqUtilities.POSITION_SCORE_TEXT.matcher(dataType).matches()) {
			PositionScoreTextData d = new PositionScoreTextData(dis, sliceInfo);
			if (d.trim(beginningBP, endingBP)) d.write(out, true);
		}
		//Region
		else if (USeqUtilities.REGION.matcher(dataType).matches()) {
			RegionData d = new RegionData(dis, sliceInfo);
			if (d.trim(beginningBP, endingBP)) d.write(out, true);
		}
		//RegionScore
		else if (USeqUtilities.REGION_SCORE.matcher(dataType).matches()) {
			RegionScoreData d = new RegionScoreData(dis, sliceInfo);
			if (d.trim(beginningBP, endingBP)) d.write(out, true);
		}
		//RegionText
		else if (USeqUtilities.REGION_TEXT.matcher(dataType).matches()) {
			RegionTextData d = new RegionTextData(dis, sliceInfo);
			if (d.trim(beginningBP, endingBP)) d.write(out, true);
		}
		//RegionScoreText
		else if (USeqUtilities.REGION_SCORE_TEXT.matcher(dataType).matches()) {
			RegionScoreTextData d = new RegionScoreTextData(dis, sliceInfo);
			if (d.trim(beginningBP, endingBP)) d.write(out, true);
		}
		//unknown!
		else {
			throw new IOException ("Unknown USeq data type, '"+dataType+"', for slicing data from  -> '"+sliceInfo.getSliceName()+"\n");
		}
		dis.close();
	}

	/**Fetches from the zip archive the files that intersect the unstranded range request and saves to a new zip archive.
	 * @return	Sliced zip archive or null if no files found*/
	public File writeSlicesToFile (File saveDirectory, String chromosome, int beginningBP, int endingBP) throws Exception{
		//fetch any overlapping entries
		ArrayList<ZipEntry> entries = fetchZipEntries(chromosome, beginningBP, endingBP);
		if (entries == null) return null;
		//add readme
		entries.add(0, archiveReadMeEntry);
		//make new zip archive to hold slices
		File slicedZipArchive = new File (saveDirectory, "USeqDataSlice_"+createRandowWord(7)+"."+USeqUtilities.USEQ_EXTENSION_NO_PERIOD);
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(slicedZipArchive));
		int count;
		byte data[] = new byte[2048];
		int numEntries = entries.size();
		//for each entry
		for (int i=0; i< numEntries; i++){
			//get input stream to read entry
			ZipEntry entry = entries.get(i);
			out.putNextEntry(entry);
			BufferedInputStream is = new BufferedInputStream (zipArchive.getInputStream(entry));
			//read in and write out, wish there was a way of just copying it directly
			while ((count = is.read(data, 0, 2048))!= -1)  out.write(data, 0, count);
			//close streams
			out.closeEntry();
			is.close();
		}
		out.close();
		return slicedZipArchive;
	}

	/**Fetches the ZipEntries for a given range.  Returns null if none found or chromStrand not found.*/
	public ArrayList<ZipEntry> fetchZipEntries (String chromStrand, int beginningBP, int endingBP){
		ArrayList<ZipEntry> al = new ArrayList<ZipEntry>();
		//fetch chromStrand, these are sorted so ounce found then lost kill it.
		DataRange[] dr = chromStrandRegions.get(chromStrand);
		if (dr == null) return null;
		boolean foundOne = false;
		for (int i=0; i< dr.length; i++){
			if (dr[i].intersects(beginningBP, endingBP)) {
				al.add(dr[i].zipEntry);
				foundOne = true;
			}
			else if (foundOne) break;
		}
		if (foundOne == false) return null;
		return al;
	}

	/**Loads the zip entries into the chromosomeStrand DataRange[] HashMap*/
	@SuppressWarnings("unchecked")
	private void parseZipFile() throws IOException{
		//make ArchiveInfo, it's always the first entry
		if (USeqUtilities.USEQ_ARCHIVE.matcher(zipFile.getName()).matches() == false) throw new IOException ("This file does not appear to be a USeq archive! "+zipFile);
		zipArchive = new ZipFile(zipFile);
		Enumeration e = zipArchive.entries();
		archiveReadMeEntry = (ZipEntry) e.nextElement();
		archiveInfo = new ArchiveInfo( zipArchive.getInputStream(archiveReadMeEntry));

		//load
		HashMap<String, ArrayList<DataRange>> map = new HashMap<String,ArrayList<DataRange>> ();

		while(e.hasMoreElements()) {
			ZipEntry zipEntry = (ZipEntry) e.nextElement();
			SliceInfo sliceInfo = new SliceInfo(zipEntry.getName());
			//get chromStrand and ranges
			String chromName;
			if (maintainStrandedness) chromName = sliceInfo.getChromosome()+sliceInfo.getStrand();
			else chromName = sliceInfo.getChromosome();
			//get/make ArrayList
			ArrayList<DataRange> al = map.get(chromName);
			if (al == null){
				al = new ArrayList<DataRange>();
				map.put(chromName, al);
			}
			al.add(new DataRange(zipEntry,sliceInfo.getFirstStartPosition(), sliceInfo.getLastStartPosition()));

		}
		//convert to arrays and sort
		Iterator<String> it = map.keySet().iterator();
		while (it.hasNext()){
			String chromName = it.next();
			ArrayList<DataRange> al = map.get(chromName);
			DataRange[] dr = new DataRange[al.size()];
			al.toArray(dr);
			Arrays.sort(dr);
			chromStrandRegions.put(chromName, dr);
		}
	}

	private class DataRange implements Comparable<DataRange>{
		ZipEntry zipEntry;
		int beginningBP;
		int endingBP;

		public DataRange (ZipEntry zipEntry, int beginningBP, int endingBP){
			this.zipEntry = zipEntry;
			this.beginningBP = beginningBP;
			this.endingBP = endingBP;
		}

		public boolean intersects (int start, int stop){
			if (stop <= beginningBP || start >= endingBP) return false;
			return true;
		}

		/**Sorts by beginningBP, smaller to larger.*/
		public int compareTo(DataRange other){
			if (beginningBP < other.beginningBP) return -1;
			if (beginningBP > other.beginningBP) return 1;
			return 0;
		}
	}

	//alphabet minus 28 abiguous characters
	public static String[] nonAmbiguousLetters = {"A","B","C","D","E","F","G","H","J","K","L","M","N",
		"P","Q","R","T","U","V","W","X","Y","3","4","6","7","8","9"};		

	/**Creates pseudorandom Strings derived from an alphabet of String[] using the
	 * java.util.Random class.  Indicate how long you want a particular word and
	 * the number of words.*/
	public static String[] createRandomWords(String[] alphabet,int lengthOfWord,int numberOfWords) {
		ArrayList<String> words = new ArrayList<String>();
		Random r = new Random();
		int len = alphabet.length;
		for (int i = 0; i < numberOfWords; i++) {
			StringBuffer w = new StringBuffer();
			for (int j = 0; j < lengthOfWord; j++) {
				w.append(alphabet[r.nextInt(len)]);
			}
			words.add(w.toString());
		}
		String[] w = new String[words.size()];
		words.toArray(w);
		return w;
	}

	/**Returns a random word using nonambiguous alphabet.  Don't use this method for creating more than one word!*/
	public static String createRandowWord(int lengthOfWord){
		return createRandomWords(nonAmbiguousLetters, lengthOfWord,1)[0];
	}
}
