package com.affymetrix.genometryImpl.parsers.useq.data;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.affymetrix.genometryImpl.parsers.useq.*;


/**Container for a sorted RegionScoreText[].
* @author david.nix@hci.utah.edu*/
public class RegionScoreTextData extends USeqData{

	//fields
	private RegionScoreText[] sortedRegionScoreTexts;

	//constructors
	public RegionScoreTextData(){}

	/**Note, be sure to sort the RegionScoreText[].*/
	public RegionScoreTextData(RegionScoreText[] sortedRegionScoreTexts, SliceInfo sliceInfo){
		this.sortedRegionScoreTexts = sortedRegionScoreTexts;
		this.sliceInfo = sliceInfo;
	}
	public RegionScoreTextData(File binaryFile) throws IOException{
		sliceInfo = new SliceInfo(binaryFile.getName());
		read (binaryFile);
	}
	public RegionScoreTextData(DataInputStream dis, SliceInfo sliceInfo) throws IOException{
		this.sliceInfo = sliceInfo;
		read (dis);
	}
	
	//methods
	/**Updates the SliceInfo setting just the FirstStartPosition, LastStartPosition, and NumberRecords.*/
	public static void updateSliceInfo (RegionScoreText[] sortedRegionScoreTexts, SliceInfo sliceInfo){
		sliceInfo.setFirstStartPosition(sortedRegionScoreTexts[0].getStart());
		sliceInfo.setLastStartPosition(sortedRegionScoreTexts[sortedRegionScoreTexts.length-1].start);
		sliceInfo.setNumberRecords(sortedRegionScoreTexts.length);
	}
	/**Writes six column xxx.bed formatted lines to the PrintWriter*/
	public void writeBed (PrintWriter out){
		String chrom = sliceInfo.getChromosome();
		String strand = sliceInfo.getStrand();
		for (int i=0; i< sortedRegionScoreTexts.length; i++){
			//chrom start stop name score strand
			out.println(chrom+"\t"+sortedRegionScoreTexts[i].start+"\t"+sortedRegionScoreTexts[i].stop+"\t"+sortedRegionScoreTexts[i].text +"\t"+ sortedRegionScoreTexts[i].score +"\t"+strand);
		}
	}

	/**Writes the RegionScoreText[] to a binary file.  Each region's start/stop is converted to a running offset/length which are written as either as ints or shorts.
	 * @param saveDirectory, the binary file will be written using the chromStrandStartBP-StopBP.extension notation to this directory
	 * @param attemptToSaveAsShort, scans to see if the offsets and region lengths exceed 65536 bp, a bit slower to write but potentially a considerable size reduction, set to false for max speed
	 * @return the binaryFile written to the saveDirectory
	 * */
	public File write (File saveDirectory, boolean attemptToSaveAsShort) throws IOException{
		//check to see if this can be saved using shorts instead of ints?
		boolean useShortBeginning = false;
		boolean useShortLength = false;
		if (attemptToSaveAsShort){			
			int bp = sortedRegionScoreTexts[0].start;
			useShortBeginning = true;
			for (int i=1; i< sortedRegionScoreTexts.length; i++){
				int currentStart = sortedRegionScoreTexts[i].start;
				int diff = currentStart - bp;
				if (diff > 65536) {
					useShortBeginning = false;
					break;
				}
				bp = currentStart;
			}
			//check to short lengths
			useShortLength = true;
			for (int i=0; i< sortedRegionScoreTexts.length; i++){
				int diff = sortedRegionScoreTexts[i].stop - sortedRegionScoreTexts[i].start;
				if (diff > 65536) {
					useShortLength = false;
					break;
				}
			}
		}

		//make and put file type/extension in SliceInfo object
		String fileType;
		if (useShortBeginning) fileType = USeqUtilities.SHORT;
		else fileType = USeqUtilities.INT;
		if (useShortLength) fileType = fileType+ USeqUtilities.SHORT;
		else fileType = fileType+ USeqUtilities.INT;
		fileType = fileType+ USeqUtilities.FLOAT + USeqUtilities.TEXT;
		sliceInfo.setBinaryType(fileType);
		binaryFile = new File(saveDirectory, sliceInfo.getSliceName());

		//open IO
		FileOutputStream workingFOS = new FileOutputStream(binaryFile);
		DataOutputStream workingDOS = new DataOutputStream( new BufferedOutputStream (workingFOS));

		//write String header, currently this isn't used
		workingDOS.writeUTF(header);

		//write first position, always an int
		workingDOS.writeInt(sortedRegionScoreTexts[0].start);

		//write short position?
		int bp = sortedRegionScoreTexts[0].start;
		if (useShortBeginning) {			
			//also short length?
			//no
			if (useShortLength == false){
				//write first record's length
				workingDOS.writeInt(sortedRegionScoreTexts[0].stop- sortedRegionScoreTexts[0].start);
				workingDOS.writeFloat(sortedRegionScoreTexts[0].score);
				workingDOS.writeUTF(sortedRegionScoreTexts[0].text);
				for (int i=1; i< sortedRegionScoreTexts.length; i++){
					int currentStart = sortedRegionScoreTexts[i].start;
					//subtract 32768 to extend range of short (-32768 to 32768)
					int diff = currentStart - bp - 32768;
					workingDOS.writeShort((short)(diff));
					workingDOS.writeInt(sortedRegionScoreTexts[i].stop- sortedRegionScoreTexts[i].start);
					workingDOS.writeFloat(sortedRegionScoreTexts[i].score);
					workingDOS.writeUTF(sortedRegionScoreTexts[i].text);
					bp = currentStart;
				}
			}
			//yes short length
			else {
				//write first record's length, subtracting 32768 to extent the range of the signed short
				workingDOS.writeShort((short)(sortedRegionScoreTexts[0].stop- sortedRegionScoreTexts[0].start - 32768));
				workingDOS.writeFloat(sortedRegionScoreTexts[0].score);
				workingDOS.writeUTF(sortedRegionScoreTexts[0].text);
				for (int i=1; i< sortedRegionScoreTexts.length; i++){
					int currentStart = sortedRegionScoreTexts[i].start;
					//subtract 32768 to extend range of short (-32768 to 32768)
					int diff = currentStart - bp - 32768;
					workingDOS.writeShort((short)(diff));
					workingDOS.writeShort((short)(sortedRegionScoreTexts[i].stop- sortedRegionScoreTexts[i].start - 32768));
					workingDOS.writeFloat(sortedRegionScoreTexts[i].score);
					workingDOS.writeUTF(sortedRegionScoreTexts[i].text);
					bp = currentStart;
				}
			}
		}

		//no, write int for position
		else {
			//short length? no
			if (useShortLength == false){
				//write first record's length
				workingDOS.writeInt(sortedRegionScoreTexts[0].stop- sortedRegionScoreTexts[0].start);
				workingDOS.writeFloat(sortedRegionScoreTexts[0].score);
				workingDOS.writeUTF(sortedRegionScoreTexts[0].text);
				for (int i=1; i< sortedRegionScoreTexts.length; i++){
					int currentStart = sortedRegionScoreTexts[i].start;
					int diff = currentStart - bp;
					workingDOS.writeInt(diff);
					workingDOS.writeInt(sortedRegionScoreTexts[i].stop- sortedRegionScoreTexts[i].start);
					workingDOS.writeFloat(sortedRegionScoreTexts[i].score);
					workingDOS.writeUTF(sortedRegionScoreTexts[i].text);
					bp = currentStart;
				}
			}
			//yes
			else {
				//write first record's length
				workingDOS.writeShort((short)(sortedRegionScoreTexts[0].stop- sortedRegionScoreTexts[0].start - 32768));
				workingDOS.writeFloat(sortedRegionScoreTexts[0].score);
				workingDOS.writeUTF(sortedRegionScoreTexts[0].text);
				for (int i=1; i< sortedRegionScoreTexts.length; i++){
					int currentStart = sortedRegionScoreTexts[i].start;
					int diff = currentStart - bp;
					workingDOS.writeInt(diff);
					workingDOS.writeShort((short)(sortedRegionScoreTexts[i].stop- sortedRegionScoreTexts[i].start - 32768));
					workingDOS.writeFloat(sortedRegionScoreTexts[i].score);
					workingDOS.writeUTF(sortedRegionScoreTexts[i].text);
					bp = currentStart;
				}
			}
		}
		//close IO
		workingDOS.close();
		workingFOS.close();
		return binaryFile;
	}
	
	/**Writes the RegionScoreTextData[] to a ZipOutputStream.
	 * @param	attemptToSaveAsShort	if true, scans to see if the offsets exceed 65536 bp, a bit slower to write but potentially a considerable size reduction, set to false for max speed
	 */
	public void write (ZipOutputStream out, boolean attemptToSaveAsShort) throws IOException{
		//check to see if this can be saved using shorts instead of ints?
		boolean useShortBeginning = false;
		boolean useShortLength = false;
		if (attemptToSaveAsShort){			
			int bp = sortedRegionScoreTexts[0].start;
			useShortBeginning = true;
			for (int i=1; i< sortedRegionScoreTexts.length; i++){
				int currentStart = sortedRegionScoreTexts[i].start;
				int diff = currentStart - bp;
				if (diff > 65536) {
					useShortBeginning = false;
					break;
				}
				bp = currentStart;
			}
			//check to short lengths
			useShortLength = true;
			for (int i=0; i< sortedRegionScoreTexts.length; i++){
				int diff = sortedRegionScoreTexts[i].stop - sortedRegionScoreTexts[i].start;
				if (diff > 65536) {
					useShortLength = false;
					break;
				}
			}
		}

		//make and put file type/extension in SliceInfo object
		String fileType;
		if (useShortBeginning) fileType = USeqUtilities.SHORT;
		else fileType = USeqUtilities.INT;
		if (useShortLength) fileType = fileType+ USeqUtilities.SHORT;
		else fileType = fileType+ USeqUtilities.INT;
		fileType = fileType+ USeqUtilities.FLOAT + USeqUtilities.TEXT;
		sliceInfo.setBinaryType(fileType);
		binaryFile = null;
		
		//make new ZipEntry
		out.putNextEntry(new ZipEntry(sliceInfo.getSliceName()));
		
		//open IO
		DataOutputStream workingDOS = new DataOutputStream(out);
		
		//write String header, currently this isn't used
		workingDOS.writeUTF(header);

		//write first bp position, always an int
		workingDOS.writeInt(sortedRegionScoreTexts[0].start);

		//write short position?
		int bp = sortedRegionScoreTexts[0].start;
		if (useShortBeginning) {			
			//also short length?
			//no
			if (useShortLength == false){
				//write first record's length
				workingDOS.writeInt(sortedRegionScoreTexts[0].stop- sortedRegionScoreTexts[0].start);
				workingDOS.writeFloat(sortedRegionScoreTexts[0].score);
				workingDOS.writeUTF(sortedRegionScoreTexts[0].text);
				for (int i=1; i< sortedRegionScoreTexts.length; i++){
					int currentStart = sortedRegionScoreTexts[i].start;
					//subtract 32768 to extend range of short (-32768 to 32768)
					int diff = currentStart - bp - 32768;
					workingDOS.writeShort((short)(diff));
					workingDOS.writeInt(sortedRegionScoreTexts[i].stop- sortedRegionScoreTexts[i].start);
					workingDOS.writeFloat(sortedRegionScoreTexts[i].score);
					workingDOS.writeUTF(sortedRegionScoreTexts[i].text);
					bp = currentStart;
				}
			}
			//yes short length
			else {
				//write first record's length, subtracting 32768 to extent the range of the signed short
				workingDOS.writeShort((short)(sortedRegionScoreTexts[0].stop- sortedRegionScoreTexts[0].start - 32768));
				workingDOS.writeFloat(sortedRegionScoreTexts[0].score);
				workingDOS.writeUTF(sortedRegionScoreTexts[0].text);
				for (int i=1; i< sortedRegionScoreTexts.length; i++){
					int currentStart = sortedRegionScoreTexts[i].start;
					//subtract 32768 to extend range of short (-32768 to 32768)
					int diff = currentStart - bp - 32768;
					workingDOS.writeShort((short)(diff));
					workingDOS.writeShort((short)(sortedRegionScoreTexts[i].stop- sortedRegionScoreTexts[i].start - 32768));
					workingDOS.writeFloat(sortedRegionScoreTexts[i].score);
					workingDOS.writeUTF(sortedRegionScoreTexts[i].text);
					bp = currentStart;
				}
			}
		}

		//no, write int for position
		else {
			//short length? no
			if (useShortLength == false){
				//write first record's length
				workingDOS.writeInt(sortedRegionScoreTexts[0].stop- sortedRegionScoreTexts[0].start);
				workingDOS.writeFloat(sortedRegionScoreTexts[0].score);
				workingDOS.writeUTF(sortedRegionScoreTexts[0].text);
				for (int i=1; i< sortedRegionScoreTexts.length; i++){
					int currentStart = sortedRegionScoreTexts[i].start;
					int diff = currentStart - bp;
					workingDOS.writeInt(diff);
					workingDOS.writeInt(sortedRegionScoreTexts[i].stop- sortedRegionScoreTexts[i].start);
					workingDOS.writeFloat(sortedRegionScoreTexts[i].score);
					workingDOS.writeUTF(sortedRegionScoreTexts[i].text);
					bp = currentStart;
				}
			}
			//yes
			else {
				//write first record's length
				workingDOS.writeShort((short)(sortedRegionScoreTexts[0].stop- sortedRegionScoreTexts[0].start - 32768));
				workingDOS.writeFloat(sortedRegionScoreTexts[0].score);
				workingDOS.writeUTF(sortedRegionScoreTexts[0].text);
				for (int i=1; i< sortedRegionScoreTexts.length; i++){
					int currentStart = sortedRegionScoreTexts[i].start;
					int diff = currentStart - bp;
					workingDOS.writeInt(diff);
					workingDOS.writeShort((short)(sortedRegionScoreTexts[i].stop- sortedRegionScoreTexts[i].start - 32768));
					workingDOS.writeFloat(sortedRegionScoreTexts[i].score);
					workingDOS.writeUTF(sortedRegionScoreTexts[i].text);
					bp = currentStart;
				}
			}
		}
		//close ZipEntry
		out.closeEntry();
	}

	/**Reads a binary file into this RegionScoreTextData.*/
	public void read (File binaryFile) throws IOException{
		//open IO
		this.binaryFile = binaryFile;
		FileInputStream workingFIS = new FileInputStream(binaryFile);
		DataInputStream workingDIS = new DataInputStream( new BufferedInputStream(workingFIS ));
		read(workingDIS);
		//close IO
		workingFIS.close();
		workingDIS.close();
	}
	
	/**Reads a DataInputStream into this RegionScoreTextData.*/
	public void read (DataInputStream dis) throws IOException{
		//read text header, currently not used
		header = dis.readUTF();	

		//make array
		int numberRegionScoreTexts = sliceInfo.getNumberRecords();
		sortedRegionScoreTexts = new RegionScoreText[numberRegionScoreTexts];
		
		//what kind of data to follow? 
		String fileType = sliceInfo.getBinaryType();
		
		//int Position, int Length
		if (USeqUtilities.REGION_SCORE_TEXT_INT_INT_FLOAT_TEXT.matcher(fileType).matches()){
			//make first RegionScoreText, position is always an int
			int start = dis.readInt();
			sortedRegionScoreTexts[0] = new RegionScoreText(start, start+dis.readInt(), dis.readFloat(), dis.readUTF());
			//read and resolve offsets to real bps and length to stop
			for (int i=1; i< numberRegionScoreTexts; i++){
				start = sortedRegionScoreTexts[i-1].start + dis.readInt();
				sortedRegionScoreTexts[i] = new RegionScoreText(start, start + dis.readInt(), dis.readFloat(), dis.readUTF());	
			}
		}
		//int Position, short Length
		else if (USeqUtilities.REGION_SCORE_TEXT_INT_SHORT_FLOAT_TEXT.matcher(fileType).matches()){
			//make first RegionScoreText, position is always an int
			int start = dis.readInt();
			sortedRegionScoreTexts[0] = new RegionScoreText(start, start+ dis.readShort() + 32768, dis.readFloat(), dis.readUTF());
			//read and resolve offsets to real bps and length to stop
			for (int i=1; i< numberRegionScoreTexts; i++){
				start = sortedRegionScoreTexts[i-1].start + dis.readInt();
				sortedRegionScoreTexts[i] = new RegionScoreText(start, start + dis.readShort() + 32768, dis.readFloat(), dis.readUTF());	
			}
		}
		//short Postion, short Length
		else if (USeqUtilities.REGION_SCORE_TEXT_SHORT_SHORT_FLOAT_TEXT.matcher(fileType).matches()){
			//make first RegionScoreText, position is always an int
			int start = dis.readInt();
			sortedRegionScoreTexts[0] = new RegionScoreText(start, start+ dis.readShort() + 32768, dis.readFloat(), dis.readUTF());
			//read and resolve offsets to real bps and length to stop
			for (int i=1; i< numberRegionScoreTexts; i++){
				start = sortedRegionScoreTexts[i-1].start + dis.readShort() + 32768;
				sortedRegionScoreTexts[i] = new RegionScoreText(start, start + dis.readShort() + 32768, dis.readFloat(), dis.readUTF());
			}
		}
		//short Position, int Length
		else if (USeqUtilities.REGION_SCORE_TEXT_SHORT_INT_FLOAT_TEXT.matcher(fileType).matches()){
			//make first RegionScoreText, position is always an int
			int start = dis.readInt();
			sortedRegionScoreTexts[0] = new RegionScoreText(start, start+ dis.readInt(), dis.readFloat(), dis.readUTF());
			//read and resolve offsets to real bps and length to stop
			for (int i=1; i< numberRegionScoreTexts; i++){
				start = sortedRegionScoreTexts[i-1].start + dis.readShort() + 32768;
				sortedRegionScoreTexts[i] = new RegionScoreText(start, start + dis.readInt(), dis.readFloat(), dis.readUTF());	
			}
		}
		//unknown!
		else {
			throw new IOException ("Incorrect file type for creating a RegionScoreText[] -> '"+fileType+"' in "+binaryFile +"\n");
		}
	}

	public RegionScoreText[] getRegionScoreTexts() {
		return sortedRegionScoreTexts;
	}
	public void setRegionScoreTexts(RegionScoreText[] sortedRegionScoreTexts) {
		this.sortedRegionScoreTexts = sortedRegionScoreTexts;
		updateSliceInfo(sortedRegionScoreTexts, sliceInfo);
	}

	/**Returns whether data remains.*/
	public boolean trim(int beginningBP, int endingBP) {
		ArrayList<RegionScoreText> al = new ArrayList<RegionScoreText>();
		for (int i=0; i< sortedRegionScoreTexts.length; i++){
			if (sortedRegionScoreTexts[i].isContainedBy(beginningBP, endingBP)) al.add(sortedRegionScoreTexts[i]);
		}
		if (al.size() == 0) return false;
		sortedRegionScoreTexts = new RegionScoreText[al.size()];
		al.toArray(sortedRegionScoreTexts);
		updateSliceInfo(sortedRegionScoreTexts, sliceInfo);
		return true;
	}
}
