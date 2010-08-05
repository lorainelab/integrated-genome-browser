package com.affymetrix.genometryImpl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * This class sorts tab delimited files such as bed, psl, wiggle etc.
 * 
 * @author hiralv
 */
public class SortTabFile {

	static private final Pattern tab_regex = Pattern.compile("\t");

	public static boolean sort(File file, int column){
		
		BufferedReader br = null;
		String line = null;
		List<String> list = new ArrayList<String>();
		try {
			
			List<String> templist = new ArrayList<String>();
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

			Thread thread = Thread.currentThread();

			while ((line = br.readLine()) != null && (!thread.isInterrupted())) {

				if(line.startsWith("track")){
					Collections.sort(templist, new LineComparator(column));
					list.addAll(templist);
					templist = new ArrayList<String>();
				}
				
				templist.add(line);
			}
			Collections.sort(templist, new LineComparator(column));
			list.addAll(templist);
						
		} catch (FileNotFoundException ex) {
			Logger.getLogger(SortTabFile.class.getName()).log(Level.SEVERE, "Could not find file " + file, ex);
			return false;
		} catch (IOException ex){
			Logger.getLogger(SortTabFile.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		} finally {
			GeneralUtils.safeClose(br);
		}

		return writeFile(file, list);
	}

	private static boolean writeFile(File file, List<String> lines){
		BufferedWriter bw = null;
		try {
			
			if(!file.canWrite()){
				Logger.getLogger(SortTabFile.class.getName()).log(Level.SEVERE, "Cannot write to file {0}", file);
				return false;
			}

			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));

			for(String line : lines){
				bw.write(line);
			}

			bw.flush();
			
		} catch (FileNotFoundException ex) {
			Logger.getLogger(SortTabFile.class.getName()).log(Level.SEVERE, "Could not find file " + file, ex);
			return false;
		} catch (IOException ex){
			Logger.getLogger(SortTabFile.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		} finally {
			GeneralUtils.safeClose(bw);
		}
		return true;
	}

	static class LineComparator implements Comparator<String>{

		private final int column;

		public LineComparator(int column){
			this.column = column - 1;
		}
		
		public int compare(String o1, String o2) {
			String[] o1Fields = tab_regex.split(o1);
			String[] o2Fields = tab_regex.split(o2);
			int o1Int, o2Int;
			try {
				o1Int = Integer.valueOf(o1Fields[column]);
			} catch (Exception ex) {
				return 0;
			}

			try {
				o2Int = Integer.valueOf(o2Fields[column]);
			} catch (Exception ex) {
				return 0;
			}

			if (o1Int == o2Int) {
				return 0;
			}

			if (o1Int > o2Int) {
				return 1;
			}

			return -1;
		}

	}
	
}
