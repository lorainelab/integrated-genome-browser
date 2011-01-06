package com.affymetrix.genometryImpl;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hiralv
 */
public class MisMatchGraphSym extends GraphSym {

	Map<SymWithResidues.ResiduesChars, GraphSym> reference = new HashMap<SymWithResidues.ResiduesChars, GraphSym>();

	public MisMatchGraphSym(int[] x, int[] w, float[] y, String id, BioSeq seq){
		super(x,w,y,id,seq);
	}

	public MisMatchGraphSym(File index, int start, int end, float ymin, float ymax, String uniqueGraphID, BioSeq seq) {
		super(index, start, end, ymin, ymax, uniqueGraphID, seq);
	}

	public void addReference(SymWithResidues.ResiduesChars ch, GraphSym gsym){
		reference.put(ch, gsym);
	}

	public Map<SymWithResidues.ResiduesChars, GraphSym> getReference(){
		return reference;
	}
	
	@Override
	public Map<String, Object> getLocationProperties(int x){
		char ch = this.getGraphSeq().getResidues(x, x+1).charAt(0);
		float y, ytotal = 0;
		String yStr;

		Map<String, Object> locprops = new HashMap<String, Object>();

		return locprops;
	}

	public static File createEmptyIndexFile(String graphName, int pointCount, int start) {
		File bufVal = null;
		DataOutputStream dos = null;
		try {
			// create indexed file.

			if (graphName.length() < 3) {
				graphName += "___";
				// fix for Java error with short names
			}
			bufVal = File.createTempFile(URLEncoder.encode(graphName, "UTF-8"), "idx");
			bufVal.deleteOnExit(); // Delete this file when shutting down.
			dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(bufVal)));
			for (int i = 0; i < pointCount; i++) {
				dos.writeInt(start++);
				dos.writeFloat(0);
				dos.writeInt(0); // width of 1 is a single point.
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(dos);
		}
		return bufVal;
	}

	static float[] updateY(File index, int offset, float[] tempy) {
		RandomAccessFile raf = null;
		float ymin = Float.POSITIVE_INFINITY, ymax = Float.NEGATIVE_INFINITY;
		try {
			// open stream
			raf = new RandomAccessFile(index, "rw");

			// skip to proper location
			int bytesToSkip = (offset*3*4);	// 3 coords (x,y,w) -- 4 bytes each
			int bytesSkipped = raf.skipBytes(bytesToSkip);
			if (bytesSkipped < bytesToSkip) {
				System.out.println("ERROR: skipped " + bytesSkipped + " out of " + bytesToSkip + " bytes when indexing");
				return new float[]{ymin,ymax};
			}

			float y, newy;
			long pos;
			int x;
			

			for(int i=0; i < tempy.length; i++){
				x = raf.readInt();

				pos = raf.getFilePointer();
				y = raf.readFloat();
				newy = y + tempy[i];
				if(newy < ymin){
					ymin = newy;
				}

				if(newy > ymax){
					ymax = newy;
				}
				
				raf.seek(pos);
				raf.writeFloat(newy);
				
				x = raf.readInt();
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(raf);
		}

		return new float[]{ymin, ymax};
	}
}
