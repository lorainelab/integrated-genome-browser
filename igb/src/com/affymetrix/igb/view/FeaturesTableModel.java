package com.affymetrix.igb.view;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.das.DasServerInfo;
import com.affymetrix.igb.das2.Das2ServerInfo;
import com.affymetrix.igb.general.GenericFeature;
import com.affymetrix.igb.view.GeneralLoadUtils.LoadStatus;
import com.affymetrix.igb.view.GeneralLoadUtils.LoadStrategy;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

public class FeaturesTableModel extends AbstractTableModel implements ChangeListener  {
	private static String [] columnNames = {"Load Range","Name","Server","Server Type","Load Status"};
    static String [] loadChoices = {"Don't Load","Visible Range","Whole Range"};
    public final EnumMap<LoadStrategy,String> LoadStrategyMap;  // map to a friendly string
    public final Map<String,LoadStrategy> reverseLoadStrategyMap;  // from friendly string to enum
    public final  EnumMap<LoadStatus,String> LoadStatusMap;    // map to a friendly string

    private static final int LOAD_STRATEGY_COLUMN = 0;
    private static final int LOAD_STATUS_COLUMN = 3;
   
	List<GenericFeature> features;

    GeneralLoadUtils glu;

    public FeaturesTableModel(GeneralLoadUtils glu, List<GenericFeature> features) {
        this.glu = glu;
        this.features = features;

        this.LoadStatusMap = new EnumMap<LoadStatus,String>(LoadStatus.class);
        this.LoadStatusMap.put(LoadStatus.LOADED, "loaded");
        this.LoadStatusMap.put(LoadStatus.LOADING, "loading...");
        this.LoadStatusMap.put(LoadStatus.UNLOADED, "not loaded");

        this.LoadStrategyMap = new EnumMap<LoadStrategy,String>(LoadStrategy.class);
        this.LoadStrategyMap.put(LoadStrategy.NO_LOAD, loadChoices[0]);
        this.LoadStrategyMap.put(LoadStrategy.VISIBLE, loadChoices[1]);
        this.LoadStrategyMap.put(LoadStrategy.WHOLE, loadChoices[2]);

        // Here we map the friendly string back to the LoadStrategy.
        // Rather than repeating the lines above, we loop over all LoadStrategy elements and take advantage
        // of the predefined LoadStrategyMap.
        this.reverseLoadStrategyMap = new HashMap<String,LoadStrategy>(3);
        for (LoadStrategy strategy : EnumSet.allOf(LoadStrategy.class)) {
            this.reverseLoadStrategyMap.put(this.LoadStrategyMap.get(strategy), strategy);
        }
    }

	public GenericFeature getFeature(int row) {
		return features.get(row);
	}

	public int getRow(GenericFeature feature) {
		return features.indexOf(feature);
	}

	public List<GenericFeature> getFeatures() { return features; }

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return features.size();
	}

    @Override
	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
        switch (col) {
            case 0:
                return this.LoadStrategyMap.get(features.get(row).loadStrategy);
            case 1:
                return features.get(row).featureName;
            case 2:
                return features.get(row).gVersion.gServer.serverName;
            case 3:
                Class c = features.get(row).gVersion.gServer.serverClass;
                if (c == Das2ServerInfo.class) {
                    return "DAS/2";
                }
                if (c == DasServerInfo.class) {
                    return "DAS";
                }
                if (c == QuickLoadServerModel.class) {
                    return "Quickload";
                }
                return "unknown";
            case 4:
                return this.LoadStatusMap.get(features.get(row).loadStatus);
            default:
                System.out.println("Shouldn't reach here: " + row + " " + col);
                return null;
        }
	}

    @Override
	public Class getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}

    @Override
	public boolean isCellEditable(int row, int col) {
		if (col == LOAD_STRATEGY_COLUMN) { return true; }
		else { return false; }
	}

    @Override
	public void setValueAt(Object value, int row, int col) {
		GenericFeature gFeature = features.get(row);
        String valueString = value.toString();
        
		if (col == LOAD_STRATEGY_COLUMN)  {
            if (!this.LoadStrategyMap.get(gFeature.loadStrategy).equals(valueString)) {
                // strategy changed.  Update the feature object.
                gFeature.loadStrategy = this.reverseLoadStrategyMap.get(valueString);
                fireTableCellUpdated(row, col);

                if (gFeature.loadStrategy == LoadStrategy.WHOLE) {
                    //  For features with "visible range", we don't dynamically load the feature.
                    System.out.println("Selected : " + gFeature.featureName);
                    this.glu.loadAndDisplayAnnotations(gFeature);
                    Application.getSingleton().setStatus("", false);
                }

            }
		}
        /*else if (col == LOAD_STATUS_COLUMN) {
            if (!this.LoadStatusMap.get(gFeature.loadStatus).equals(valueString)) {
                // status changed.
                gFeature.loadStatus = (LoadStatus)value;
                fireTableCellUpdated(row, col);
            }
        }*/
	}

	public void stateChanged(ChangeEvent evt) {
		Object src = evt.getSource();
        System.out.println("FeaturesTableModel.stateChanged() called, source:" + src);
		if (src instanceof GenericFeature) {
			int row = getRow((GenericFeature)src);
			if (row >=0) {  // if typestate is present in table, then send notification of row change
				fireTableRowsUpdated(row, row);
			}
		}
	}

}
