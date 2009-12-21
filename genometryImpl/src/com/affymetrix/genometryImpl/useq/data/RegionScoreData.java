package com.affymetrix.genometryImpl.useq.data;

import java.io.*;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.affymetrix.genometryImpl.useq.*;

/**Container for a sorted RegionScore[].
* @author david.nix@hci.utah.edu*/
public class RegionScoreData extends USeqData{

	//fields
	private RegionScore[] sortedRegionScores;

	//constructors
	public RegionScoreData(){}

	/**Note, be sure to sort the RegionScore[].*/
	public RegionScoreData(RegionScore[] sortedRegionScores, SliceInfo sliceInfo){
		this.sortedRegionScores = sortedRegionScores;
		this.sliceInfo = sliceInfo;
	}
	public RegionScoreData(File binaryFile) throws IOException{
		sliceInfo = new SliceInfo(binaryFile.getName());
		read (binaryFile);
	}
	public RegionScoreData(DataInputStream dis, SliceInfo sliceInfo) throws IOException{
		this.sliceInfo = sliceInfo;
		read (dis);
	}
	
	//methods
	/**Updates the SliceInfo setting just the FirstStartPosition, LastStartPosition, and NumberRecords.*/
	public static void updateSliceInfo (RegionScore[] sortedRegionScores, SliceInfo sliceInfo){
		sliceInfo.setFirstStartPosition(sortedRegionScores[0].getStart());
		sliceInfo.setLastStartPosition(sortedRegionScores[sortedRegionScores.length-1].start);
		sliceInfo.setNumberRecords(sortedRegionScores.length);
	}

	/**Writes six column xxx.bed formatted lines to the PrintWriter*/
	public void writeBed (PrintWriter out){
		String chrom = sliceInfo.getChromosome();
		String strand = sliceInfo.getStrand();
		for (int i=0; i< sortedRegionScores.length; i++){
			//chrom start stop name score strand
			out.println(chrom+"\t"+sortedRegionScores[i].start+"\t"+sortedRegionScores[i].stop+"\t"+".\t"+ sortedRegionScores[i].score +"\t"+strand);
		}
	}
	/**Writes the RegionScore[] to a binary file.  Each region's start/stop is converted to a running offset/length which are written as either ints or shorts.
	 * @param saveDirectory, the binary file will be written using the chromStrandStartBP-StopBP.extension notation to this directory
	 * @param attemptToSaveAsShort, scans to see if the offsets and region lengths exceed 65536 bp, a bit slower to write but potentially a considerable size reduction, set to false for max speed
	 * @return the binaryFile written to the saveDirectory
	 * */
	public File write (File saveDirectory, boolean attemptToSaveAsShort) throws IOException{
		//check to see if this can be saved using shorts instead of ints?
		boolean useShortBeginning = false;
		boolean useShortLength = false;
		if (attemptToSaveAsShort){			
			int bp = sortedRegionScores[0].start;
			useShortBeginning = true;
			for (int i=1; i< sortedRegionScores.length; i++){
				int currentStart = sortedRegionScores[i].start;
				int diff = currentStart - bp;
				if (diff > 65536) {
					useShortBeginning = false;
					break;
				}
				bp = currentStart;
			}
			//check to short length
			useShortLength = true;
			for (int i=0; i< sortedRegionScores.length; i++){
				int diff = sortedRegionScores[i].stop - sortedRegionScores[i].start;
				if (diff > 65536) {
					useShortLength = false;
					break;
				}
			}
		}

		//make and put file type/extension in header
		String fileType;
		if (useShortBeginning) fileType = USeqUtilities.SHORT;
		else fileType = USeqUtilities.INT;
		if (useShortLength) fileType = fileType+ USeqUtilities.SHORT;
		else fileType = fileType+ USeqUtilities.INT;
		fileType = fileType+ USeqUtilities.FLOAT;
		sliceInfo.setBinaryType(fileType);
		binaryFile = new File(saveDirectory, sliceInfo.getSliceName());
		
		//make IO
		FileOutputStream workingFOS = new FileOutputStream(binaryFile);
		DataOutputStream workingDOS = new DataOutputStream( new BufferedOutputStream (workingFOS));

		//write String header, currently this isn't used
		workingDOS.writeUTF(header);

		//write first position, always an int
		workingDOS.writeInt(sortedRegionScores[0].start);

		//write short position?
		int bp = sortedRegionScores[0].start;
		if (useShortBeginning) {			
			//also short length?
			//no
			if (useShortLength == false){
				//write first record's length
				workingDOS.writeInt(sortedRegionScores[0].stop- sortedRegionScores[0].start);
				workingDOS.writeFloat(sortedRegionScores[0].score);
				for (int i=1; i< sortedRegionScores.length; i++){
					int currentStart = sortedRegionScores[i].start;
					//subtract 32768 to extend range of short (-32768 to 32768)
					int diff = currentStart - bp - 32768;
					workingDOS.writeShort((short)(diff));
					workingDOS.writeInt(sortedRegionScores[i].stop- sortedRegionScores[i].start);
					workingDOS.writeFloat(sortedRegionScores[i].score);
					bp = currentStart;
				}
			}
			//yes short length
			else {
				//write first record's length, subtracting 32768 to extent the range of the signed short
				workingDOS.writeShort((short)(sortedRegionScores[0].stop- sortedRegionScores[0].start - 32768));
				workingDOS.writeFloat(sortedRegionScores[0].score);
				for (int i=1; i< sortedRegionScores.length; i++){
					int currentStart = sortedRegionScores[i].start;
					//subtract 32768 to extend range of short (-32768 to 32768)
					int diff = currentStart - bp - 32768;
					workingDOS.writeShort((short)(diff));
					workingDOS.writeShort((short)(sortedRegionScores[i].stop- sortedRegionScores[i].start - 32768));
					workingDOS.writeFloat(sortedRegionScores[i].score);
					bp = currentStart;
				}
			}
		}

		//no, write int for position
		else {
			//short length? no
			if (useShortLength == false){
				//write first record's length
				workingDOS.writeInt(sortedRegionScores[0].stop- sortedRegionScores[0].start);
				workingDOS.writeFloat(sortedRegionScores[0].score);
				for (int i=1; i< sortedRegionScores.length; i++){
					int currentStart = sortedRegionScores[i].start;
					int diff = currentStart - bp;
					workingDOS.writeInt(diff);
					workingDOS.writeInt(sortedRegionScores[i].stop- sortedRegionScores[i].start);
					workingDOS.writeFloat(sortedRegionScores[i].score);
					bp = currentStart;
				}
			}
			//yes
			else {
				//write first record's length
				workingDOS.writeShort((short)(sortedRegionScores[0].stop- sortedRegionScores[0].start - 32768));
				workingDOS.writeFloat(sortedRegionScores[0].score);
				for (int i=1; i< sortedRegionScores.length; i++){
					int currentStart = sortedRegionScores[i].start;
					int diff = currentStart - bp;
					workingDOS.writeInt(diff);
					workingDOS.writeShort((short)(sortedRegionScores[i].stop- sortedRegionScores[i].start - 32768));
					workingDOS.writeFloat(sortedRegionScores[i].score);
					bp = currentStart;
				}
			}
		}
		//close IO
		workingDOS.close();
		workingFOS.close();
		return binaryFile;
	}
	
	/**Writes the Region[] to a ZipOutputStream.
	 * @param	attemptToSaveAsShort	if true, scans to see if the offsets exceed 65536 bp, a bit slower to write but potentially a considerable size reduction, set to false for max speed
	 * */
	public void write (ZipOutputStream out, boolean attemptToSaveAsShort) throws IOException{
		//check to see if this can be saved using shorts instead of ints?
		boolean useShortBeginning = false;
		boolean useShortLength = false;
		if (attemptToSaveAsShort){			
			int bp = sortedRegionScores[0].start;
			useShortBeginning = true;
			for (int i=1; i< sortedRegionScores.length; i++){
				int currentStart = sortedRegionScores[i].start;
				int diff = currentStart - bp;
				if (diff > 65536) {
					useShortBeginning = false;
					break;
				}
				bp = currentStart;
			}
			//check to short length
			useShortLength = true;
			for (int i=0; i< sortedRegionScores.length; i++){
				int diff = sortedRegionScores[i].stop - sortedRegionScores[i].start;
				if (diff > 65536) {
					useShortLength = false;
					break;
				}
			}
		}

		//make and put file type/extension in header
		String fileType;
		if (useShortBeginning) fileType = USeqUtilities.SHORT;
		else fileType = USeqUtilities.INT;
		if (useShortLength) fileType = fileType+ USeqUtilities.SHORT;
		else fileType = fileType+ USeqUtilities.INT;
		fileType = fileType+ USeqUtilities.FLOAT;
		sliceInfo.setBinaryType(fileType);
		binaryFile = null;
		
		//make new ZipEntry
		out.putNextEntry(new ZipEntry(sliceInfo.getSliceName()));
		
		//make IO
		DataOutputStream workingDOS = new DataOutputStream(out);
		
		//write String header, currently this isn't used
		workingDOS.writeUTF(header);

		//write first position, always an int
		workingDOS.writeInt(sortedRegionScores[0].start);

		//write short position?
		int bp = sortedRegionScores[0].start;
		if (useShortBeginning) {			
			//also short length?
			//no
			if (useShortLength == false){
				//write first record's length
				workingDOS.writeInt(sortedRegionScores[0].stop- sortedRegionScores[0].start);
				workingDOS.writeFloat(sortedRegionScores[0].score);
				for (int i=1; i< sortedRegionScores.length; i++){
					int currentStart = sortedRegionScores[i].start;
					//subtract 32768 to extend range of short (-32768 to 32768)
					int diff = currentStart - bp - 32768;
					workingDOS.writeShort((short)(diff));
					workingDOS.writeInt(sortedRegionScores[i].stop- sortedRegionScores[i].start);
					workingDOS.writeFloat(sortedRegionScores[i].score);
					bp = currentStart;
				}
			}
			//yes short length
			else {
				//write first record's length, subtracting 32768 to extent the range of the signed short
				workingDOS.writeShort((short)(sortedRegionScores[0].stop- sortedRegionScores[0].start - 32768));
				workingDOS.writeFloat(sortedRegionScores[0].score);
				for (int i=1; i< sortedRegionScores.length; i++){
					int currentStart = sortedRegionScores[i].start;
					//subtract 32768 to extend range of short (-32768 to 32768)
					int diff = currentStart - bp - 32768;
					workingDOS.writeShort((short)(diff));
					workingDOS.writeShort((short)(sortedRegionScores[i].stop- sortedRegionScores[i].start - 32768));
					workingDOS.writeFloat(sortedRegionScores[i].score);
					bp = currentStart;
				}
			}
		}

		//no, write int for position
		else {
			//short length? no
			if (useShortLength == false){
				//write first record's length
				workingDOS.writeInt(sortedRegionScores[0].stop- sortedRegionScores[0].start);
				workingDOS.writeFloat(sortedRegionScores[0].score);
				for (int i=1; i< sortedRegionScores.length; i++){
					int currentStart = sortedRegionScores[i].start;
					int diff = currentStart - bp;
					workingDOS.writeInt(diff);
					workingDOS.writeInt(sortedRegionScores[i].stop- sortedRegionScores[i].start);
					workingDOS.writeFloat(sortedRegionScores[i].score);
					bp = currentStart;
				}
			}
			//yes
			else {
				//write first record's length
				workingDOS.writeShort((short)(sortedRegionScores[0].stop- sortedRegionScores[0].start - 32768));
				workingDOS.writeFloat(sortedRegionScores[0].score);
				for (int i=1; i< sortedRegionScores.length; i++){
					int currentStart = sortedRegionScores[i].start;
					int diff = currentStart - bp;
					workingDOS.writeInt(diff);
					workingDOS.writeShort((short)(sortedRegionScores[i].stop- sortedRegionScores[i].start - 32768));
					workingDOS.writeFloat(sortedRegionScores[i].score);
					bp = currentStart;
				}
			}
		}
		//close ZipEntry
		out.closeEntry();
	}

	/**Reads a binary file into this RegionScoreData.*/
	private void read (File binaryFile) throws IOException{
		//open IO
		this.binaryFile = binaryFile;
		FileInputStream workingFIS = new FileInputStream(binaryFile);
		DataInputStream workingDIS = new DataInputStream( new BufferedInputStream(workingFIS ));
		read(workingDIS);
		//close IO
		workingFIS.close();
		workingDIS.close();
	}
	
	/**Reads a DataInputStream into this RegionScoreData.*/
	private void read (DataInputStream dis) throws IOException{
		//read text header, currently not used
		header = dis.readUTF();

		//make array
		int numberRegionScores = sliceInfo.getNumberRecords();
		sortedRegionScores = new RegionScore[numberRegionScores];
		
		//what kind of data to follow? 
		String fileType = sliceInfo.getBinaryType();
	
		//int Position, int Length
		if (USeqUtilities.REGION_SCORE_INT_INT_FLOAT.matcher(fileType).matches()){
			//make first RegionScore, position is always an int
			int start = dis.readInt();
			sortedRegionScores[0] = new RegionScore(start, start+dis.readInt(), dis.readFloat());
			//read and resolve offsets to real bps and length to stop
			for (int i=1; i< numberRegionScores; i++){
				start = sortedRegionScores[i-1].start + dis.readInt();
				sortedRegionScores[i] = new RegionScore(start, start + dis.readInt(), dis.readFloat());	
			}
		}
		//int Position, short Length
		else if (USeqUtilities.REGION_SCORE_INT_SHORT_FLOAT.matcher(fileType).matches()){
			//make first RegionScore, position is always an int
			int start = dis.readInt();
			sortedRegionScores[0] = new RegionScore(start, start+ dis.readShort() + 32768, dis.readFloat());
			//read and resolve offsets to real bps and length to stop
			for (int i=1; i< numberRegionScores; i++){
				start = sortedRegionScores[i-1].start + dis.readInt();
				sortedRegionScores[i] = new RegionScore(start, start + dis.readShort() + 32768, dis.readFloat());	
			}
		}
		//short Postion, short Length
		else if (USeqUtilities.REGION_SCORE_SHORT_SHORT_FLOAT.matcher(fileType).matches()){
			//make first RegionScore, position is always an int
			int start = dis.readInt();
			sortedRegionScores[0] = new RegionScore(start, start+ dis.readShort() + 32768, dis.readFloat());
			//read and resolve offsets to real bps and length to stop
			for (int i=1; i< numberRegionScores; i++){
				start = sortedRegionScores[i-1].start + dis.readShort() + 32768;
				sortedRegionScores[i] = new RegionScore(start, start + dis.readShort() + 32768, dis.readFloat());
			}
		}
		//short Position, int Length
		else if (USeqUtilities.REGION_SCORE_SHORT_INT_FLOAT.matcher(fileType).matches()){
			//make first RegionScore, position is always an int
			int start = dis.readInt();
			sortedRegionScores[0] = new RegionScore(start, start+ dis.readInt(), dis.readFloat());
			//read and resolve offsets to real bps and length to stop
			for (int i=1; i< numberRegionScores; i++){
				start = sortedRegionScores[i-1].start + dis.readShort() + 32768;
				sortedRegionScores[i] = new RegionScore(start, start + dis.readInt(), dis.readFloat());	
			}
		}
		//unknown!
		else {
			throw new IOException ("Incorrect file type for creating a RegionScore[] -> '"+fileType+"' in "+binaryFile +"\n");
		}
	}

	public RegionScore[] getRegionScores() {
		return sortedRegionScores;
	}

	public void setRegionScores(RegionScore[] sortedRegionScores) {
		this.sortedRegionScores = sortedRegionScores;
		updateSliceInfo(sortedRegionScores, sliceInfo);
	}

	/**Returns whether data remains.*/
	public boolean trim(int beginningBP, int endingBP) {
		ArrayList<RegionScore> al = new ArrayList<RegionScore>();
		for (int i=0; i< sortedRegionScores.length; i++){
			if (sortedRegionScores[i].isContainedBy(beginningBP, endingBP)) al.add(sortedRegionScores[i]);
		}
		if (al.size() == 0) return false;
		sortedRegionScores = new RegionScore[al.size()];
		al.toArray(sortedRegionScores);
		updateSliceInfo(sortedRegionScores, sliceInfo);
		return true;
	}
}
