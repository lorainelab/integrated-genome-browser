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
import java.util.Arrays;
import java.util.Map;

/**
 *
 * @author hiralv
 */
public class MisMatchGraphSym extends GraphSym {

	int[][] residuesTot = null;
	private final File helperIndex;

	public MisMatchGraphSym(int[] x, int[] w, float[] y, String id, BioSeq seq){
		super(x,w,y,id,seq);
		helperIndex = null;
	}

	public MisMatchGraphSym(File index, File helperIndex, int[] x, float ymin, float ymax, String uniqueGraphID, BioSeq seq) {
		super(index, x, ymin, ymax, uniqueGraphID, seq);
		this.helperIndex = helperIndex;
		residuesTot = new int[5][BUFSIZE];
		readIntoBuffers(0);
	}
	
	@Override
	public Map<String, Object> getLocationProperties(int x){
		return super.getLocationProperties(x);
	}

	void setAllResidues(int[] a, int[] t, int[] g, int[] c, int[] n) {

		if(a.length != t.length || t.length != g.length || g.length != c.length || c.length != n.length){
			throw new IllegalArgumentException("All arrays should have same length.");
		}

		residuesTot = new int[5][a.length];

		System.arraycopy(a, 0, residuesTot[0], 0, a.length);
		System.arraycopy(t, 0, residuesTot[1], 0, t.length);
		System.arraycopy(g, 0, residuesTot[2], 0, g.length);
		System.arraycopy(c, 0, residuesTot[3], 0, c.length);
		System.arraycopy(n, 0, residuesTot[4], 0, n.length);
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
			int bytesToSkip = (offset*7*4);	// 3 coords (x,y,w) -- 4 bytes each
			raf.seek(bytesToSkip);
			
			int y;
			int[] newy = new int[yR.length];
			long pos;
			int len = offset + tempy.length > end ? end - offset :tempy.length;

			for(int i=0; i < len; i++){
				raf.readInt();

				pos = raf.getFilePointer();
				y = raf.readInt() + tempy[i];

				if(y < ymin)
					ymin = y;
				
				if(y > ymax)
					ymax = y;
				

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

				if(yR[0] > 0 || yR[1] > 0 || yR[2] > 0 || yR[3] > 0 || yR[4] > 0){
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
			int bytesToSkip = (start*6*4);	// 6 coords (x,yA,yT,yG,yC,yN) -- 4 bytes each
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
				//xBuf[i] = dis.readInt();	// x
				dis.readInt();	//x

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
}
