package com.affymetrix.genometryImpl.useq.data;

import java.io.*;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.affymetrix.genometryImpl.useq.*;

/**Container for a sorted Region[] and it's associated SliceInfo.
* @author david.nix@hci.utah.edu*/
public class RegionData extends USeqData{

	//fields
	private Region[] sortedRegions;

	//constructors
	public RegionData(){}

	/**Note, be sure to sort the Region[].*/
	public RegionData(Region[] sortedRegions, SliceInfo sliceInfo){
		this.sortedRegions = sortedRegions;
		this.sliceInfo = sliceInfo;
	}
	public RegionData(File binaryFile) throws IOException{
		sliceInfo = new SliceInfo(binaryFile.getName());
		read (binaryFile);
	}
	public RegionData(DataInputStream dis, SliceInfo sliceInfo) throws IOException{
		this.sliceInfo = sliceInfo;
		read (dis);
	}
	
	//methods
	/**Updates the SliceInfo setting just the FirstStartPosition, LastStartPosition, and NumberRecords.*/
	public static void updateSliceInfo (Region[] sortedRegions, SliceInfo sliceInfo){
		sliceInfo.setFirstStartPosition(sortedRegions[0].getStart());
		sliceInfo.setLastStartPosition(sortedRegions[sortedRegions.length-1].start);
		sliceInfo.setNumberRecords(sortedRegions.length);
	}
	
	/**Writes six column xxx.bed formatted lines to the PrintWriter*/
	public void writeBed (PrintWriter out){
		String chrom = sliceInfo.getChromosome();
		String strand = sliceInfo.getStrand();
		for (int i=0; i< sortedRegions.length; i++){
			//chrom start stop name score strand
			out.println(chrom+"\t"+sortedRegions[i].start+"\t"+sortedRegions[i].stop+"\t"+".\t0\t"+strand);
		}
	}

	/**Writes the Region[] to a binary file.  Each region's start/stop is converted to a running offset/length which are written as either ints or shorts.
	 * @param saveDirectory, the binary file will be written using the chromStrandStartBP-StopBP.extension notation to this directory
	 * @param attemptToSaveAsShort, scans to see if the offsets and region lengths exceed 65536 bp, a bit slower to write but potentially a considerable size reduction, set to false for max speed
	 * @return the binaryFile written to the saveDirectory
	 * */
	public File write (File saveDirectory, boolean attemptToSaveAsShort) throws IOException{
		//check to see if this can be saved using shorts instead of ints?
		boolean useShortBeginning = false;
		boolean useShortLength = false;
		if (attemptToSaveAsShort){			
			int bp = sortedRegions[0].start;
			useShortBeginning = true;
			for (int i=1; i< sortedRegions.length; i++){
				int currentStart = sortedRegions[i].start;
				int diff = currentStart - bp;
				if (diff > 65536) {
					useShortBeginning = false;
					break;
				}
				bp = currentStart;
			}
			//check to short length
			useShortLength = true;
			for (int i=0; i< sortedRegions.length; i++){
				int diff = sortedRegions[i].stop - sortedRegions[i].start;
				if (diff > 65536) {
					useShortLength = false;
					break;
				}
			}
		}
		//make and put file type/extension 
		String fileType;
		if (useShortBeginning) fileType = USeqUtilities.SHORT;
		else fileType = USeqUtilities.INT;
		if (useShortLength) fileType = fileType+ USeqUtilities.SHORT;
		else fileType = fileType+ USeqUtilities.INT;
		sliceInfo.setBinaryType(fileType);
		binaryFile = new File(saveDirectory, sliceInfo.getSliceName());

		//make IO
		FileOutputStream workingFOS = new FileOutputStream(binaryFile);
		DataOutputStream workingDOS = new DataOutputStream( new BufferedOutputStream (workingFOS));

		//write String header, currently this isn't used
		workingDOS.writeUTF(header);

		//write first position, always an int
		workingDOS.writeInt(sortedRegions[0].start);

		//write short position?
		int bp = sortedRegions[0].start;
		if (useShortBeginning) {			
			//also short length?
			//no
			if (useShortLength == false){
				//write first record's length
				workingDOS.writeInt(sortedRegions[0].stop- sortedRegions[0].start);
				for (int i=1; i< sortedRegions.length; i++){
					int currentStart = sortedRegions[i].start;
					//subtract 32768 to extend range of short (-32768 to 32768)
					int diff = currentStart - bp - 32768;
					workingDOS.writeShort((short)(diff));
					workingDOS.writeInt(sortedRegions[i].stop- sortedRegions[i].start);
					bp = currentStart;
				}
			}
			//yes short length
			else {
				//write first record's length, subtracting 32768 to extent the range of the signed short
				workingDOS.writeShort((short)(sortedRegions[0].stop- sortedRegions[0].start - 32768));
				for (int i=1; i< sortedRegions.length; i++){
					int currentStart = sortedRegions[i].start;
					//subtract 32768 to extend range of short (-32768 to 32768)
					int diff = currentStart - bp - 32768;
					workingDOS.writeShort((short)(diff));
					workingDOS.writeShort((short)(sortedRegions[i].stop- sortedRegions[i].start - 32768));
					bp = currentStart;
				}
			}
		}

		//no, write int for position
		else {
			//short length? no
			if (useShortLength == false){
				//write first record's length
				workingDOS.writeInt(sortedRegions[0].stop- sortedRegions[0].start);
				for (int i=1; i< sortedRegions.length; i++){
					int currentStart = sortedRegions[i].start;
					int diff = currentStart - bp;
					workingDOS.writeInt(diff);
					workingDOS.writeInt(sortedRegions[i].stop- sortedRegions[i].start);
					bp = currentStart;
				}
			}
			//yes
			else {
				//write first record's length
				workingDOS.writeShort((short)(sortedRegions[0].stop- sortedRegions[0].start - 32768));
				for (int i=1; i< sortedRegions.length; i++){
					int currentStart = sortedRegions[i].start;
					int diff = currentStart - bp;
					workingDOS.writeInt(diff);
					workingDOS.writeShort((short)(sortedRegions[i].stop- sortedRegions[i].start - 32768));
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
			int bp = sortedRegions[0].start;
			useShortBeginning = true;
			for (int i=1; i< sortedRegions.length; i++){
				int currentStart = sortedRegions[i].start;
				int diff = currentStart - bp;
				if (diff > 65536) {
					useShortBeginning = false;
					break;
				}
				bp = currentStart;
			}
			//check to short length
			useShortLength = true;
			for (int i=0; i< sortedRegions.length; i++){
				int diff = sortedRegions[i].stop - sortedRegions[i].start;
				if (diff > 65536) {
					useShortLength = false;
					break;
				}
			}
		}
		//make and put file type/extension 
		String fileType;
		if (useShortBeginning) fileType = USeqUtilities.SHORT;
		else fileType = USeqUtilities.INT;
		if (useShortLength) fileType = fileType+ USeqUtilities.SHORT;
		else fileType = fileType+ USeqUtilities.INT;
		sliceInfo.setBinaryType(fileType);
		binaryFile = null;
		
		//make new ZipEntry
		out.putNextEntry(new ZipEntry(sliceInfo.getSliceName()));
		
		//make IO
		DataOutputStream workingDOS = new DataOutputStream(out);
		
		//write String header, currently this isn't used
		workingDOS.writeUTF(header);

		//write first position, always an int
		workingDOS.writeInt(sortedRegions[0].start);

		//write short position?
		int bp = sortedRegions[0].start;
		if (useShortBeginning) {			
			//also short length?
			//no
			if (useShortLength == false){
				//write first record's length
				workingDOS.writeInt(sortedRegions[0].stop- sortedRegions[0].start);
				for (int i=1; i< sortedRegions.length; i++){
					int currentStart = sortedRegions[i].start;
					//subtract 32768 to extend range of short (-32768 to 32768)
					int diff = currentStart - bp - 32768;
					workingDOS.writeShort((short)(diff));
					workingDOS.writeInt(sortedRegions[i].stop- sortedRegions[i].start);
					bp = currentStart;
				}
			}
			//yes short length
			else {
				//write first record's length, subtracting 32768 to extent the range of the signed short
				workingDOS.writeShort((short)(sortedRegions[0].stop- sortedRegions[0].start - 32768));
				for (int i=1; i< sortedRegions.length; i++){
					int currentStart = sortedRegions[i].start;
					//subtract 32768 to extend range of short (-32768 to 32768)
					int diff = currentStart - bp - 32768;
					workingDOS.writeShort((short)(diff));
					workingDOS.writeShort((short)(sortedRegions[i].stop- sortedRegions[i].start - 32768));
					bp = currentStart;
				}
			}
		}
		//no, write int for position
		else {
			//short length? no
			if (useShortLength == false){
				//write first record's length
				workingDOS.writeInt(sortedRegions[0].stop- sortedRegions[0].start);
				for (int i=1; i< sortedRegions.length; i++){
					int currentStart = sortedRegions[i].start;
					int diff = currentStart - bp;
					workingDOS.writeInt(diff);
					workingDOS.writeInt(sortedRegions[i].stop- sortedRegions[i].start);
					bp = currentStart;
				}
			}
			//yes
			else {
				//write first record's length
				workingDOS.writeShort((short)(sortedRegions[0].stop- sortedRegions[0].start - 32768));
				for (int i=1; i< sortedRegions.length; i++){
					int currentStart = sortedRegions[i].start;
					int diff = currentStart - bp;
					workingDOS.writeInt(diff);
					workingDOS.writeShort((short)(sortedRegions[i].stop- sortedRegions[i].start - 32768));
					bp = currentStart;
				}
			}
		}		
		//close ZipEntry
		out.closeEntry();
	}

	
	/**Reads a binary file into this RegionData.*/
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
	
	/**Reads a DataInputStream into this RegionData.*/
	private void read (DataInputStream dis) throws IOException{
		//read text header, currently not used
		header = dis.readUTF();

		//make array
		int numberRegions = sliceInfo.getNumberRecords();
		sortedRegions = new Region[numberRegions];
		
		//what kind of data to follow? 
		String fileType = sliceInfo.getBinaryType();
	
		//int Position, int Length
		if (USeqUtilities.REGION_INT_INT.matcher(fileType).matches()){
			//make first Region, position is always an int
			int start = dis.readInt();
			sortedRegions[0] = new Region(start, start+dis.readInt());
			//read and resolve offsets to real bps and length to stop
			for (int i=1; i< numberRegions; i++){
				start = sortedRegions[i-1].start + dis.readInt();
				sortedRegions[i] = new Region(start, start + dis.readInt());	
			}
		}
		//int Position, short Length
		else if (USeqUtilities.REGION_INT_SHORT.matcher(fileType).matches()){
			//make first Region, position is always an int
			int start = dis.readInt();
			sortedRegions[0] = new Region(start, start+ dis.readShort() + 32768);
			//read and resolve offsets to real bps and length to stop
			for (int i=1; i< numberRegions; i++){
				start = sortedRegions[i-1].start + dis.readInt();
				sortedRegions[i] = new Region(start, start + dis.readShort() + 32768);	
			}
		}
		//short Postion, short Length
		else if (USeqUtilities.REGION_SHORT_SHORT.matcher(fileType).matches()){
			//make first Region, position is always an int
			int start = dis.readInt();
			sortedRegions[0] = new Region(start, start+ dis.readShort() + 32768);
			//read and resolve offsets to real bps and length to stop
			for (int i=1; i< numberRegions; i++){
				start = sortedRegions[i-1].start + dis.readShort() + 32768;
				sortedRegions[i] = new Region(start, start + dis.readShort() + 32768);	
			}
		}
		//short Position, int Length
		else if (USeqUtilities.REGION_SHORT_INT.matcher(fileType).matches()){
			//make first Region, position is always an int
			int start = dis.readInt();
			sortedRegions[0] = new Region(start, start+ dis.readInt());
			//read and resolve offsets to real bps and length to stop
			for (int i=1; i< numberRegions; i++){
				start = sortedRegions[i-1].start + dis.readShort() + 32768;
				sortedRegions[i] = new Region(start, start + dis.readInt());	
			}
		}
		//unknown!
		else {
			throw new IOException ("Incorrect file type for creating a Region[] -> '"+fileType+"' in "+binaryFile +"\n");
		}
	}

	public Region[] getRegions() {
		return sortedRegions;
	}

	public void setRegions(Region[] sortedRegions) {
		this.sortedRegions = sortedRegions;
		updateSliceInfo(sortedRegions, sliceInfo);
	}

	/**Returns whether data remains.*/
	public boolean trim(int beginningBP, int endingBP) {
		ArrayList<Region> al = new ArrayList<Region>();
		for (int i=0; i< sortedRegions.length; i++){
			if (sortedRegions[i].isContainedBy(beginningBP, endingBP)) al.add(sortedRegions[i]);
		}
		if (al.size() == 0) return false;
		sortedRegions = new Region[al.size()];
		al.toArray(sortedRegions);
		updateSliceInfo(sortedRegions, sliceInfo);
		return true;
	}
}
