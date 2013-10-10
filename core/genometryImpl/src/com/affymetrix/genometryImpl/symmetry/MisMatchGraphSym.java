package com.affymetrix.genometryImpl.symmetry;

import cern.colt.list.IntArrayList;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.ResiduesChars;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hiralv
 */
public class MisMatchGraphSym extends GraphSym {

	int[][] residuesTot = null;
	private File helperIndex;

	public MisMatchGraphSym(int[] x, int[] w, float[] y, 
			int[] a, int[] t, int[] g, int[] c, int[] n, 
			String id, BioSeq seq){
		super(x,w,y,id,seq);
		if (a != null && t != null && g != null && c != null && n != null) {
			setAllResidues(a,t,g,c,n);
		}
	}
	
	protected File index(String graphName, int[] a, int[] t, int[] g, int[] c, int[] n) {
		residuesTot = new int[5][BUFSIZE];
		
		System.arraycopy(a, 0, residuesTot[0], 0, Math.min(BUFSIZE, getPointCount()));
		System.arraycopy(t, 0, residuesTot[1], 0, Math.min(BUFSIZE, getPointCount()));
		System.arraycopy(g, 0, residuesTot[2], 0, Math.min(BUFSIZE, getPointCount()));
		System.arraycopy(c, 0, residuesTot[3], 0, Math.min(BUFSIZE, getPointCount()));
		System.arraycopy(n, 0, residuesTot[4], 0, Math.min(BUFSIZE, getPointCount()));
		
		if (getPointCount() <= BUFSIZE) {
			// no need to index.  Array is too small.
			return null;
		}
		
		return createIndexedFile(graphName,a,t,g,c,n);
	}
	
	@Override
	public Map<String, Object> getLocationProperties(int x, SeqSpan span){
		float y = getYCoordFromX(x);
		if (y < 0) {
			return super.getLocationProperties(x, span);
		}
		
		int leftBound = this.determineBegIndex(x);
		if(span.getMax() - span.getMin() > BUFSIZE || leftBound < 0) {
			return super.getLocationProperties(x, span);
		}

		Map<String, Object> locprops = new HashMap<String, Object>();
		
		locprops.put("x coord", x);
		float ytot = 0;
		for(int i=0; i<residuesTot.length; i++){
			y = residuesTot[i][leftBound - getBufStart()];
			locprops.put(String.valueOf(ResiduesChars.getCharFor(i)), y);
			ytot += y;
		}
		locprops.put("y total", ytot);

		return locprops;
	}

	void setAllResidues(int[] a, int[] t, int[] g, int[] c, int[] n) {
		if(a.length != t.length || t.length != g.length || g.length != c.length || c.length != n.length){
			throw new IllegalArgumentException("All arrays should have same length.");
		}
		helperIndex = index(this.getID()+"helper",a,t,g,c,n);
	}

	public int[][] getAllResidues() {
		return copyAllResidues();
	}
	
	/** Returns a copy of the residues as a int[][]
	 */
	public synchronized int[][] copyAllResidues() {
		int[][] tempCoords = new int[residuesTot.length][this.getPointCount()];
		float[] temp;
		for (int i=0;i<this.getPointCount();i++) {
			temp = getAllResiduesY(i);
			for(int j=0; j<temp.length; j++){
				tempCoords[j][i] = (int) temp[j];
			}
		}
		return tempCoords;
	}
	
	public final float[] getAllResiduesY(int i) {

		float[] ret = new float[residuesTot.length];

		if (i >= this.getPointCount()) {
			Arrays.fill(ret, 0);
			return ret;	// out of range
		}
		
		if (i < getBufStart() || i >= getBufStart() + BUFSIZE) {
			readIntoBuffers(i);
		}

		for(int j =0; j<residuesTot.length; j++){
			ret[j] = residuesTot[j][i - getBufStart()];
		}
		
		return ret;
	}

	private static File createIndexedFile(String graphName, int[] a, int[] t, int[] g, int[] c, int[] n){
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
			//write(y0, y1, y2, y3, y4)
			for (int i = 0; i < a.length; i++) {
				//Write other residues.
				dos.writeInt(a[i]);
				dos.writeInt(t[i]);
				dos.writeInt(g[i]);
				dos.writeInt(c[i]);
				dos.writeInt(n[i]);

			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(dos);
		}
		return bufVal;
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
			//write(x, y, y0, y1, y2, y3, y4)
			for (int i = 0; i < pointCount; i++) {
				dos.writeInt(start++);
				dos.writeInt(0);

				//Write other residues.
				dos.writeInt(0);
				dos.writeInt(0);
				dos.writeInt(0);
				dos.writeInt(0);
				dos.writeInt(0);

			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(dos);
		}
		return bufVal;
	}
	
	static float[] updateY(File index, int offset, int end, int[] tempy, int[][] yR) {
		RandomAccessFile raf = null;
		float ymin = Float.POSITIVE_INFINITY, ymax = Float.NEGATIVE_INFINITY;
		try {
			// open stream
			raf = new RandomAccessFile(index, "rw");

			// skip to proper location
			int bytesToSkip = (offset*7*4);	// 7 coords (x,y,y0,y1,y2,y3,y4) -- 4 bytes each
			raf.seek(bytesToSkip);
			
			int y;
			int[] newy = new int[yR.length];
			long pos;
			int len = offset + tempy.length > end ? end - offset :tempy.length;

			for(int i=0; i < len; i++){
				raf.readInt();
					
				pos = raf.getFilePointer();
				y = raf.readInt() + tempy[i];

				if(y < ymin) {
					ymin = y;
				}
				
				if(y > ymax) {
					ymax = y;
				}
				

				for(int j=0; j<yR.length; j++){
					newy[j] = raf.readInt() + yR[j][i];
				}
				
				raf.seek(pos);

				raf.writeInt(y);
				for(int j=0; j<yR.length; j++){
					raf.writeInt(newy[j]);
				}
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(raf);
		}

		return new float[]{ymin, ymax};
	}

	static int[] getXCoords(File index, File finalIndex, File finalHelper, int len) {
		DataOutputStream dos = null;
		DataInputStream dis = null;
		DataOutputStream hdos = null;
		IntArrayList xpos = new IntArrayList(len);
		int x;
		int y;
		int[] yR = new int[5];
		try {
			dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(finalIndex)));
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(index)));
			hdos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(finalHelper)));

			for (int i = 0; i < len; i++) {
				x = dis.readInt();
				y = dis.readInt();

				for(int j=0; j<yR.length; j++){
					yR[j] = dis.readInt();
				}

				if(yR[0] >= 0 || yR[1] >= 0 || yR[2] >= 0 || yR[3] >= 0 || yR[4] >= 0){
					xpos.add(x);

					//Write regular index file
					dos.writeInt(x);
					dos.writeFloat(y);
					dos.writeInt(1); // width of 1 is a single point.

					//Write helper index file
					hdos.writeInt(x);
					for(int j=0; j<yR.length; j++){
						hdos.writeInt(yR[j]);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(dos);
			GeneralUtils.safeClose(dis);
			GeneralUtils.safeClose(hdos);
		}
		xpos.trimToSize();

		return xpos.elements();
	}

	static float getFirstY(File index){
		DataInputStream dis = null;
		float y = 0;

		try {
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(index)));
			dis.readInt();
			y = dis.readFloat();
		}catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(dis);
		}

		return y;
	}
	
	@Override
	protected synchronized void readIntoBuffers(int start) {
		super.readIntoBuffers(start);

		if(helperIndex == null){
			return;
		}
		
		DataInputStream dis = null;
		try {
			// open stream
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(helperIndex)));

			// skip to proper location
			int bytesToSkip = (start*5*4);	// 6 coords (yA,yT,yG,yC,yN) -- 4 bytes each
			int bytesSkipped = dis.skipBytes(bytesToSkip);
			if (bytesSkipped < bytesToSkip) {
				System.out.println("ERROR: skipped " + bytesSkipped + " out of " + bytesToSkip + " bytes when indexing");

				for(int i=0; i<5; i++){
					Arrays.fill(residuesTot[i], 0);
				}
				return;
			}

			int maxPoints = Math.min(BUFSIZE, getPointCount() - start);
			// read in bytes
			for (int i=0;i<maxPoints;i++) {

				for(int j=0; j<5; j++){
					residuesTot[j][i] = dis.readInt();
				}
			}
			// zero out remainder of buffer, if necessary
			for(int i=0; i<5; i++){
				Arrays.fill(residuesTot[i], maxPoints,BUFSIZE,0);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			//Arrays.fill(xBuf, 0);
			for(int i=0; i<5; i++){
				Arrays.fill(residuesTot[i], 0);
			}
		} finally {
			GeneralUtils.safeClose(dis);
		}
	}
	
	@Override
	public void clear(){
		super.clear();
		residuesTot = null;
	}

	@Override
	public FileTypeCategory getCategory() {
		return FileTypeCategory.Mismatch;
	}
}
