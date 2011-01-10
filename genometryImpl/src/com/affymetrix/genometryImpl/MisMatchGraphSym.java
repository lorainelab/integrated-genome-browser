package com.affymetrix.genometryImpl;

import cern.colt.list.IntArrayList;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

	public MisMatchGraphSym(File index, int[] x, float ymin, float ymax, String uniqueGraphID, BioSeq seq) {
		super(index, x, ymin, ymax, uniqueGraphID, seq);
	}

	public void addReference(SymWithResidues.ResiduesChars ch, GraphSym gsym){
		reference.put(ch, gsym);
	}

	public Map<SymWithResidues.ResiduesChars, GraphSym> getReference(){
		return reference;
	}
	
	@Override
	public Map<String, Object> getLocationProperties(int x){
		return super.getLocationProperties(x);
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
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(dos);
		}
		return bufVal;
	}

	static float[] updateY(File index, int offset, int end, float[] tempy) {
		RandomAccessFile raf = null;
		float ymin = Float.POSITIVE_INFINITY, ymax = Float.NEGATIVE_INFINITY;
		try {
			// open stream
			raf = new RandomAccessFile(index, "rw");

			// skip to proper location
			int bytesToSkip = (offset*2*4);	// 3 coords (x,y,w) -- 4 bytes each
			raf.seek(bytesToSkip);
			
			float y, newy;
			long pos;
			int len = offset + tempy.length > end ? end - offset :tempy.length;

			for(int i=0; i < len; i++){
				raf.readInt();

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
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(raf);
		}

		return new float[]{ymin, ymax};
	}

	static int[] getXCoords(File index, File finalIndex, int len) {
		DataOutputStream dos = null;
		DataInputStream dis = null;
		IntArrayList xpos = new IntArrayList(len);
		int x;
		float y;
		try {
			dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(finalIndex)));
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(index)));

			for (int i = 0; i < len; i++) {
				x = dis.readInt();
				y = dis.readFloat();
	
				if(y > 0){
					xpos.add(x);
					dos.writeInt(x);
					dos.writeFloat(y);
					dos.writeInt(1); // width of 1 is a single point.
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(dos);
			GeneralUtils.safeClose(dis);
		}
		xpos.trimToSize();

		return xpos.elements();
	}
}
