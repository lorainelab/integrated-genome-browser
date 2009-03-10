package com.affymetrix.igb.view.load;

import com.affymetrix.genometry.AnnotatedBioSeq;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.das.DasServerInfo;
import com.affymetrix.igb.das2.Das2ServerInfo;
import com.affymetrix.igb.general.GenericFeature;
import com.affymetrix.igb.general.GenericServer;
import com.affymetrix.igb.view.load.GeneralLoadUtils.LoadStatus;
import com.affymetrix.igb.view.load.GeneralLoadUtils.LoadStrategy;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

/**
 * Model for table of features.
 */
final class FeaturesTableModel extends AbstractTableModel implements ChangeListener {

	//private static String[] columnNames = {"Load Mode", "Name", "Server", "Server Type", "Load Status"};
	//Turn off "Load Status" for now.
	private static String[] columnNames = {"Load Mode", "Name", "Server", "Server Type"};
	static String[] standardLoadChoices = {"Don't Load", "Region In View", "Whole Chromosome"};
	static String[] quickloadLoadChoices = {"Don't Load", "Whole Genome"};
	private final EnumMap<LoadStrategy, String> DASLoadStrategyMap;  // map to a friendly string
	private final EnumMap<LoadStrategy, String> QuickLoadStrategyMap;  // map to a friendly string
	private final Map<String, LoadStrategy> reverseDASLoadStrategyMap;  // from friendly string to enum
	private final Map<String, LoadStrategy> reverseQuickLoadStrategyMap;  // from friendly string to enum
	private final EnumMap<LoadStatus, String> LoadStatusMap;    // map to a friendly string
	private final AnnotatedBioSeq cur_seq;
	private static final int LOAD_STRATEGY_COLUMN = 0;
	private static final int LOAD_STATUS_COLUMN = 3;
	final List<GenericFeature> features;
	private final GeneralLoadView glv;

	FeaturesTableModel(GeneralLoadView glv, List<GenericFeature> features, AnnotatedBioSeq cur_seq) {
		this.glv = glv;
		this.features = features;
		this.cur_seq = cur_seq;

		this.LoadStatusMap = new EnumMap<LoadStatus, String>(LoadStatus.class);
		this.LoadStatusMap.put(LoadStatus.LOADED, "loaded");
		this.LoadStatusMap.put(LoadStatus.LOADING, "loading...");
		this.LoadStatusMap.put(LoadStatus.UNLOADED, "not loaded");

		this.DASLoadStrategyMap = new EnumMap<LoadStrategy, String>(LoadStrategy.class);
		this.DASLoadStrategyMap.put(LoadStrategy.NO_LOAD, standardLoadChoices[0]);
		this.DASLoadStrategyMap.put(LoadStrategy.VISIBLE, standardLoadChoices[1]);
		this.DASLoadStrategyMap.put(LoadStrategy.WHOLE, standardLoadChoices[2]);
		// Here we map the friendly string back to the LoadStrategy.
		// Rather than repeating the lines above, we loop over all LoadStrategy elements and take advantage
		// of the predefined DASLoadStrategyMap.
		this.reverseDASLoadStrategyMap = new HashMap<String, LoadStrategy>(3);
		for (LoadStrategy strategy : EnumSet.allOf(LoadStrategy.class)) {
			this.reverseDASLoadStrategyMap.put(this.DASLoadStrategyMap.get(strategy), strategy);
		}

		this.QuickLoadStrategyMap = new EnumMap<LoadStrategy, String>(LoadStrategy.class);
		this.QuickLoadStrategyMap.put(LoadStrategy.NO_LOAD, quickloadLoadChoices[0]);
		this.QuickLoadStrategyMap.put(LoadStrategy.WHOLE, quickloadLoadChoices[1]);
		// Here we map the friendky string back to the LoadStrategy.
		// Rather than repeating the lines above, we loop over all LoadStrategy elements and take advantage
		// of the predefined QuickLoadStrategyMap.
		this.reverseQuickLoadStrategyMap = new HashMap<String, LoadStrategy>(3);
		for (LoadStrategy strategy : EnumSet.allOf(LoadStrategy.class)) {
			this.reverseQuickLoadStrategyMap.put(this.QuickLoadStrategyMap.get(strategy), strategy);
		}
	}

	public GenericFeature getFeature(int row) {
		return (features == null) ? null : features.get(row);
	}

	public int getRow(GenericFeature feature) {
		return (features == null) ? null : features.indexOf(feature);
	}

	public List<GenericFeature> getFeatures() {
		return features;
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return (features == null) ? 0 : features.size();
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		if (features == null) {
			return null;
		}
		GenericFeature gFeature = features.get(row);
		GenericServer.ServerType serverType;
		switch (col) {
			case 0:
				serverType = gFeature.gVersion.gServer.serverType;
				if (serverType == GenericServer.ServerType.QuickLoad) {
					return this.QuickLoadStrategyMap.get(gFeature.loadStrategy);
				}
				return this.DASLoadStrategyMap.get(gFeature.loadStrategy);
			case 1:
				return gFeature.featureName;
			case 2:
				return gFeature.gVersion.gServer.serverName;
			case 3:
				serverType = gFeature.gVersion.gServer.serverType;
				if (serverType == GenericServer.ServerType.DAS2) {
					return "DAS/2";
				}
				if (serverType == GenericServer.ServerType.DAS) {
					return "DAS";
				}
				if (serverType == GenericServer.ServerType.QuickLoad) {
					return "Quickload";
				}
				return "unknown";
			case 4:
				LoadStatus ls = gFeature.LoadStatusMap.get(this.cur_seq);
				return this.LoadStatusMap.get(ls);
			default:
				System.out.println("Shouldn't reach here: " + row + " " + col);
				return null;
		}
	}

	@Override
	public Class getColumnClass(int c) {
		if ((getValueAt(0, c)) == null) {
			System.out.println("Null Reference ERROR: column " + c);
		}
		return getValueAt(0, c).getClass();
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return (col == LOAD_STRATEGY_COLUMN);
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		if (col != LOAD_STRATEGY_COLUMN) {
			return;
		}

		String valueString = value.toString();
		GenericFeature gFeature = features.get(row);
		GenericServer.ServerType serverType = gFeature.gVersion.gServer.serverType;

		if (serverType == GenericServer.ServerType.QuickLoad) {
			if (!this.QuickLoadStrategyMap.get(gFeature.loadStrategy).equals(valueString)) {
				// strategy changed.  Update the feature object.
				gFeature.loadStrategy = this.reverseQuickLoadStrategyMap.get(valueString);
				updatedStrategy(row, col, gFeature);
			}
		} else if (!this.DASLoadStrategyMap.get(gFeature.loadStrategy).equals(valueString)) {
				// strategy changed.  Update the feature object.
				gFeature.loadStrategy = this.reverseDASLoadStrategyMap.get(valueString);
				updatedStrategy(row, col, gFeature);
			}
	}

	/**
	 * The strategy was changed.  Update the table, and if necessary, load the annotations and change the button statuses.
	 * @param row
	 * @param col
	 * @param gFeature
	 */
	private void updatedStrategy(int row, int col, GenericFeature gFeature) {
		fireTableCellUpdated(row, col);

			if (gFeature.loadStrategy == LoadStrategy.WHOLE) {
				this.glv.glu.loadAndDisplayAnnotations(gFeature, this.cur_seq, this);
				Application.getSingleton().setStatus("", false);
			}

			//  Whatever feature strategy changed, it may have affected
			// the enable status of the "load visible" button
			this.glv.changeVisibleDataButtonIfNecessary(features);
	}

	public void stateChanged(ChangeEvent evt) {
		Object src = evt.getSource();
		if (src instanceof GenericFeature) {
			int row = getRow((GenericFeature) src);
			if (row >= 0) {  // if typestate is present in table, then send notification of row change
				fireTableRowsUpdated(row, row);
			}
		}
	}
}
