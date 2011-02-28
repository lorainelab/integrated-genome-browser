package com.affymetrix.igb.view;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
//import com.affymetrix.genometryImpl.comparator.SeqSymIdComparator;
import com.affymetrix.genometryImpl.comparator.SeqSymIdComparator;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.util.DisplayUtils;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.action.LoadSequence;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.osgi.service.OrientableTableModel;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

public final class SeqGroupView extends IGBTabPanel implements ListSelectionListener, GroupSelectionListener, SeqSelectionListener {
	private static final long serialVersionUID = 1L;
	private static final boolean DEBUG_EVENTS = false;
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	private static final String LOAD = IGBConstants.BUNDLE.getString("load");
	private final JTable seqtable;
	private final JTableHeader seqtableheader;
	private SeqMapView gviewer;
	private BioSeq selected_seq = null;
	private AnnotatedSeqGroup previousGroup = null;
	private int previousSeqCount = 0;
	private final JButton all_residuesB;
	private final JButton partial_residuesB;
	private final ListSelectionModel lsm;
	private OrientableTableModel omod;
	private TableRowSorter<SeqGroupTableModel> sorter;
	private String most_recent_seq_id = null;
	private static final NumberFormat nformat = NumberFormat.getIntegerInstance(Locale.ENGLISH);

	private static SeqGroupView singleton;

	public static void init(IGBService _igbService) {
		singleton = new SeqGroupView(_igbService);
	}

	public static SeqGroupView getInstance() {
		return singleton;
	}

	private SeqGroupView(IGBService _igbService) {
		super(_igbService, BUNDLE.getString("sequenceTab"), BUNDLE.getString("sequenceTab"), false, 1);
		gviewer = (SeqMapView)igbService.getMapView();
		this.setLayout(new BorderLayout());
		seqtable = new JTable() {
			private static final long serialVersionUID = 1L;
			TableCellRenderer lengthRenderer = new LengthRenderer();
			public TableCellRenderer getCellRenderer(int row, int column) {
				if ((portrait && column == 1) || (!portrait && row == 1 && column > 0)) {
					return lengthRenderer;
				}
				else {
					return super.getCellRenderer(row, column);
				}
		    }
		};
		seqtableheader = seqtable.getTableHeader();

		seqtable.setToolTipText(BUNDLE.getString("chooseSeq"));
		seqtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		seqtable.setFillsViewportHeight(true);
		
		SeqGroupTableModel mod = new SeqGroupTableModel(null);
		omod = new OrientableTableModel(mod);
		setPortraitInternal(portrait);
		seqtable.setModel(omod);	// Force immediate visibility of column headers (although there's no data).

		JScrollPane scroller = new JScrollPane(seqtable);
		scroller.setBorder(BorderFactory.createCompoundBorder(
				scroller.getBorder(),
				BorderFactory.createEmptyBorder(0, 2, 0, 2)));

		JPanel tablePanel = new JPanel();
		tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
//		tablePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		tablePanel.add(scroller);
		this.add("Center", tablePanel);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 2));

		all_residuesB = new JButton(LoadSequence.getWholeAction());
		all_residuesB.setToolTipText(MessageFormat.format(LOAD,IGBConstants.BUNDLE.getString("nucleotideSequence")));
		all_residuesB.setMaximumSize(all_residuesB.getPreferredSize());
		all_residuesB.setEnabled(false);
		buttonPanel.add(all_residuesB);
		partial_residuesB = new JButton(LoadSequence.getPartialAction());
		partial_residuesB.setToolTipText(MessageFormat.format(LOAD,IGBConstants.BUNDLE.getString("partialNucleotideSequence")));
		partial_residuesB.setMaximumSize(partial_residuesB.getPreferredSize());
		partial_residuesB.setEnabled(false);
		buttonPanel.add(partial_residuesB);
		this.add("South", buttonPanel);

		gmodel.addGroupSelectionListener(this);
		gmodel.addSeqSelectionListener(this);
		lsm = seqtable.getSelectionModel();
		lsm.addListSelectionListener(this);
	}

	@Override
	public boolean isOrientable() {
		return true;
	}

	@Override
	public void setPortrait(boolean portrait) {
		if (this.portrait == portrait) {
			return;
		}
		super.setPortrait(portrait);
		setPortraitInternal(portrait);
	}

	private void setPortraitInternal(boolean portrait) {
		omod.setReverse(!portrait);
		seqtable.setRowSelectionAllowed(portrait);
		seqtable.setColumnSelectionAllowed(!portrait);
		seqtable.setAutoCreateRowSorter(!portrait);
		seqtable.setRowSorter(portrait ? null : sorter);
		seqtable.setTableHeader(portrait ? seqtableheader : null);
	}

	public void enableButtons(boolean enabled) {
		all_residuesB.setEnabled(enabled);
		partial_residuesB.setEnabled(enabled);
	}

	/**
	 * Handles clicking of partial residue, all residue, and refresh data buttons.
	 * @param evt
	 */
	public void loadResidues(AbstractAction action) {
		Object src = null;

		if(action.equals(partial_residuesB.getAction())){
			src = partial_residuesB;
		}else if (action.equals(all_residuesB.getAction())){
			src = all_residuesB;
		}

		if (src != partial_residuesB && src != all_residuesB) {
			return;
		}

		final BioSeq seq = gmodel.getSelectedSeq();
		final boolean partial = src == partial_residuesB;
		
		SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

			public Boolean doInBackground() {
				return GeneralLoadView.getInstance().loadResidues(seq, gviewer.getVisibleSpan(), partial, false, true);
			}

			@Override
			public void done() {
				try {
					if (get()) {
						gviewer.setAnnotatedSeq(seq, true, true, true);
					}
				} catch (Exception ex) {
					Logger.getLogger(GeneralLoadView.class.getName()).log(Level.SEVERE, null, ex);
				} finally{
					igbService.removeNotLockedUpMsg("Loading residues for " + seq.getID());
				}
			}
		};

		// Use a SwingWorker to avoid locking up the GUI.
		ThreadUtils.getPrimaryExecutor(src).execute(worker);
	}

  /**
   * Refresh seqtable if more chromosomes are added, for example.
   */
  public void refreshTable() {
	  seqtable.validate();
	  seqtable.updateUI();
	  seqtable.repaint();
	  updateTableHeader();
  }

  public void updateTableHeader(){
	TableColumnModel model = seqtableheader.getColumnModel();

	TableColumn col1 = model.getColumn(0);
	col1.setHeaderValue(MessageFormat.format(IGBConstants.BUNDLE.getString("sequenceColumnHeader"), nformat.format(seqtable.getRowCount())));
  }

  public void groupSelectionChanged(GroupSelectionEvent evt) {
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		if (SeqGroupView.DEBUG_EVENTS) {
			System.out.println("SeqGroupView received groupSelectionChanged() event");
			if (group == null) {
				System.out.println("  group is null");
			} else {
				System.out.println("  group: " + group.getID());
				System.out.println("  seq count: " + group.getSeqCount());
			}
		}
		if (previousGroup == group) {
			if (group == null) {
				return;
			}
			warnAboutNewlyAddedChromosomes(previousSeqCount, group);
		}

		previousGroup = group;
		previousSeqCount = group == null ? 0 : group.getSeqCount();


		SeqGroupTableModel mod = new SeqGroupTableModel(group);
		omod = new OrientableTableModel(mod);
		setPortraitInternal(portrait);
		sorter = new TableRowSorter<SeqGroupTableModel>(mod){
			@Override
			public Comparator<?> getComparator(int column){
				if(column == 0){
					return String.CASE_INSENSITIVE_ORDER;
				}
				return new SeqLengthComparator();
			}
		};
		selected_seq = null;
		seqtable.setModel(omod);
		
		refreshTable();

		if (group != null && most_recent_seq_id != null) {
			// When changing genomes, try to keep the same chromosome selected when possible
			BioSeq aseq = group.getSeq(most_recent_seq_id);
			if (aseq != null) {
				gmodel.setSelectedSeq(aseq);
			}
		}
		if (group != null && group.getEnabledVersions().isEmpty()) {
			enableButtons(false);
		}
	}

	private static void warnAboutNewlyAddedChromosomes(int previousSeqCount, AnnotatedSeqGroup group) {
		if (previousSeqCount > group.getSeqCount()) {
			System.out.println("WARNING: chromosomes have been added");
			if (previousSeqCount < group.getSeqCount()) {
				System.out.print("New chromosomes:");
				for (int i = previousSeqCount; i < group.getSeqCount(); i++) {
					System.out.print(" " + group.getSeq(i).getID());
				}
				System.out.println();
			}
		}
	}

  public void seqSelectionChanged(SeqSelectionEvent evt) {
		if (SeqGroupView.DEBUG_EVENTS) {
			System.out.println("SeqGroupView received seqSelectionChanged() event: seq is " + evt.getSelectedSeq());
		}
		synchronized (seqtable) {  // or should synchronize on lsm?
			lsm.removeListSelectionListener(this);
			selected_seq = evt.getSelectedSeq();
			if (selected_seq == null) {
				seqtable.clearSelection();
			} else {
				most_recent_seq_id = selected_seq.getID();

				int rowCount = seqtable.getRowCount();
				for (int i = 0; i < rowCount; i++) {
					// should be able to use == here instead of equals(), because table's model really returns seq.getID()
					if (most_recent_seq_id == seqtable.getValueAt(i, 0)) {
						if (seqtable.getSelectedRow() != i) {
							seqtable.setRowSelectionInterval(i, i);
							scrollTableLater(seqtable, i);
						}
						break;
					}
				}
				AnnotatedSeqGroup group = selected_seq.getSeqGroup();
				if (group != null && group.getEnabledVersions().isEmpty()) {
					enableButtons(false);
				}
			}
			lsm.addListSelectionListener(this);
		}
	}

  // Scroll the table such that the selected row is visible
  void scrollTableLater(final JTable table, final int i) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        // Check the row count first since this is multi-threaded
        if (table.getRowCount() >= i) {
          DisplayUtils.scrollToVisible(table, i, 0);
        }
      }
    });
  }

  public void valueChanged(ListSelectionEvent evt) {
    Object src = evt.getSource();
    if ((src == lsm) && (! evt.getValueIsAdjusting())) { // ignore extra messages
      if (SeqGroupView.DEBUG_EVENTS)  { System.out.println("SeqGroupView received valueChanged() ListSelectionEvent"); }
      int srow = seqtable.getSelectedRow();
      if (srow >= 0)  {
        String seq_name = (String) seqtable.getValueAt(srow, 0);
        selected_seq = gmodel.getSelectedSeqGroup().getSeq(seq_name);
        if (selected_seq != gmodel.getSelectedSeq()) {
          gmodel.setSelectedSeq(selected_seq);
        }
      }
    }
  }

  @Override
  public Dimension getMinimumSize() { return new Dimension(220, 50); }
    @Override
  public Dimension getPreferredSize() { return new Dimension(220, 50); }

	private final class SeqLengthComparator implements Comparator<String>{

		public int compare(String o1, String o2) {
			if (o1 == null || o2 == null) {
				return SeqSymIdComparator.compareNullIDs(o2, o1);	// null is last
			}
			if (o1.length() == 0 || o2.length() == 0) {
				return o2.compareTo(o1);	// empty string is last
			}

			// use valueOf to get a Long object versus a long primitive.
			return Long.valueOf(o1).compareTo(Long.parseLong(o2));
		}
	}

	final class LengthRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		public LengthRenderer(){
			setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		}
		
		@Override
		public Component getTableCellRendererComponent(
				JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {

			if (value.toString().length() == 0) {
				return super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
			}
			return super.getTableCellRendererComponent(table, nformat.format(Double.valueOf(value.toString())),
					isSelected, hasFocus, row, column);
		}
	}
}
