package com.affymetrix.igb.trackOperations;

import java.awt.event.*;
import java.text.MessageFormat;
import java.util.*;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.affymetrix.common.ExtensionPointHandler;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.*;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.operator.OperatorComparator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.util.ThreadUtils;

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.genoviz.swing.recordplayback.JRPComboBoxWithSingleListener;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.SeqMapViewI;
import com.affymetrix.igb.shared.*;
import com.affymetrix.igb.thresholding.action.ThresholdingAction;

public final class TrackOperationsTab implements SeqSelectionListener, SymSelectionListener {

	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("trackOperations");
	private static TrackOperationsTab singleton;
	private static GenometryModel gmodel;
	boolean is_listening = true; // used to turn on and off listening to GUI events
	boolean DEBUG_EVENTS = false;
	public final List<RootSeqSymmetry> rootSyms = new ArrayList<RootSeqSymmetry>();
	public final List<ViewModeGlyph> glyphs = new ArrayList<ViewModeGlyph>();
	private Map<FileTypeCategory, Integer> categoryCounts;
	public final JRPButton threshB = new JRPButton("TrackOperationsTab_threshB");
	public final JRPButton combineB = new JRPButton("TrackOperationsTab_combineB", CombineGraphsAction.getAction());
	public final JRPButton splitB = new JRPButton("TrackOperationsTab_splitB", SplitGraphsAction.getAction());
	private IGBService igbService;
	private final HoverEffect hovereffect;

	private final Map<String, Operator> name2transformation;
	public final JLabel transformation_label = new JLabel(BUNDLE.getString("transformationLabel"));
	public final JRPComboBoxWithSingleListener transformationCB = new JRPComboBoxWithSingleListener("TrackOperationsTab_transformation");
	public final JRPButton transformationGoB = new JRPButton("TrackOperationsTab_transformationGoB", BUNDLE.getString("goButton"));
	public final JLabel transformationParamLabel = new JLabel();
	public final JTextField transformationParam = new JTextField();
	private final ItemListener transformationListener = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			setTransformationDisplay();
		}
	};

	private final Map<String, Operator> name2operation;
	public final JLabel operation_label = new JLabel(BUNDLE.getString("operationLabel"));
	public final JRPComboBoxWithSingleListener operationCB = new JRPComboBoxWithSingleListener("TrackOperationsTab_operation");
	public final JRPButton operationGoB = new JRPButton("TrackOperationsTab_operationGoB", BUNDLE.getString("goButton"));
	public final JLabel operationParamLabel = new JLabel();
	public final JTextField operationParam = new JTextField();
	private final ItemListener operationListener = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			setOperationDisplay();
		}
	};

	public static void init(IGBService igbService) {
		singleton = new TrackOperationsTab(igbService);
		gmodel = GenometryModel.getGenometryModel();
		gmodel.addSeqSelectionListener(singleton);
		gmodel.addSymSelectionListener(singleton);
	}

	public static synchronized TrackOperationsTab getSingleton() {
		return singleton;
	}

	public TrackOperationsTab(IGBService igbS) {
		igbService = igbS;
		name2transformation = new HashMap<String, Operator>();
		name2operation = new HashMap<String, Operator>();
		hovereffect = new HoverEffect();
		transformationCB.addItemListener(transformationListener);

		operationCB.addMouseListener(hovereffect);
		operationCB.addItemListener(operationListener);

		transformationGoB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String selection = (String) transformationCB.getSelectedItem();
				Operator operator = name2transformation.get(selection);
				SeqMapViewI gviewer = igbService.getSeqMapView();
				new TrackTransformAction(gviewer, operator).actionPerformed(e);
			}
		});

		operationGoB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String selection = (String) operationCB.getSelectedItem();
				Operator operator = name2operation.get(selection);
				SeqMapViewI gviewer = igbService.getSeqMapView();
				new TrackOperationAction(gviewer, operator).actionPerformed(e);
			}
		});
		threshB.setAction(ThresholdingAction.createThresholdingAction(igbS));
		resetSelectedGlyphs(Collections.<RootSeqSymmetry>emptyList());
	}

	public boolean isTierGlyph(GlyphI glyph) {
		return glyph instanceof TierGlyph;
	}

	public void addOperator(Operator operator) {
		loadOperators();
	}

	public void removeOperator(Operator operator) {
		loadOperators();
	}

	public void symSelectionChanged(SymSelectionEvent evt) {
		List<RootSeqSymmetry> selected_syms = evt.getAllSelectedSyms();
		// Only pay attention to selections from the main SeqMapView or its map.
		// Ignore the splice view as well as events coming from this class itself.
		Object src = evt.getSource();
		if (!(src == igbService.getSeqMapView() || src == igbService.getSeqMap())) {
			return;
		}
		resetSelectedGlyphs(selected_syms);
	}

	private void resetSelectedGlyphs(List<RootSeqSymmetry> selected_syms) {
		is_listening = false; // turn off propagation of events from the GUI while we modify the settings
		int symcount = selected_syms.size();
		collectGraphsAndGlyphs(selected_syms, symcount);
		loadOperators();
		setPanelEnabled();
		setOperationDisplay();
		is_listening = true; // turn back on GUI events
	}

	private void collectGraphsAndGlyphs(List<RootSeqSymmetry> selected_syms, int symcount) {
		if (rootSyms != selected_syms) {
			// in certain cases selected_syms arg and syms list may be same, for example when method is being
			//     called to catch changes in syms representing selected sym, not the syms themselves)
			//     therefore don't want to change syms list if same as selected_syms (especially don't want to clear it!)
			rootSyms.clear();
		}
		glyphs.clear();
		// First loop through and collect syms and glyphs
		for (int i = 0; i < symcount; i++) {
			RootSeqSymmetry sym = selected_syms.get(i);
			// only add to syms if list is not identical to selected_syms arg
			if (rootSyms != selected_syms) {
				rootSyms.add(sym);
			}
			// add all glyphs representing sym
			//	  System.out.println("found multiple glyphs for sym: " + multigl.size());
			for (Glyph g : igbService.getVisibleTierGlyphs()) {
				ViewModeGlyph vg = ((TierGlyph) g).getViewModeGlyph();
				if (vg instanceof MultiGraphGlyph) {
					for (GlyphI child : vg.getChildren()) {
						if (rootSyms.contains(child.getInfo())) {
							glyphs.add((ViewModeGlyph) child);
						}
					}
				} else if (rootSyms.contains(vg.getInfo())) {
					glyphs.add(vg);
				}
			}
		}
		categoryCounts = getCategoryCounts();
	}

	public void seqSelectionChanged(SeqSelectionEvent evt) {
		if (DEBUG_EVENTS) {
			System.out.println("SeqSelectionEvent, selected seq: " + evt.getSelectedSeq() + " received by " + this.getClass().getName());
		}
//		current_seq = evt.getSelectedSeq();
//		resetSelectedGlyphs(gmodel.getSelectedSymmetries(current_seq));
		resetSelectedGlyphs(Collections.<RootSeqSymmetry>emptyList());
	}

	public void updateViewer() {
		final List<RootSeqSymmetry> previous_syms = new ArrayList<RootSeqSymmetry>(rootSyms);
		// set selections to empty so that options get turned off
		resetSelectedGlyphs(Collections.<RootSeqSymmetry>emptyList());
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				igbService.getSeqMapView().setAnnotatedSeq(gmodel.getSelectedSeq(), true, true);
				resetSelectedGlyphs(previous_syms);
			}
		});
	}

	public void trackstylePropertyChanged(EventObject eo) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	private Map<FileTypeCategory, Integer> getCategoryCounts() {
		Map<FileTypeCategory, Integer> categoryCounts = new HashMap<FileTypeCategory, Integer>();
		for (FileTypeCategory category : FileTypeCategory.values()) {
			categoryCounts.put(category, 0);
		}
		for (RootSeqSymmetry rootSym : rootSyms) {
			FileTypeCategory category = rootSym.getCategory();
			categoryCounts.put(category, categoryCounts.get(category) + 1);
		}
		return categoryCounts;
	}

	private void loadOperators() {
		transformationCB.removeAllItems();
		name2transformation.clear();
		operationCB.removeAllItems();
		name2operation.clear();
		if (rootSyms.size() == 0) {
			return;
		}
		FileTypeCategory transformCategory = null;
		FileTypeCategory testTransformCategory = null;
		for (FileTypeCategory category : FileTypeCategory.values()) {
			if (categoryCounts.get(category) > 0) {
				if (testTransformCategory == null) {
					testTransformCategory = category;
					transformCategory = category;
				}
				if (testTransformCategory != category) {
					transformCategory = null;
				}
			}
		}
		TreeSet<Operator> operators = new TreeSet<Operator>(new OperatorComparator());
		operators.addAll(ExtensionPointHandler.getExtensionPoint(Operator.class).getExtensionPointImpls());
		for (Operator operator : operators) {
			boolean operatorOK = true;
			boolean transformOK = true;
			for (FileTypeCategory category : FileTypeCategory.values()) {
				int count = categoryCounts.get(category);
				if (count < operator.getOperandCountMin(category) || count > operator.getOperandCountMax(category)) {
					operatorOK = false;
				}
				if (category == transformCategory) {
					if (operator.getOperandCountMin(category) != 1 || operator.getOperandCountMax(category) != 1) {
						transformOK = false;
					}
				}
				else {
					if (operator.getOperandCountMax(category) > 0) {
						transformOK = false;
					}
				}
			}
			if (transformOK) {
				name2transformation.put(operator.getDisplay(), operator);
				transformationCB.addItem(operator.getDisplay());
			}
			if (operatorOK) {
				name2operation.put(operator.getDisplay(), operator);
				operationCB.addItem(operator.getDisplay());
			}
		}
	}

	public void setPanelEnabled() {
		is_listening = false; // turn off propagation of events from the GUI while we modify the settings
		FileTypeCategory saveCategory = null;

		boolean any_are_combined = false; // are any selections inside a combined tier
		boolean all_are_combined = false; // are all selections inside (a) combined tier(s)
		boolean any_graphs = false;       // are any selections graph tracks
		boolean all_same = true;         // all tracks are the same type

		// Now loop through other glyphs if there are more than one
		// and see if the graph_style and heatmap are the same in all selections
		for (ViewModeGlyph gl : glyphs) {
			if (gl instanceof AbstractGraphGlyph) {
				any_graphs = true;
				boolean this_one_is_combined = (((AbstractGraphGlyph)gl).getGraphState().getComboStyle() != null);
				any_are_combined = any_are_combined || this_one_is_combined;
				all_are_combined = all_are_combined && this_one_is_combined;
			}
			RootSeqSymmetry rootSym = (RootSeqSymmetry)gl.getInfo();
			FileTypeCategory category = rootSym.getCategory();
			if (saveCategory == null) {
				saveCategory = category;
			}
			else {
				all_same &= (saveCategory == category);
			}
		}

		combineB.setEnabled(!all_are_combined && any_graphs);
		splitB.setEnabled(any_are_combined);
		threshB.setEnabled(any_graphs);
		int operatorCount = name2operation.size();
		transformationCB.setEnabled(all_same && operatorCount > 0);
		transformationGoB.setEnabled(all_same && operatorCount > 0);
		operationCB.setEnabled(operatorCount > 0);
		operationGoB.setEnabled(operatorCount > 0);

		is_listening = true; // turn back on GUI events
	}

	private void setTransformationDisplay() {
		set___AtionDisplay(transformationCB, transformationParamLabel, transformationParam, name2transformation, transformationGoB);
	}

	private void setOperationDisplay() {
		set___AtionDisplay(operationCB, operationParamLabel, operationParam, name2operation, operationGoB);
	}

	private void set___AtionDisplay(
		JRPComboBoxWithSingleListener ationCB,
		JLabel ationLabel,
		JTextField ationParam,
		Map<String, Operator> name2ation,
		JRPButton ationGoB
		) {
		String selection = (String) ationCB.getSelectedItem();
		if (selection == null) {
			ationLabel.setText("");
			ationParam.setVisible(false);
		} else {
			Operator operator = name2ation.get(selection);
			ationGoB.setToolTipText(getTooltipMessage(operator));
			Map<String, Class<?>> params = operator.getParameters();
			if (params == null || params.size() == 0) {
				ationLabel.setText("");
				ationParam.setVisible(false);
			} else {
				ationLabel.setText(params.keySet().iterator().next());
				ationParam.setText("");
				ationParam.setVisible(true);
			}
		}
	}

	/**
	 * get the error message text for an attempted graph/annotation operation
	 * @param graphCount the number of graph glyphs
	 * @param minCount the minimum graphs for the operator
	 * @param maxCount the maximum graphs for the operator
	 * @return the error message text
	 */
	private static final String selectExactGraphsMessage = BUNDLE.getString("operatorExactTooltip");
	private static final String selectMinGraphsMessage = BUNDLE.getString("operatorMinTooltip");
	private static final String selectMaxGraphsMessage = BUNDLE.getString("operatorMaxTooltip");
	private static final String selectRangeGraphsMessage = BUNDLE.getString("operatorRangeTooltip");
	public static String getTooltipMessage(Operator operator) {
		if (operator == null) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		for (FileTypeCategory category : FileTypeCategory.values()) {
			int minCount = operator.getOperandCountMin(category);
			int maxCount = operator.getOperandCountMax(category);
			String categoryName = category.toString(); // not translated for now
			if (maxCount > 0) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				if (minCount == maxCount) {
					sb.append(MessageFormat.format(selectExactGraphsMessage, minCount, categoryName));
				}
				else if (minCount == 0) {
					sb.append(MessageFormat.format(selectMaxGraphsMessage, maxCount, categoryName));
				}
				else if (maxCount == Integer.MAX_VALUE) {
					sb.append(MessageFormat.format(selectMinGraphsMessage, minCount, categoryName));
				}
				else {
					sb.append(MessageFormat.format(selectRangeGraphsMessage, minCount, maxCount, categoryName));
				}
			}
		}
		return sb.toString();
	}

	public List<RootSeqSymmetry> getRootSyms() {
		return rootSyms;
	}

	public IGBService getIgbService() {
		return igbService;
	}

	private class HoverEffect implements MouseListener {

		private String A = null;
		private String B = null;

		public void mouseClicked(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
			JRPComboBoxWithSingleListener comp = (JRPComboBoxWithSingleListener) e.getComponent();
			String selection = (String) comp.getSelectedItem();
			Operator operator = name2operation.get(selection);

			if (rootSyms.size() >= operator.getOperandCountMin(FileTypeCategory.Graph)
					&& rootSyms.size() <= operator.getOperandCountMax(FileTypeCategory.Graph)) {
				setGraphName(comp, operator);
			} else {
				comp.setToolTipText(getTooltipMessage(operator));
			}
		}

		public void mouseExited(MouseEvent e) {
			JRPComboBoxWithSingleListener comp = (JRPComboBoxWithSingleListener) e.getComponent();
			String selection = (String) comp.getSelectedItem();
			unsetGraphName(name2operation.get(selection));
		}

		public void setGraphName(JRPComboBoxWithSingleListener comp, Operator operator) {
			if (operator.getOperandCountMin(FileTypeCategory.Graph) == 2 && operator.getOperandCountMax(FileTypeCategory.Graph) == 2) {
				A = ((GraphSym)rootSyms.get(0)).getGraphName();
				B = ((GraphSym)rootSyms.get(1)).getGraphName();

				((GraphSym)rootSyms.get(0)).setGraphName("A");
				((GraphSym)rootSyms.get(1)).setGraphName("B");

				comp.setToolTipText(null);
				ThreadUtils.runOnEventQueue(new Runnable() {

					public void run() {
						igbService.getSeqMap().updateWidget();
					}
				});
			}
		}

		public void unsetGraphName(Operator operator) {
			if (operator.getOperandCountMin(FileTypeCategory.Graph) == 2 && operator.getOperandCountMax(FileTypeCategory.Graph) == 2) {
				if (A != null && B != null && rootSyms.size() > 1) {
					((GraphSym)rootSyms.get(0)).setGraphName(A);
					((GraphSym)rootSyms.get(1)).setGraphName(B);

					ThreadUtils.runOnEventQueue(new Runnable() {

						public void run() {
							igbService.getSeqMap().updateWidget();
						}
					});
					A = null;
					B = null;

				}
			}
		}
	}
}
