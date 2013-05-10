
package com.affymetrix.igb.shared;

import cern.colt.list.DoubleArrayList;
import com.affymetrix.genoviz.bioviews.GlyphI;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author hiralv
 */
public class ColumnSorter extends FasterExpandPacker {
	
	public void sort(GlyphI parent){
		int child_count = parent.getChildCount();
		if (child_count == 0) {
			return;
		}
		List<GlyphI> column = new ArrayList<GlyphI>();
		List<GlyphI> children = new CopyOnWriteArrayList<GlyphI>(parent.getChildren());
		DoubleArrayList slot_maxes = new DoubleArrayList(1000);
		double slot_height = getMaxChildHeight(parent) + 2 * spacing;
		double current_max = children.get(0).getCoordBox().x + children.get(0).getCoordBox().width, child_min, child_max;
		double ymin = Double.POSITIVE_INFINITY;
		for (GlyphI child : children) {
			child_min = child.getCoordBox().x;
			child_max = child.getCoordBox().x + child.getCoordBox().width;
			if(child_min > current_max){
				current_max = child_max;
				Collections.sort(column, Collections.reverseOrder(new GlyphLengthComparator()));
				column.clear();
			} else {
				current_max = Math.max(current_max, child_max);
			}
			column.add(child);
		}
		
		if(!column.isEmpty()){
			Collections.sort(column, Collections.reverseOrder(new GlyphLengthComparator()));
			column.clear();
		}
	}
	
	public final static class GlyphLengthComparator implements Comparator<GlyphI> {
	
		public int compare(GlyphI g1, GlyphI g2) {
			return Double.compare(g1.getCoordBox().width, g2.getCoordBox().width);
		}
	}
	
	public final static class GlyphXComparator implements Comparator<GlyphI> {
	
		public int compare(GlyphI g1, GlyphI g2) {
			Rectangle2D.Double c1 = g1.getCoordBox();
			Rectangle2D.Double c2 = g2.getCoordBox();
			
			if(c1.x + c1.width < c2.x){
				return -1;
			}
			
			if (c1.x > c2.x + c2.width) {
				return 1;
			}
			
			return Integer.valueOf(g1.getRowNumber()).compareTo(g2.getRowNumber());
		}
	}
}
