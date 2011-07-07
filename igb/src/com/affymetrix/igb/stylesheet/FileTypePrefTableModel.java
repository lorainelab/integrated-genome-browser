package com.affymetrix.igb.stylesheet;

import java.util.Map.Entry;
import javax.swing.table.AbstractTableModel;
import com.affymetrix.igb.tiers.TrackStyle;

/**
 *
 * @author hiralv
 */
public class FileTypePrefTableModel extends AbstractTableModel implements PropertyConstants {

	private static final String FILE_TYPE = "File Type";
	private static final String COLOR = "Color";
	private static final String SEPARATE = "2 Track";
	private static final String COLLAPSED = "Collapsed";
	private static final String MAX_DEPTH = "Max Depth";
	private static final String BACKGROUND = "Background";
	private static final String GLYPH_DEPTH = "Connected";
	private static final String LABEL_FIELD = "Label Field";
	private static final String FONT_SIZE = "Font Size";
	private static final String DIRECTION_TYPE = "Direction Type";
	
	private final static String[] col_headings = {
		FILE_TYPE,
		COLOR, BACKGROUND,
		SEPARATE, COLLAPSED,
		MAX_DEPTH, GLYPH_DEPTH, LABEL_FIELD, FONT_SIZE, DIRECTION_TYPE
	};
	private static final int COL_FILE_TYPE = 0;
	private static final int COL_COLOR = 1;
	private static final int COL_BACKGROUND = 2;
	private static final int COL_SEPARATE = 3;
	private static final int COL_COLLAPSED = 4;
	private static final int COL_MAX_DEPTH = 5;
	private static final int COL_GLYPH_DEPTH = 6;
	private static final int COL_LABEL_FIELD = 7;
	private static final int COL_FONT_SIZE = 8;
	private static final int COL_DIRECTION_TYPE = 9;
	
	Entry[] file2types = null;

	public void setElements(java.util.Map<String, AssociationElement> elements){
		file2types = elements.entrySet().toArray(new Entry[elements.size()]);
		fireTableDataChanged();
	}
	
	// Allow editing most fields in normal rows, but don't allow editing some
	// fields in the "default" style row.
	@Override
	public boolean isCellEditable(int row, int column) {
		if (column == COL_FILE_TYPE) {
			return false;
		}

		return true;
	}

	public int getRowCount() {
		return this.file2types.length;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return col_headings[columnIndex];
	}

	public int getColumnCount() {
		return col_headings.length;
	}

	public Object getValueAt(int row, int column) {
		Entry entry = file2types[row];
		AssociationElement element = (AssociationElement) entry.getValue();
		TrackStyle style = new TrackStyle(element);
		switch (column) {
			case COL_FILE_TYPE:
				return entry.getKey();
			case COL_COLOR:
				return style.getForeground();

			case COL_SEPARATE:
				return Boolean.valueOf(style.getSeparate());

			case COL_COLLAPSED:
				return Boolean.valueOf(style.getCollapsed());

			case COL_MAX_DEPTH:
				int md = style.getMaxDepth();
				if (md == 0) {
					return "";
				} else {
					return String.valueOf(md);
				}

			case COL_BACKGROUND:
				return style.getBackground();

			case COL_GLYPH_DEPTH:
				return (style.getShow2Tracks() == 2 ? Boolean.TRUE : Boolean.FALSE);

			case COL_LABEL_FIELD:
				return style.getLabelField();

			case COL_FONT_SIZE:
				return style.getTrackNameSize();

			case COL_DIRECTION_TYPE:
				return style.getDirectionName();

			default:
				return null;
		}
	}

	@Override
	public Class<?> getColumnClass(int c) {
		Object val = getValueAt(0, c);
		if (val == null) {
			return Object.class;
		} else {
			return val.getClass();
		}
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		try {
			Entry entry = file2types[row];
			AssociationElement element = (AssociationElement) entry.getValue();
			
			switch (col) {
				case COL_COLOR:
					element.propertyMap.put(PROP_COLOR, value);
					break;
				case COL_SEPARATE:
					element.propertyMap.put(PROP_SEPARATE, value.toString());
					break;
				case COL_COLLAPSED:
					element.propertyMap.put(PROP_COLLAPSED, value.toString());
					break;
				case COL_MAX_DEPTH: 
					element.propertyMap.put(PROP_MAX_DEPTH, value.toString());
					break;
				case COL_BACKGROUND:
					element.propertyMap.put(PROP_BACKGROUND, value);
					break;
				case COL_GLYPH_DEPTH:
					if (Boolean.TRUE.equals(value)) {
						element.propertyMap.put(PROP_GLYPH_DEPTH, String.valueOf(2));
					} else {
						element.propertyMap.put(PROP_GLYPH_DEPTH, String.valueOf(1));
					}
					break;
				case COL_LABEL_FIELD:
					element.propertyMap.put(PROP_LABEL_FIELD, value);
					break;
				case COL_FONT_SIZE:
					element.propertyMap.put(PROP_FONT_SIZE, value.toString());
					break;
				case COL_DIRECTION_TYPE:
					element.propertyMap.put(PROP_DIRECTION_TYPE, value.toString());
					break;
				default:
					System.out.println("Unknown column selected: " + col);

			}
			fireTableCellUpdated(row, col);
		} catch (Exception e) {
			// exceptions should not happen, but must be caught if they do
			System.out.println("Exception in TierPrefsView.setValueAt(): " + e);
		}
	}

	int parseInteger(String s, int empty_string, int fallback) {
		//System.out.println("Parsing string: '" + s + "'");
		int i = fallback;
		try {
			if ("".equals(s.trim())) {
				i = empty_string;
			} else {
				i = Integer.parseInt(s);
			}
		} catch (Exception e) {
			//System.out.println("Exception: " + e);
			// don't report the error, use the fallback value
		}
		return i;
	}
	
}
