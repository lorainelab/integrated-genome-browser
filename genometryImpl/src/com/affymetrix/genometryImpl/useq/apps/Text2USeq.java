package com.affymetrix.genometryImpl.useq.apps;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.affymetrix.genometryImpl.useq.*;
import com.affymetrix.genometryImpl.useq.data.*;

/**Splits a tab delimited text file by chromosome (and optionally strand), sort on position, then divides it by the number of rows.
 * Writes to a directory named after the input file, minus the extension, capitalized first letter using a chrStrndStartBP-StopBP.useries extension. (e.g. chr5_Random+294383948-294393948.us1) */
public class Text2USeq {
	//fields
	//user defined
	private int chromosomeColumnIndex = -1;
	private int strandColumnIndex = -1;
	private int beginningColumnIndex = -1;
	private int endingColumnIndex = -1;
	private int textColumnIndex = -1;
	private int scoreColumnIndex = -1;
	private int rowChunkSize = 10000;
	private File[] inputFiles;
	private String versionedGenome = null;
	private boolean makeGraph = true;
	private int graphStyle = 0;
	private String color = null;
	private String description = null;

	//internal fields
	private static String[] graphStyles = {ArchiveInfo.GRAPH_STYLE_VALUE_BAR, ArchiveInfo.GRAPH_STYLE_VALUE_STAIRSTEP, ArchiveInfo.GRAPH_STYLE_VALUE_HEATMAP, ArchiveInfo.GRAPH_STYLE_VALUE_LINE};

	private int maxIndex;
	private File[] outputDirectories;
	private File workingSaveDirectory;
	private TreeMap<String,PositionStringArray[]> chromStrandPositionStringArray;
	private ArrayList<File> files2Zip = new ArrayList<File>();
	private int numberLines = 0;
	private StringBuilder comments = null;
	public static final Pattern PATTERN_TAB = Pattern.compile("\\t");
	public static final Pattern PATTERN_STRAND = Pattern.compile("[+-\\.]");

	//constructors
	//for use with main, contains System.exit calls!
	public Text2USeq(String[] args){
		long startTime = System.currentTimeMillis();
		processArgs(args);

		//for each file
		for (int i=0; i< inputFiles.length; i++){
			System.out.println("Processing "+inputFiles[i]);

			//split text file by chromStrand
			System.out.println("\tSplitting by chromosome and possibly strand...");
			chromStrandPositionStringArray = parseDataFile(inputFiles[i]);
			if (chromStrandPositionStringArray == null){
				USeqUtilities.printErrAndExit("\nFailed to parse genomic data text file, aborting!\n");
			}

			//Make directory to hold split files
			outputDirectories[i] = USeqUtilities.makeDirectory(inputFiles[i], ".TempDelMe");
			workingSaveDirectory = outputDirectories[i];

			//clear files to zip
			files2Zip.clear();

			//write readme.txt 
			writeReadMeTxt(inputFiles[i]);

			//split slice and write data to binary file
			System.out.println("\tParsing, slicing, and writing binary data...");
			if (sliceWriteSplitData() == false){
				USeqUtilities.deleteDirectory(workingSaveDirectory);
				USeqUtilities.printErrAndExit("\nFailed to convert split data to binary, aborting!\n");
			}

			//zip compress directory
			System.out.println("\tZipping...");
			String zipName = USeqUtilities.removeExtension( workingSaveDirectory.getName()) + "."+USeqUtilities.USEQ_EXTENSION_NO_PERIOD;
			File zipFile = new File (inputFiles[i].getParentFile(), zipName);
			File[] files = new File[files2Zip.size()];
			files2Zip.toArray(files);
			USeqUtilities.zip(files, zipFile);
			USeqUtilities.deleteDirectory(workingSaveDirectory);
		}
		//finish and calc run time
		double diffTime = ((double)(System.currentTimeMillis() -startTime))/1000;
		System.out.println("\nDone! "+Math.round(diffTime)+" seconds\n");
	}

	//methods
	private void writeReadMeTxt(File sourceFile){
		try {
			ArchiveInfo ai = new ArchiveInfo(versionedGenome, null);
			//set data type, graph or region
			if (makeGraph) {
				ai.setDataType(ArchiveInfo.DATA_TYPE_VALUE_GRAPH);
				ai.setInitialGraphStyle(graphStyles[graphStyle]);
			}
			else ai.setDataType(ArchiveInfo.DATA_TYPE_VALUE_REGION);
			//set text file source
			ai.setOriginatingDataSource(sourceFile.toString());
			//set color
			if (color != null) ai.setInitialColor(color);
			//set description?
			if (description != null) ai.setDescription(description);
			//write
			File readme = ai.writeReadMeFile(workingSaveDirectory);
			files2Zip.add(readme);
		} catch (IOException e){
			e.printStackTrace();
		}
	}

	/**Calls the appropriate slice writer on the working PositionScoreText[]*/
	private boolean sliceWriteSplitData(){
		try {
			//Region or Position data
			if (endingColumnIndex == -1){
				//Position!
				if (scoreColumnIndex == -1){
					if (textColumnIndex == -1) sliceWritePositionData();
					else sliceWritePositionTextData();
				}
				else {
					if (textColumnIndex == -1) sliceWritePositionScoreData();
					else sliceWritePositionScoreTextData();
				}
			}
			else {
				//Region
				if (scoreColumnIndex == -1){
					if (textColumnIndex == -1) sliceWriteRegionData();
					else sliceWriteRegionTextData();
				}
				else {
					if (textColumnIndex == -1) sliceWriteRegionScoreData();
					else sliceWriteRegionScoreTextData();
				}

			}
		} catch (Exception e){
			System.err.println("Error slicing and writing data!");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**Split chroms by the rowChunkSize and writes each to file using an appropriate binary file type.*/
	private void sliceWriteRegionData () throws Exception{
		Iterator<String> it = chromStrandPositionStringArray.keySet().iterator();
		while (it.hasNext()){
			String chromStrand = it.next();
			String chromosome = chromStrand.substring(0, chromStrand.length()-1);
			String strand = chromStrand.substring(chromStrand.length()-1);
			SliceInfo sliceInfo = new SliceInfo(chromosome, strand,0,0,0,null);
			int beginningIndex = 0;
			int endIndex = 0;
			Region[] reg = makeRegions(chromStrandPositionStringArray.get(chromStrand));
			int numberReg = reg.length;
			while (true){
				//find beginningIndex and endIndex(excluded) indexes
				Region[] slice;
				//don't slice?
				if (rowChunkSize == -1){
					beginningIndex =0;
					endIndex = numberReg;
					slice = reg;
				}
				//slice!
				else {
					beginningIndex = endIndex;
					endIndex = beginningIndex + rowChunkSize;
					if (endIndex > numberReg) {
						endIndex = numberReg;
					}
					else {
						//advance until start changes
						int endBP = reg[endIndex-1].getStart();
						for (int i=endIndex; i< numberReg; i++){
							if (reg[i].getStart() != endBP){
								break;
							}
							endIndex++;
						}
					}
					int num = endIndex - beginningIndex;
					slice = new Region[num];
					System.arraycopy(reg, beginningIndex, slice, 0, num);
				}
				//update slice info
				RegionData.updateSliceInfo(slice, sliceInfo);
				RegionData rd = new RegionData (slice, sliceInfo);
				File savedFile = rd.write(workingSaveDirectory, true);
				files2Zip.add(savedFile);
				//at the end of the data?
				if (endIndex == numberReg) break;
			}
		}
	}	

	/**Split chroms by the rowChunkSize and writes each to file using an appropriate binary file type.*/
	private void sliceWriteRegionScoreData () throws Exception{
		Iterator<String> it = chromStrandPositionStringArray.keySet().iterator();
		while (it.hasNext()){
			String chromStrand = it.next();
			String chromosome = chromStrand.substring(0, chromStrand.length()-1);
			String strand = chromStrand.substring(chromStrand.length()-1);
			SliceInfo sliceInfo = new SliceInfo(chromosome, strand,0,0,0,null);
			int beginningIndex = 0;
			int endIndex = 0;
			RegionScore[] reg = makeRegionScores(chromStrandPositionStringArray.get(chromStrand));
			int numberReg = reg.length;
			while (true){
				//find beginningIndex and endIndex(excluded) indexes
				RegionScore[] slice;
				//don't slice?
				if (rowChunkSize == -1){
					beginningIndex =0;
					endIndex = numberReg;
					slice = reg;
				}
				//slice!
				else {
					beginningIndex = endIndex;
					endIndex = beginningIndex + rowChunkSize;
					if (endIndex > numberReg) {
						endIndex = numberReg;
					}
					else {
						//advance until start changes
						int endBP = reg[endIndex-1].getStart();
						for (int i=endIndex; i< numberReg; i++){
							if (reg[i].getStart() != endBP){
								break;
							}
							endIndex++;
						}
					}
					int num = endIndex - beginningIndex;
					slice = new RegionScore[num];
					System.arraycopy(reg, beginningIndex, slice, 0, num);
				}
				//update slice info
				RegionScoreData.updateSliceInfo(slice, sliceInfo);
				RegionScoreData rd = new RegionScoreData (slice, sliceInfo);
				File savedFile = rd.write(workingSaveDirectory, true);
				files2Zip.add(savedFile);
				//at the end of the data?
				if (endIndex == numberReg) break;
			}
		}
	}

	/**Split chroms by the rowChunkSize and writes each to file using an appropriate binary file type.*/
	private void sliceWriteRegionScoreTextData () throws Exception{
		Iterator<String> it = chromStrandPositionStringArray.keySet().iterator();
		while (it.hasNext()){
			String chromStrand = it.next();
			String chromosome = chromStrand.substring(0, chromStrand.length()-1);
			String strand = chromStrand.substring(chromStrand.length()-1);
			SliceInfo sliceInfo = new SliceInfo(chromosome, strand,0,0,0,null);
			int beginningIndex = 0;
			int endIndex = 0;
			RegionScoreText[] reg = makeRegionScoreTexts(chromStrandPositionStringArray.get(chromStrand));
			int numberReg = reg.length;
			while (true){
				//find beginningIndex and endIndex(excluded) indexes
				RegionScoreText[] slice;
				//don't slice?
				if (rowChunkSize == -1){
					beginningIndex =0;
					endIndex = numberReg;
					slice = reg;
				}
				//slice!
				else {
					beginningIndex = endIndex;
					endIndex = beginningIndex + rowChunkSize;
					if (endIndex > numberReg) {
						endIndex = numberReg;
					}
					else {
						//advance until start changes
						int endBP = reg[endIndex-1].getStart();
						for (int i=endIndex; i< numberReg; i++){
							if (reg[i].getStart() != endBP){
								break;
							}
							endIndex++;
						}
					}
					int num = endIndex - beginningIndex;
					slice = new RegionScoreText[num];
					System.arraycopy(reg, beginningIndex, slice, 0, num);
				}
				//update slice info
				RegionScoreTextData.updateSliceInfo(slice, sliceInfo);
				RegionScoreTextData rd = new RegionScoreTextData (slice, sliceInfo);
				File savedFile = rd.write(workingSaveDirectory, true);
				files2Zip.add(savedFile);
				//at the end of the data?
				if (endIndex == numberReg) break;
			}
		}
	}

	/**Split chroms by the rowChunkSize and writes each to file using an appropriate binary file type.*/
	private void sliceWriteRegionTextData () throws Exception{
		Iterator<String> it = chromStrandPositionStringArray.keySet().iterator();
		while (it.hasNext()){
			String chromStrand = it.next();
			String chromosome = chromStrand.substring(0, chromStrand.length()-1);
			String strand = chromStrand.substring(chromStrand.length()-1);
			SliceInfo sliceInfo = new SliceInfo(chromosome, strand,0,0,0,null);
			int beginningIndex = 0;
			int endIndex = 0;
			RegionText[] reg = makeRegionTexts(chromStrandPositionStringArray.get(chromStrand));
			int numberReg = reg.length;
			while (true){
				//find beginningIndex and endIndex(excluded) indexes
				RegionText[] slice;
				//don't slice?
				if (rowChunkSize == -1){
					beginningIndex =0;
					endIndex = numberReg;
					slice = reg;
				}
				//slice!
				else {
					beginningIndex = endIndex;
					endIndex = beginningIndex + rowChunkSize;
					if (endIndex > numberReg) {
						endIndex = numberReg;
					}
					else {
						//advance until start changes
						int endBP = reg[endIndex-1].getStart();
						for (int i=endIndex; i< numberReg; i++){
							if (reg[i].getStart() != endBP){
								break;
							}
							endIndex++;
						}
					}
					int num = endIndex - beginningIndex;
					slice = new RegionText[num];
					System.arraycopy(reg, beginningIndex, slice, 0, num);
				}
				//update slice info
				RegionTextData.updateSliceInfo(slice, sliceInfo);
				RegionTextData rd = new RegionTextData (slice, sliceInfo);
				File savedFile = rd.write(workingSaveDirectory, true);
				files2Zip.add(savedFile);
				//at the end of the data?
				if (endIndex == numberReg) break;
			}
		}
	}

	/**Split chroms by the rowChunkSize and writes each to file using an appropriate binary file type.*/
	private void sliceWritePositionData () throws Exception{
		Iterator<String> it = chromStrandPositionStringArray.keySet().iterator();
		while (it.hasNext()){
			String chromStrand = it.next();
			String chromosome = chromStrand.substring(0, chromStrand.length()-1);
			String strand = chromStrand.substring(chromStrand.length()-1);
			SliceInfo sliceInfo = new SliceInfo(chromosome, strand,0,0,0,null);
			int beginningIndex = 0;
			int endIndex = 0;

			Position[] positions = makePositions(chromStrandPositionStringArray.get(chromStrand));
			int numberPositions = positions.length;
			while (true){
				//find beginningIndex and endIndex(excluded) indexes
				Position[] slice;
				//don't slice?
				if (rowChunkSize == -1){
					beginningIndex =0;
					endIndex = numberPositions;
					slice = positions;
				}
				//slice!
				else {
					beginningIndex = endIndex;
					endIndex = beginningIndex + rowChunkSize;
					if (endIndex > numberPositions) {
						endIndex = numberPositions;
					}
					else {
						//advance until position changes
						int endBP = positions[endIndex-1].getPosition();
						for (int i=endIndex; i< numberPositions; i++){
							if (positions[i].getPosition() != endBP){
								break;
							}
							endIndex++;
						}
					}
					int num = endIndex - beginningIndex;
					slice = new Position[num];
					System.arraycopy(positions, beginningIndex, slice, 0, num);
				}
				//update slice info
				PositionData.updateSliceInfo(slice, sliceInfo);
				PositionData pd = new PositionData (slice, sliceInfo);
				File savedFile = pd.write(workingSaveDirectory, true);
				files2Zip.add(savedFile);
				//at the end of the data?
				if (endIndex == numberPositions) break;
			}
		}
	}	

	/**Split chroms by the rowChunkSize and writes each to file using an appropriate binary file type.*/
	private void sliceWritePositionTextData () throws Exception{
		Iterator<String> it = chromStrandPositionStringArray.keySet().iterator();
		while (it.hasNext()){
			String chromStrand = it.next();
			String chromosome = chromStrand.substring(0, chromStrand.length()-1);
			String strand = chromStrand.substring(chromStrand.length()-1);
			SliceInfo sliceInfo = new SliceInfo(chromosome, strand,0,0,0,null);
			int beginningIndex = 0;
			int endIndex = 0;
			PositionText[] positions = makePositionTexts(chromStrandPositionStringArray.get(chromStrand));
			int numberPositions = positions.length;
			while (true){
				//find beginningIndex and endIndex(excluded) indexes
				PositionText[] slice;
				//don't slice?
				if (rowChunkSize == -1){
					beginningIndex =0;
					endIndex = numberPositions;
					slice = positions;
				}
				//slice!
				else {
					beginningIndex = endIndex;
					endIndex = beginningIndex + rowChunkSize;
					if (endIndex > numberPositions) {
						endIndex = numberPositions;
					}
					else {
						//advance until position changes
						int endBP = positions[endIndex-1].getPosition();
						for (int i=endIndex; i< numberPositions; i++){
							if (positions[i].getPosition() != endBP){
								break;
							}
							endIndex++;
						}
					}
					int num = endIndex - beginningIndex;
					slice = new PositionText[num];
					System.arraycopy(positions, beginningIndex, slice, 0, num);
				}
				//update slice info
				PositionTextData.updateSliceInfo(slice, sliceInfo);
				PositionTextData pd = new PositionTextData (slice, sliceInfo);
				File savedFile = pd.write(workingSaveDirectory, true);
				files2Zip.add(savedFile);
				//at the end of the data?
				if (endIndex == numberPositions) break;
			}
		}
	}

	/**Split chroms by the rowChunkSize and writes each to file using an appropriate binary file type.*/
	private void sliceWritePositionScoreTextData () throws Exception{
		Iterator<String> it = chromStrandPositionStringArray.keySet().iterator();
		while (it.hasNext()){
			String chromStrand = it.next();
			String chromosome = chromStrand.substring(0, chromStrand.length()-1);
			String strand = chromStrand.substring(chromStrand.length()-1);
			SliceInfo sliceInfo = new SliceInfo(chromosome, strand,0,0,0,null);
			int beginningIndex = 0;
			int endIndex = 0;
			PositionScoreText[] positions = makePositionScoreTexts(chromStrandPositionStringArray.get(chromStrand));
			int numberPositions = positions.length;
			while (true){
				//find beginningIndex and endIndex(excluded) indexes
				PositionScoreText[] slice;
				//don't slice?
				if (rowChunkSize == -1){
					beginningIndex =0;
					endIndex = numberPositions;
					slice = positions;
				}
				//slice!
				else {
					beginningIndex = endIndex;
					endIndex = beginningIndex + rowChunkSize;
					if (endIndex > numberPositions) {
						endIndex = numberPositions;
					}
					else {
						//advance until position changes
						int endBP = positions[endIndex-1].getPosition();
						for (int i=endIndex; i< numberPositions; i++){
							if (positions[i].getPosition() != endBP){
								break;
							}
							endIndex++;
						}
					}
					int num = endIndex - beginningIndex;
					slice = new PositionScoreText[num];
					System.arraycopy(positions, beginningIndex, slice, 0, num);
				}
				//update slice info
				PositionScoreTextData.updateSliceInfo(slice, sliceInfo);
				PositionScoreTextData pd = new PositionScoreTextData (slice, sliceInfo);
				File savedFile = pd.write(workingSaveDirectory, true);
				files2Zip.add(savedFile);
				//at the end of the data?
				if (endIndex == numberPositions) break;
			}
		}
	}

	/**Split chroms by the rowChunkSize and writes each to file using an appropriate binary file type.*/
	private void sliceWritePositionScoreData () throws Exception{
		Iterator<String> it = chromStrandPositionStringArray.keySet().iterator();
		while (it.hasNext()){
			String chromStrand = it.next();
			String chromosome = chromStrand.substring(0, chromStrand.length()-1);
			String strand = chromStrand.substring(chromStrand.length()-1);
			SliceInfo sliceInfo = new SliceInfo(chromosome, strand,0,0,0,null);
			int beginningIndex = 0;
			int endIndex = 0;
			PositionScore[] positions = makePositionScores(chromStrandPositionStringArray.get(chromStrand));
			int numberPositions = positions.length;
			while (true){
				//find beginningIndex and endIndex(excluded) indexes
				PositionScore[] slice;
				//don't slice?
				if (rowChunkSize == -1){
					beginningIndex =0;
					endIndex = numberPositions;
					slice = positions;
				}
				//slice!
				else {
					beginningIndex = endIndex;
					endIndex = beginningIndex + rowChunkSize;
					if (endIndex > numberPositions) {
						endIndex = numberPositions;
					}
					else {
						//advance until position changes
						int endBP = positions[endIndex-1].getPosition();
						for (int i=endIndex; i< numberPositions; i++){
							if (positions[i].getPosition() != endBP){
								break;
							}
							endIndex++;
						}
					}
					int num = endIndex - beginningIndex;
					slice = new PositionScore[num];
					System.arraycopy(positions, beginningIndex, slice, 0, num);
				}
				//update slice info
				PositionScoreData.updateSliceInfo(slice, sliceInfo);
				PositionScoreData pd = new PositionScoreData (slice, sliceInfo);
				File savedFile = pd.write(workingSaveDirectory, true);
				files2Zip.add(savedFile);
				//at the end of the data?
				if (endIndex == numberPositions) break;
			}
		}

	}

	/**Parses a PostionScoreText[]*/
	private PositionScoreText[] makePositionScoreTexts(PositionStringArray[] psa){
		PositionScoreText[] pos = new PositionScoreText[psa.length];
		String[] line = null;
		try {
			for (int i=0; i< pos.length; i++){
				line = psa[i].getLine();
				pos[i] = new PositionScoreText(psa[i].getPosition(), Float.parseFloat(line[scoreColumnIndex]), line[textColumnIndex]);
			}
		} catch (Exception e){
			System.out.println("Could not parse a float value from '"+line[scoreColumnIndex]+"', malformed line -> "+USeqUtilities.stringArrayToString(line, "\t"));
			e.printStackTrace();
			pos = null;
		}
		return pos;
	}

	/**Parses a PostionText[]*/
	private PositionText[] makePositionTexts(PositionStringArray[] psa){
		PositionText[] pos = new PositionText[psa.length];
		String[] line = null;
		for (int i=0; i< pos.length; i++){
			line = psa[i].getLine();			
			pos[i] = new PositionText(psa[i].getPosition(), line[textColumnIndex]);
		}
		return pos;
	}

	/**Parses a PostionScore[]*/
	private PositionScore[] makePositionScores(PositionStringArray[] psa){
		PositionScore[] pos = new PositionScore[psa.length];
		String[] line = null;
		try {
			for (int i=0; i< pos.length; i++){
				line = psa[i].getLine();
				pos[i] = new PositionScore(psa[i].getPosition(), Float.parseFloat(line[scoreColumnIndex]));
			}
		} catch (Exception e){
			System.out.println("Could not parse a float value from '"+line[scoreColumnIndex]+"', malformed line -> "+USeqUtilities.stringArrayToString(line, "\t"));
			e.printStackTrace();
			pos = null;
		}
		return pos;
	}

	private Position[] makePositions(PositionStringArray[] psa){
		Position[] pos = new Position[psa.length];
		for (int i=0; i< pos.length; i++){
			pos[i] = new Position(psa[i].getPosition());
		}
		return pos;
	}

	/**Parses a Region[]*/
	private Region[] makeRegions(PositionStringArray[] psa){
		Region[] pos = new Region[psa.length];
		String[] line = null;
		try {
			for (int i=0; i< pos.length; i++){
				line = psa[i].getLine();
				pos[i] = new Region(psa[i].getPosition(), Integer.parseInt(line[endingColumnIndex]));
			}
		} catch (Exception e){
			System.out.println("Could not parse an int value from '"+line[endingColumnIndex]+"', malformed line -> "+USeqUtilities.stringArrayToString(line, "\t"));
			e.printStackTrace();
			pos = null;
		}
		return pos;
	}

	/**Parses a RegionScore[]*/
	private RegionScore[] makeRegionScores(PositionStringArray[] psa){
		RegionScore[] pos = new RegionScore[psa.length];
		String[] line = null;
		try {
			for (int i=0; i< pos.length; i++){
				line = psa[i].getLine();
				pos[i] = new RegionScore(psa[i].getPosition(), Integer.parseInt(line[endingColumnIndex]), Float.parseFloat(line[scoreColumnIndex]));
			}
		} catch (Exception e){
			System.out.println("Could not parse an int or float value from malformed line -> "+USeqUtilities.stringArrayToString(line, "\t"));
			e.printStackTrace();
			pos = null;
		}
		return pos;
	}

	/**Parses a RegionText[]*/
	private RegionText[] makeRegionTexts(PositionStringArray[] psa){
		RegionText[] pos = new RegionText[psa.length];
		String[] line = null;
		try {
			for (int i=0; i< pos.length; i++){
				line = psa[i].getLine();
				pos[i] = new RegionText(psa[i].getPosition(), Integer.parseInt(line[endingColumnIndex]), line[textColumnIndex]);
			}
		} catch (Exception e){
			System.out.println("Could not parse an int value from '"+line[endingColumnIndex]+"', malformed line -> "+USeqUtilities.stringArrayToString(line, "\t"));
			e.printStackTrace();
			pos = null;
		}
		return pos;
	}

	/**Parses a RegionScoreText[]*/
	private RegionScoreText[] makeRegionScoreTexts(PositionStringArray[] psa){
		RegionScoreText[] pos = new RegionScoreText[psa.length];
		String[] line = null;
		try {
			for (int i=0; i< pos.length; i++){
				line = psa[i].getLine();
				pos[i] = new RegionScoreText(psa[i].getPosition(), Integer.parseInt(line[endingColumnIndex]), Float.parseFloat(line[scoreColumnIndex]), line[textColumnIndex]);
			}
		} catch (Exception e){
			System.out.println("Could not parse an int or float value from malformed line -> "+USeqUtilities.stringArrayToString(line, "\t"));
			e.printStackTrace();
			pos = null;
		}
		return pos;
	}




	/**Splits a tab delimited genomic data file by chromosome and possibly strand returning a HashMap<chromosomeStrand, PositionStringArray[] sorted by position>.
	 * Returns null if something bad happened. Blank lines and those starting with # are ignored.*/
	public TreeMap<String,PositionStringArray[]> parseDataFile(File file){
		numberLines = 0;
		comments = new StringBuilder();
		TreeMap<String, ArrayList<PositionStringArray>> chromStrandLine = new TreeMap<String, ArrayList<PositionStringArray>>();
		int numBadLines = 0;
		String line = null;
		ArrayList<PositionStringArray> al = new ArrayList<PositionStringArray>();
		try{
			BufferedReader in = USeqUtilities.fetchBufferedReader(file);
			String[] tokens = null; 
			String currentChromStrand = "";

			while ((line = in.readLine()) !=null) {
				try{
					line = line.trim();
					if (line.length() == 0) continue;
					if (line.startsWith("#")) {
						comments.append(line);
						continue;
					}
					tokens = PATTERN_TAB.split(line);
					if (maxIndex >= tokens.length){
						System.err.println("Error: too few columns! Skipping -> \n\t"+line);
						if (numBadLines == 10) {
							System.err.println("\nToo many malformed lines, aborting!\n");
							return null;
						}
						numBadLines++;
						continue;
					}
					//make chromosome strand text
					String chromStrand;
					if (strandColumnIndex == -1) chromStrand = tokens[chromosomeColumnIndex]+".";
					else {
						//check strand, should be +,-, or .
						if (PATTERN_STRAND.matcher(tokens[strandColumnIndex]).matches() == false){
							System.err.println("\nError: cannot parse strand info (+,-,or.) from "+tokens[strandColumnIndex]+", skipping this line -> "+line);
							if (numBadLines == 10) {
								System.err.println("\nToo many malformed lines, aborting!\n");
								return null;
							}
							numBadLines++;
							continue;
						}
						else chromStrand = tokens[chromosomeColumnIndex]+tokens[strandColumnIndex];
					}
					//fetch ArrayList
					if (currentChromStrand != chromStrand){
						currentChromStrand = chromStrand;
						if (chromStrandLine.containsKey(currentChromStrand)) al = chromStrandLine.get(currentChromStrand);
						else {
							al = new ArrayList<PositionStringArray>(); 
							chromStrandLine.put(currentChromStrand, al);
						}
					}
					//parse position
					int position = Integer.parseInt(tokens[beginningColumnIndex]);
					al.add(new PositionStringArray(position, tokens));
				}
				catch (NumberFormatException n){
					n.printStackTrace();
					if (endingColumnIndex == -1) System.err.println("Error: cannot parse a position from "+tokens[beginningColumnIndex]+", skipping this line -> "+line);
					else System.err.println("Error: cannot parse a beginning position from "+tokens[beginningColumnIndex]+", or an ending position from "+tokens[endingColumnIndex]+", skipping this line -> "+line);
					if (numBadLines == 10) {
						System.err.println("\nToo many malformed lines, aborting!\n");
						return null;
					}
					numBadLines++;
				}
			}
		}catch (Exception e){
			e.printStackTrace();
			System.err.println("Error: aboring genomic data file sorting and splitting!\n");
			return null;
		}
		//sort and load hash
		TreeMap<String,PositionStringArray[]> chromStrandPosLine = new TreeMap<String,PositionStringArray[]>();
		Iterator<String> it = chromStrandLine.keySet().iterator();
		while (it.hasNext()){
			String cs = it.next();
			al = chromStrandLine.get(cs);
			PositionStringArray[] pl = new PositionStringArray[al.size()];
			al.toArray(pl);
			Arrays.sort(pl);
			chromStrandPosLine.put(cs, pl);
			numberLines+= pl.length;
		}
		return chromStrandPosLine;
	}

	public static void main(String[] args) {
		if (args.length ==0){
			printDocs();
			System.exit(0);
		}
		new Text2USeq(args);
	}

	/**This method will process each argument and assign new variables*/
	public void processArgs(String[] args){
		Pattern pat = Pattern.compile("-[a-z]");
		System.out.println("\nArguments: "+USeqUtilities.stringArrayToString(args, " ")+"\n");
		for (int i = 0; i<args.length; i++){
			String lcArg = args[i].toLowerCase();
			Matcher mat = pat.matcher(lcArg);
			if (mat.matches()){
				char test = args[i].charAt(1);
				try{
					switch (test){
					case 'f': inputFiles = USeqUtilities.extractFiles(new File(args[++i])); break;
					case 'b': beginningColumnIndex = Integer.parseInt(args[++i]); break;
					case 'e': endingColumnIndex = Integer.parseInt(args[++i]); break;
					case 'v': scoreColumnIndex = Integer.parseInt(args[++i]); break;
					case 't': textColumnIndex = Integer.parseInt(args[++i]); break;
					case 's': strandColumnIndex = Integer.parseInt(args[++i]); break;
					case 'c': chromosomeColumnIndex = Integer.parseInt(args[++i]); break;
					case 'i': rowChunkSize = Integer.parseInt(args[++i]); break;
					case 'g': versionedGenome = args[++i]; break;
					case 'd': description = args[++i]; break;
					case 'h': color = args[++i]; break;
					case 'r': graphStyle = Integer.parseInt(args[++i]); break;
					default: USeqUtilities.printExit("\nProblem, unknown option! " + mat.group());
					}
				}
				catch (Exception e){
					USeqUtilities.printExit("\nSorry, something doesn't look right with this parameter: -"+test+"\n");
				}
			}
		}
		//check params
		if (inputFiles == null || inputFiles.length ==0) USeqUtilities.printErrAndExit("\nCannot find your input files?\n");
		if (chromosomeColumnIndex == -1 || beginningColumnIndex == -1) USeqUtilities.printErrAndExit("\nPlease enter a chromosome and or position column indexes\n");
		if (versionedGenome == null) USeqUtilities.printErrAndExit("\nPlease enter a genome version following DAS/2 notation (e.g. H_sapiens_Mar_2006, M_musculus_Jul_2007, C_elegans_May_2008).\n");

		//instantiate outputFiles
		outputDirectories = new File[inputFiles.length];

		//find max index
		maxIndex = -1;
		if (beginningColumnIndex > maxIndex) maxIndex = beginningColumnIndex;
		if (endingColumnIndex > maxIndex) maxIndex = endingColumnIndex;
		if (scoreColumnIndex > maxIndex) maxIndex = scoreColumnIndex;
		if (textColumnIndex > maxIndex) maxIndex = textColumnIndex;
		if (strandColumnIndex > maxIndex) maxIndex = strandColumnIndex;
		if (chromosomeColumnIndex > maxIndex) maxIndex = chromosomeColumnIndex;
		if (beginningColumnIndex > maxIndex) maxIndex = beginningColumnIndex;

		//flip make graph boolean? if text or end position are provided
		if (textColumnIndex != -1 || endingColumnIndex != -1) makeGraph = false;

		//check color
		if (color !=null){
			if (ArchiveInfo.COLOR_HEX_FORM.matcher(color).matches() == false){
				USeqUtilities.printErrAndExit("\nCannot parse a hexidecimal color code (e.g. #CCFF33) from your color choice?! -> "+color);
			}
		}

	}	

	public static void printDocs(){
		StringBuilder sb = new StringBuilder();
		for (int i=0; i< graphStyles.length; i++){
			sb.append("      "+i+"\t"+graphStyles[i]+"\n");
		}
		System.out.println("\n" +
				"**************************************************************************************\n" +
				"**                               Text 2 USeq: Dec 2009                              **\n" +
				"**************************************************************************************\n" +
				"Converts text genomic data files (e.g. xxx.bed, xxx.gff, xxx.sgr, etc.) to\n" +
				"binary USeq archives (xxx.useq).  Assumes interbase coordinates. Only select\n" +
				"the columns that contain relevant information.  For example, if your data isn't\n" +
				"stranded, or you want to ignore strands, then skip the -s option.  If your data\n" +
				"doesn't have a value/ score then skip the -v option. Etc. Use the USeq2Text app to\n" +
				"convert back to text xxx.bed format. \n\n" +

				"Options:\n"+
				"-f Full path file/directory containing tab delimited genomic data files.\n" +
				"-g Genome verison using DAS notation (e.g. H_sapiens_Mar_2006, M_musculus_Jul_2007),\n" +
				"      see http://genome.ucsc.edu/FAQ/FAQreleases#release1\n"+
				"-c Chromosome column index\n" +
				"-b Position/Beginning column index\n" +
				"-s (Optional) Strand column index (+, -, or .; NOT F, R)\n" +
				"-e (Optional) End column index\n"+
				"-v (Optional) Value column index\n"+
				"-t (Optional) Text column index, only for Region data.\n"+
				"-i (Optional) Index size for slicing split chromosome data (e.g. # rows per file),\n" +
				"      defaults to 10000.\n"+
				"-r (Optional) For graphs, select a style, defaults to 0\n"+ sb+
				"-h (Optional) Color, hexadecimal (e.g. #6633FF), enclose in quotations!\n"+
				"-d (Optional) Description, enclose in quotations! \n"+


				"\nExample: java -Xmx4G -jar pathTo/USeq/Apps/GenomicDataSlicer -f\n" +
				"      /AnalysisResults/BedFiles/ -c 0 -b 1 -e 2 -i 5000 -h '#6633FF'\n" +
				"      -d 'Final processed chIP-Seq results for Bcd and Hunchback, 30M reads' \n\n" +

		"**************************************************************************************\n");

	}
	private class PositionStringArray implements Comparable{

		private int position;
		private String[] line;
		
		public PositionStringArray(int position, String[] line){
			this.position = position;
			this.line = line;
		}
		
		/**Sorts by position.*/
		public int compareTo(Object other){
			PositionStringArray se = (PositionStringArray)other;
			if (position <se.position) return -1;
			if (position>se.position) return 1;
			return 0;
		}

		public int getPosition() {
			return position;
		}

		public String[] getLine() {
			return line;
		}
	}
}
