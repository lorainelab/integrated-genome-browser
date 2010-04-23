/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.genometryImpl.symloader;

import cern.colt.GenericSorting;
import cern.colt.Swapper;
import cern.colt.function.IntComparator;
import cern.colt.list.FloatArrayList;
import cern.colt.list.IntArrayList;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author hiralv
 */
public final class Gr extends SymLoader {
	private File f;
	private final AnnotatedSeqGroup group;
	private final String featureName;
	private boolean isSorted = false;
	
	public Gr(URI uri, String featureName, AnnotatedSeqGroup seq_group) {
		super(uri);
		this.group = seq_group;
		this.featureName = featureName;
	}

	@Override
	public void init() {
		if (this.isInitialized) {
			return;
		}
		super.init();
		this.f = LocalUrlCacher.convertURIToFile(uri);
	}

	@Override
	public String[] getLoadChoices() {
		String[] choices = {LoadStrategy.NO_LOAD.toString(), LoadStrategy.VISIBLE.toString(), LoadStrategy.CHROMOSOME.toString()};
		return choices;
	}

	@Override
	public List<GraphSym> getChromosome(BioSeq seq) {
		init();
		List<GraphSym> results = new ArrayList<GraphSym>();
		results.add(parse(seq,seq.getMin(),seq.getMax() + 1));
		return results;
	}


	@Override
	public List<GraphSym> getRegion(SeqSpan span) {
		init();
		List<GraphSym> results = new ArrayList<GraphSym>();
		results.add(parse(span.getBioSeq(),span.getMin(),span.getMax() + 1));
		return results;
	}

	public String getMimeType() {
		return "text/gr";
	}

	public static boolean writeGrFormat(GraphSym graf, OutputStream ostr) throws IOException {
		BufferedOutputStream bos = null;
		DataOutputStream dos = null;
		try {
			bos = new BufferedOutputStream(ostr);
			dos = new DataOutputStream(bos);
			writeGraphPoints(graf, dos);
		} finally {
			GeneralUtils.safeClose(dos);
		}
		return true;
	}

	private static void writeGraphPoints(GraphSym graf, DataOutputStream dos) throws IOException {
		int total_points = graf.getPointCount();
		for (int i = 0; i < total_points; i++) {
			dos.writeBytes("" + graf.getGraphXCoord(i) + "\t" +
					graf.getGraphYCoordString(i) + "\n");
		}
	}

	private GraphSym parse(BioSeq aseq, int min, int max){
		GraphSym sym = parse(aseq, min, max, true);

		if(!isSorted){
		FileOutputStream fos = null;
			try{
				fos = new FileOutputStream(f);
				writeGrFormat(sym,fos);
				isSorted = true;
			}catch(IOException ex){
				ex.printStackTrace();
			}finally{
				GeneralUtils.safeClose(fos);
			}
		}
		
		return sym;
	}
	
	private GraphSym parse(BioSeq aseq, int min, int max, boolean ensure_unique_id){
		GraphSym graf = null;
		String line = null;
		String headerstr = null;
		String name = this.featureName;
		boolean hasHeader = false;
		int count = 0;

		IntArrayList xlist = new IntArrayList();
		FloatArrayList ylist = new FloatArrayList();

		FileInputStream fis = null;
		InputStream is = null;
		BufferedReader br = null;
		
		try {

			fis = new FileInputStream(this.f);
			is = GeneralUtils.unzipStream(fis, featureName, new StringBuffer());
			br = new BufferedReader(new InputStreamReader(is));
			// check first line, may be a header for column labels...
			line = br.readLine();
			if (line == null) {
				System.out.println("can't find any data in file!");
				return null;
			}

			try {
				int firstx;
				float firsty;
				if (line.indexOf(' ') > 0) {
					firstx = Integer.parseInt(line.substring(0, line.indexOf(' ')));
					firsty = Float.parseFloat(line.substring(line.indexOf(' ') + 1));
				} else if (line.indexOf('\t') > 0) {
					firstx = Integer.parseInt(line.substring(0, line.indexOf('\t')));
					firsty = Float.parseFloat(line.substring(line.indexOf('\t') + 1));
				} else {
					System.out.println("format not recognized");
					return null;
				}
				if(!(firstx < min || firstx >= max)){
					xlist.add(firstx);
					ylist.add(firsty);
					count++;  // first line parses as numbers, so is not a header, increment count
				}
			} catch (Exception ex) {
				// if first line does not parse as numbers, must be a header...
				// set header flag, don't count as a line...
				headerstr = line;
				System.out.println("Found header on graph file: " + line);
				hasHeader = true;
			}
			int x = 0;
			float y = 0;
			int xprev = Integer.MIN_VALUE;
			boolean sorted = true;
			while ((line = br.readLine()) != null) {
				if (line.indexOf(' ') > 0) {
					x = Integer.parseInt(line.substring(0, line.indexOf(' ')));
					y = Float.parseFloat(line.substring(line.indexOf(' ') + 1));
				} else if (line.indexOf('\t') > 0) {
					x = Integer.parseInt(line.substring(0, line.indexOf('\t')));
					y = Float.parseFloat(line.substring(line.indexOf('\t') + 1));
				}

				if (x >= max) {
					if (isSorted) {
						break;
					} else {
						continue;
					}
				}

				if (x < min ) {
					continue;
				}

				xlist.add(x);
				ylist.add(y);
				count++;
				// checking on whether graph is sorted...
				if(!isSorted){
					if (xprev > x) {
						sorted = false;
					}else
						xprev = x;
				}
				
			}
			isSorted = sorted;

			if (name == null && hasHeader) {
				name = headerstr;
			}
			int xcoords[] = Arrays.copyOf(xlist.elements(), xlist.size());
			xlist = null;
			float ycoords[] = Arrays.copyOf(ylist.elements(), ylist.size());
			ylist = null;

			if (!sorted) {
				System.err.println("input graph not sorted, sorting by base coord");
				sortXYDataOnX(xcoords, ycoords);
			}
			if (ensure_unique_id) {
				name = AnnotatedSeqGroup.getUniqueGraphID(this.featureName, this.group);
			}

			
			graf = new GraphSym(xcoords, ycoords, name, aseq);
			System.out.println("loaded graph data, total points = " + count);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			GeneralUtils.safeClose(br);
		}
		return graf;
	}

	/**
	 * Sort xList, yList, and wList based upon xList
	 * @param xList
	 * @param yList
	 * @param wList
	 */
	private static void sortXYDataOnX(final int[] xList, final float[] yList) {
		Swapper swapper = new Swapper() {

			public void swap(int a, int b) {
				int swapInt = xList[a];
				xList[a] = xList[b];
				xList[b] = swapInt;

				float swapFloat = yList[a];
				yList[a] = yList[b];
				yList[b] = swapFloat;
			}
		};
		IntComparator comp = new IntComparator() {
			public int compare(int a, int b) {
				return ((Integer) xList[a]).compareTo(xList[b]);
			}
		};
		GenericSorting.quickSort(0, xList.length, comp, swapper);
	}

}
