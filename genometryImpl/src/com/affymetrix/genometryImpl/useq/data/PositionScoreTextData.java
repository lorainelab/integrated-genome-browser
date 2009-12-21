package com.affymetrix.genometryImpl.useq.data;
import com.affymetrix.genometryImpl.useq.*;

import java.io.*;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**Container for a sorted PositionScoreText[].
 * @author david.nix@hci.utah.edu*/
public class PositionScoreTextData extends USeqData{

	//fields
	private PositionScoreText[] sortedPositionScoreTexts;
	
	//constructors
	public PositionScoreTextData(){}
	
	/**Note, be sure to sort the PositionScoreText[].*/
	public PositionScoreTextData(PositionScoreText[] sortedPositionScoreTexts, SliceInfo sliceInfo){
		this.sortedPositionScoreTexts = sortedPositionScoreTexts;
		this.sliceInfo = sliceInfo;
	}
	public PositionScoreTextData(File binaryFile) throws IOException{
		sliceInfo = new SliceInfo(binaryFile.getName());
		read (binaryFile);
	}
	public PositionScoreTextData(DataInputStream dis, SliceInfo sliceInfo) throws IOException{
		this.sliceInfo = sliceInfo;
		read (dis);
	}
	
	//methods
	/**Updates the SliceInfo setting just the FirstStartPosition, LastStartPosition, and NumberRecords.*/
	public static void updateSliceInfo (PositionScoreText[] sortedPositionScoreTexts, SliceInfo sliceInfo){
		sliceInfo.setFirstStartPosition(sortedPositionScoreTexts[0].position);
		sliceInfo.setLastStartPosition(sortedPositionScoreTexts[sortedPositionScoreTexts.length-1].position);
		sliceInfo.setNumberRecords(sortedPositionScoreTexts.length);
	}
	/**Writes six column xxx.bed formatted lines to the PrintWriter*/
	public void writeBed (PrintWriter out){
		String chrom = sliceInfo.getChromosome();
		String strand = sliceInfo.getStrand();
		for (int i=0; i< sortedPositionScoreTexts.length; i++){
			//chrom start stop name score strand
			out.println(chrom+"\t"+sortedPositionScoreTexts[i].position+"\t"+(sortedPositionScoreTexts[i].position + 1)+"\t"+ sortedPositionScoreTexts[i].text +"\t"+sortedPositionScoreTexts[i].score+"\t"+strand);
		}
	}
	
	//methods
	
	/**Writes the PositionScoreText[] to a binary file.
	 * @param saveDirectory, the binary file will be written using the chromStrandStartBP-StopBP.extension notation to this directory
	 * @param attemptToSaveAsShort, scans to see if the offsets exceed 65536 bp, a bit slower to write but potentially a considerable size reduction, set to false for max speed
	 * @return the binaryFile written to the saveDirectory
	 * */
	public File write (File saveDirectory, boolean attemptToSaveAsShort) throws IOException{
		//check to see if this can be saved using shorts instead of ints?
		boolean useShort = false;
		if (attemptToSaveAsShort){			
			int bp = sortedPositionScoreTexts[0].position;
			useShort = true;
			for (int i=1; i< sortedPositionScoreTexts.length; i++){
				int currentStart = sortedPositionScoreTexts[i].position;
				int diff = currentStart - bp;
				if (diff > 65536) {
					useShort = false;
					break;
				}
				bp = currentStart;
			}
		}

		//make and put file type/extension in header
		String fileType;
		if (useShort) fileType = USeqUtilities.SHORT + USeqUtilities.FLOAT;
		else fileType = USeqUtilities.INT + USeqUtilities.FLOAT;
		fileType = fileType + USeqUtilities.TEXT;
		sliceInfo.setBinaryType(fileType);
		binaryFile = new File(saveDirectory, sliceInfo.getSliceName());

		//make IO
		FileOutputStream workingFOS = new FileOutputStream(binaryFile);
		DataOutputStream workingDOS = new DataOutputStream( new BufferedOutputStream (workingFOS));

		//write String header, currently this isn't used
		workingDOS.writeUTF(header);
		
		//write first position score, always an int
		workingDOS.writeInt(sortedPositionScoreTexts[0].position);
		workingDOS.writeFloat(sortedPositionScoreTexts[0].score);
		workingDOS.writeUTF(sortedPositionScoreTexts[0].text);
		
		//write shorts?
		if (useShort) {			
			int bp = sortedPositionScoreTexts[0].position;
			for (int i=1; i< sortedPositionScoreTexts.length; i++){
				int currentStart = sortedPositionScoreTexts[i].position;
				//subtract 32768 to extend range of short (-32768 to 32768)
				int diff = currentStart - bp - 32768;
				workingDOS.writeShort((short)(diff));
				workingDOS.writeFloat(sortedPositionScoreTexts[i].score);
				workingDOS.writeUTF(sortedPositionScoreTexts[i].text);
				bp = currentStart;
			}
		}
		
		//no, write ints
		else {
			int bp = sortedPositionScoreTexts[0].position;
			for (int i=1; i< sortedPositionScoreTexts.length; i++){
				int currentStart = sortedPositionScoreTexts[i].position;
				int diff = currentStart - bp;
				workingDOS.writeInt(diff);
				workingDOS.writeFloat(sortedPositionScoreTexts[i].score);
				workingDOS.writeUTF(sortedPositionScoreTexts[i].text);
				bp = currentStart;
			}
		}
		
		//close IO
		workingDOS.close();
		workingFOS.close();
		
		return binaryFile;
	}
	
	/**Writes the PositionScoreText[] to a ZipOutputStream.
	 * @param	attemptToSaveAsShort	if true, scans to see if the offsets exceed 65536 bp, a bit slower to write but potentially a considerable size reduction, set to false for max speed
	 * */
	public void write (ZipOutputStream out, boolean attemptToSaveAsShort) throws IOException{
		//check to see if this can be saved using shorts instead of ints?
		boolean useShort = false;
		if (attemptToSaveAsShort){			
			int bp = sortedPositionScoreTexts[0].position;
			useShort = true;
			for (int i=1; i< sortedPositionScoreTexts.length; i++){
				int currentStart = sortedPositionScoreTexts[i].position;
				int diff = currentStart - bp;
				if (diff > 65536) {
					useShort = false;
					break;
				}
				bp = currentStart;
			}
		}

		//make and put file type/extension in header
		String fileType;
		if (useShort) fileType = USeqUtilities.SHORT + USeqUtilities.FLOAT;
		else fileType = USeqUtilities.INT + USeqUtilities.FLOAT;
		fileType = fileType + USeqUtilities.TEXT;
		sliceInfo.setBinaryType(fileType);
		binaryFile = null;
		
		//make new ZipEntry
		out.putNextEntry(new ZipEntry(sliceInfo.getSliceName()));
		
		//make IO
		DataOutputStream workingDOS = new DataOutputStream(out);
		
		//write String header, currently this isn't used
		workingDOS.writeUTF(header);
		
		//write first position score, always an int
		workingDOS.writeInt(sortedPositionScoreTexts[0].position);
		workingDOS.writeFloat(sortedPositionScoreTexts[0].score);
		workingDOS.writeUTF(sortedPositionScoreTexts[0].text);
		
		//write shorts?
		if (useShort) {			
			int bp = sortedPositionScoreTexts[0].position;
			for (int i=1; i< sortedPositionScoreTexts.length; i++){
				int currentStart = sortedPositionScoreTexts[i].position;
				//subtract 32768 to extend range of short (-32768 to 32768)
				int diff = currentStart - bp - 32768;
				workingDOS.writeShort((short)(diff));
				workingDOS.writeFloat(sortedPositionScoreTexts[i].score);
				workingDOS.writeUTF(sortedPositionScoreTexts[i].text);
				bp = currentStart;
			}
		}
		
		//no, write ints
		else {
			int bp = sortedPositionScoreTexts[0].position;
			for (int i=1; i< sortedPositionScoreTexts.length; i++){
				int currentStart = sortedPositionScoreTexts[i].position;
				int diff = currentStart - bp;
				workingDOS.writeInt(diff);
				workingDOS.writeFloat(sortedPositionScoreTexts[i].score);
				workingDOS.writeUTF(sortedPositionScoreTexts[i].text);
				bp = currentStart;
			}
		}
			
		//close ZipEntry but not zip stream!
		out.closeEntry();
	}


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
	
	
	/**Reads a DataInputStream into this PositionScoreData.*/
	public void read (DataInputStream dis) throws IOException{
		//read text header, currently not used
		header = dis.readUTF();	
		
		//make array
		int numberPositions = sliceInfo.getNumberRecords();
		sortedPositionScoreTexts = new PositionScoreText[numberPositions];
		
		//make first position
		sortedPositionScoreTexts[0] = new PositionScoreText(dis.readInt(), dis.readFloat(), dis.readUTF());
		
		//what kind of data to follow? 
		String fileType = sliceInfo.getBinaryType();
		
		//ints?
		if (USeqUtilities.POSITION_SCORE_TEXT_INT_FLOAT_TEXT.matcher(fileType).matches()){	
			//read and resolve offsets to real bps
			for (int i=1; i< numberPositions; i++){
				sortedPositionScoreTexts[i] = new PositionScoreText(sortedPositionScoreTexts[i-1].position + dis.readInt(), dis.readFloat(), dis.readUTF());	
			}
		}
		//shorts?
		else if (USeqUtilities.POSITION_SCORE_TEXT_SHORT_FLOAT_TEXT.matcher(fileType).matches()){
			//read and resolve offsets to real bps
			for (int i=1; i< numberPositions; i++){
				sortedPositionScoreTexts[i] = new PositionScoreText(sortedPositionScoreTexts[i-1].position + dis.readShort() + 32768,  dis.readFloat(), dis.readUTF());	
			}
		}
		//unknown!
		else {
			throw new IOException ("Incorrect file type for creating a PositionScoreText[] -> '"+fileType+"' in "+binaryFile +"\n");
		}
	}

	public PositionScoreText[] getPositionScoreTexts() {
		return sortedPositionScoreTexts;
	}

	public void setPositionScoreTexts(PositionScoreText[] sortedPositionScoreTexts) {
		this.sortedPositionScoreTexts = sortedPositionScoreTexts;
		updateSliceInfo(sortedPositionScoreTexts, sliceInfo);
	}
	/**Returns whether data remains.*/
	public boolean trim(int beginningBP, int endingBP) {
		ArrayList<PositionScoreText> al = new ArrayList<PositionScoreText>();
		for (int i=0; i< sortedPositionScoreTexts.length; i++){
			if (sortedPositionScoreTexts[i].isContainedBy(beginningBP, endingBP)) al.add(sortedPositionScoreTexts[i]);
		}
		if (al.size() == 0) return false;
		sortedPositionScoreTexts = new PositionScoreText[al.size()];
		al.toArray(sortedPositionScoreTexts);
		updateSliceInfo(sortedPositionScoreTexts, sliceInfo);
		return true;
	}
}
