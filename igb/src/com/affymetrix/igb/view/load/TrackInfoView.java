package com.affymetrix.igb.view.load;

import java.awt.Dimension;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.event.FeatureSelectionListener;
import com.affymetrix.genometryImpl.general.GenericFeature;

class TrackInfoView extends JComponent  {
	
	static boolean DEBUG_EVENTS = false;
	static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
	static final String NO_TRACK = "No Track Selected";
	
	JTable trackPropTable;
	BioSeq selected_track = null;
	ListSelectionModel lsm;

	public TrackInfoView() {
		trackPropTable = new JTable();
		trackPropTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				
		TrackPropertyTableModel mod = new TrackPropertyTableModel(null, null);
		trackPropTable.setModel(mod);	// Force immediate visibility of column headers (although there's no data).
		 
		JScrollPane scroller = new JScrollPane(trackPropTable);
		scroller.setBorder(BorderFactory.createCompoundBorder(scroller.getBorder(), BorderFactory.createEmptyBorder(0,2,0,2)));
		
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(Box.createRigidArea(new Dimension(0, 5)));
		this.add(scroller);
		
		this.setBorder(BorderFactory.createTitledBorder("Data Set Properties"));			
		
		 
	}
	
	public void initializeFeature(GenericFeature selectedFeature) {
		String selectedName = selectedFeature.featureName;				
		
		//reset the title
		this.setBorder(BorderFactory.createTitledBorder(selectedName + " Properties"));
		//add the data		
		Map<String, String> properties= selectedFeature.featureProps;	
		
		TrackPropertyTableModel mod = new TrackPropertyTableModel(selectedName, properties);
		trackPropTable.setModel(mod);
	
		trackPropTable.validate();
		trackPropTable.repaint();
	}
	
	@Override
	public Dimension getMinimumSize() { 
		return new Dimension(260, 50); 
	}
	
	@Override
	public Dimension getPreferredSize() { 
		return new Dimension(260, 50); 
	}
}

