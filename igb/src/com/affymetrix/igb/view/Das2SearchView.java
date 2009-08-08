package com.affymetrix.igb.view;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.util.List;

import javax.swing.EditableComboBox;

import com.affymetrix.igb.*;
import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.comparator.Das2FeatureComparator;
import com.affymetrix.genometryImpl.das2.SimpleDas2Feature;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.das2.*;


/**
 *  Strategy is to call Das2VersionedSource.getFeaturesByName()
 *     (either only the "current version" or all known versions that match current seq group)
 *  Should get back list of results as syms (returned from DAS/2 server in das2feature XML format)
 *  However, want to configure parsing so that results are NOT added as annotations to seqs,
 *      rather they are displayed as a list to choose from
 *  When a result is selected, the result sym's range is used as a guide to construct other DAS/2 queries
 *      via Das2LoadView3.loadFeatures() / loadFeaturesInView(), (potentially in optimized formats)
 *      and to set view to results sym's seq and span
 */
public final class Das2SearchView extends JPanel implements ActionListener, GroupSelectionListener {
	static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
	static boolean DEBUG_EVENTS = false;

	EditableComboBox searchCB = new EditableComboBox();
	SeqMapView gviewer;
	JTable results_table;
	//Map<String,List> search_results_history = new LinkedHashMap<String,List>();  // keeping history of search results

	public void groupSelectionChanged(GroupSelectionEvent evt) {
		if (DEBUG_EVENTS) {
			System.out.println("Das2SearchView.groupSelectionChanged() called");
		}
		// need to clear table, search history, combo box
		//search_results_history = new LinkedHashMap<String,List>();
		searchCB.reset();
		results_table.setModel(new SearchResultsTableModel(new ArrayList()));
	}

	public Das2SearchView() {
		super();
		gviewer = Application.getSingleton().getMapView();
		this.setLayout(new BorderLayout());

		JPanel pan1 = new JPanel();
		pan1.add(new JLabel("Name Search (EXPERIMENTAL, DAS2 SERVERS ONLY): "));
		pan1.add(searchCB);
		this.add("North", pan1);
		searchCB.addActionListener(this);

		results_table = new JTable();
		results_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		results_table.setModel(new SearchResultsTableModel(new ArrayList())); // initializing table with empty search results model
		JScrollPane table_scroller = new JScrollPane(results_table);

		//    this.setLayout(new BorderLayout());
		//    JPanel types_panel = new JPanel(new BorderLayout());
		//    types_panel.setBorder(new TitledBorder("Recently Accessed Annotation Types"));
		//    types_panel.add("Center", table_scroller);
		this.add("Center", table_scroller);
		results_table.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					Point p = e.getPoint();
					int row = results_table.rowAtPoint(p);
					//int column = results_table.columnAtPoint(p); // This is the view column!
					SearchResultsTableModel mod = (SearchResultsTableModel) results_table.getModel();
					SimpleDas2Feature feat = mod.getSearchResult(row);
					displaySearchResult(feat);
				// loading of WHOLE_SEQUENCE strategy types already dealt with by Das2LoadView3.seqSelectionChanged
				//    but also want to at least for range of selected annot load annots of the
				//       type that has been selected (including itself)
				//    maybe force type strategy of selected annot's type to change to VISIBLE_RANGE
				//       (or WHOLE_SEQ if type hint present)
				// parent_view.loadFeatures(Das2TypeState.WHOLE_SEQUENCE, false);
				// parent_view.loadFeatures(Das2TypeState.VISIBLE_RANGE, false);
				}
			}
		});
		gmodel.addGroupSelectionListener(this);
	}

	public void displaySearchResult(SimpleDas2Feature feat) {
		System.out.println("displaying result of name search: " + feat);
		SeqSpan span = feat.getSpan(0);
		MutableAnnotatedBioSeq seq = span.getBioSeq();
		if (gmodel.getSelectedSeq() != seq) {
			gmodel.setSelectedSeq(seq);
		}
		gviewer.zoomTo(span);
	}

	public void actionPerformed(ActionEvent evt) {
		Object src = evt.getSource();
		String command = evt.getActionCommand();
		String name = null;
		if ((src == searchCB) && (command.equalsIgnoreCase("comboBoxChanged"))) {
			name = (String) searchCB.getSelectedItem();
		}
		if (name != null) {
			System.out.println("@@@@@  trying to search for annotation name: " + name + "  @@@@@");
			searchFeaturesByName(name);
		}
	}

	public List searchFeaturesByName(String name) {
		if (name == null || name.equals("")) {
			return null;
		}
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		if (group == null) {
			return null;
		}

		List features = new ArrayList();
		for (GenericVersion gVersion : group.getVersions()) {
			if (gVersion.gServer.serverType == ServerType.DAS2) {
				Das2VersionedSource version = (Das2VersionedSource) gVersion.versionSourceObj;
				if (version != null) {
					features.addAll(searchFeaturesByName(name, version));
				}
			}
		}
		if (features.isEmpty()) {
			return null;
		}

		System.out.println("features found: " + features.size());
		Collections.sort(features, new Das2FeatureComparator(true)); // sort based on name / ID
		SearchResultsTableModel tmodel = new SearchResultsTableModel(features);
		results_table.setModel(tmodel);

		return features;
	}

	private List searchFeaturesByName(String name, Das2VersionedSource version) {
		List<SeqSymmetry> feats = version.getFeaturesByName(name, false);
		MutableComboBoxModel mcb = (MutableComboBoxModel) searchCB.getModel();
		mcb.addElement(name);

		return feats;
	}
}

class SearchResultsTableModel extends AbstractTableModel {

	static int NAME_COLUMN = 0;
	static int ID_COLUMN = 1;
	static int TYPE_COLUMN = 2;
	static int CHROM_COLUMN = 3;
	static int MIN_COLUMN = 4;
	static int MAX_COLUMN = 5;
	static int STRAND_COLUMN = 6;
	List search_results;
	String[] column_names = {"name", "ID", "type", "chromosome", "min", "max", "strand"};

	public SearchResultsTableModel(List results) {
		search_results = results;
	}

	public Object getValueAt(int row, int col) {
		SimpleDas2Feature feat = getSearchResult(row);
		SeqSpan span = feat.getSpan(0);
		Object result = "NOT_ASSIGNED";
		if (col == NAME_COLUMN) {
			result = feat.getName();
		} else if (col == ID_COLUMN) {
			result = feat.getID();
		} else if (col == TYPE_COLUMN) {
			result = feat.getType();
		} else if (col == CHROM_COLUMN) {
			result = span.getBioSeq().getID();
		} else if (col == MIN_COLUMN) {
			result = new Integer(span.getMin());
		} else if (col == MAX_COLUMN) {
			result = new Integer(span.getMax());
		} else if (col == STRAND_COLUMN) {
			result = (span.isForward() ? "+" : "-");
		}
		return result;
	}

	public int getColumnCount() {
		return column_names.length;
	}

	@Override
	public String getColumnName(int col) {
		return column_names[col];
	}

	public int getRowCount() {
		return search_results.size();
	}

	public SimpleDas2Feature getSearchResult(int row) {
		return (SimpleDas2Feature) search_results.get(row);
	}
}

