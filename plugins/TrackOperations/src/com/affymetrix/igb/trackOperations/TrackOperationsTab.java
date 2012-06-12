package com.affymetrix.igb.trackOperations;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
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
import com.affymetrix.igb.shared.*;
import com.affymetrix.igb.thresholding.action.ThresholdingAction;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.MessageFormat;
import java.util.*;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public final class TrackOperationsTab implements SeqSelectionListener, SymSelectionListener {

	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("trackOperations");
	private static TrackOperationsTab singleton;
	private static GenometryModel gmodel;
	boolean is_listening = true; // used to turn on and off listening to GUI events
	boolean DEBUG_EVENTS = false;
	public final List<RootSeqSymmetry> rootSyms = new ArrayList<RootSeqSymmetry>();
	public final List<ViewModeGlyph> glyphs = new ArrayList<ViewModeGlyph>();
	public final JRPButton threshB = new JRPButton("TrackOperationsTab_threshB");
	public final JRPButton combineB = new JRPButton("TrackOperationsTab_combineB", CombineGraphsAction.getAction());
	public final JRPButton splitB = new JRPButton("TrackOperationsTab_splitB", SplitGraphsAction.getAction());
	private final IGBService igbService;
	private final ThresholdingAction thresholdingAction;
	private final HoverEffect hovereffect;

	private final Map<String, Operator> name2operation;
	public final JLabel operationLabel = new JLabel(BUNDLE.getString("operationLabel"));
	public final JRPComboBoxWithSingleListener operationCB = new JRPComboBoxWithSingleListener("TrackOperationsTab_operation");
	public final JRPButton operationGoB = new JRPButton("TrackOperationsTab_operationGoB");
	public final JRPButton operationReplaceB = new JRPButton("TrackOperationsTab_operationReplaceB");
	public final JLabel operationParamLabel = new JLabel("base");
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
		name2operation = new HashMap<String, Operator>();
		hovereffect = new HoverEffect();
		
		operationCB.addMouseListener(hovereffect);
		operationCB.addItemListener(operationListener);

		operationGoB.setAction(new TrackOperationAction(igbService.getSeqMapView(), null) {
			private static final long serialVersionUID = 1L;
			@Override
			protected Operator getOperator() {
				String selection = (String) operationCB.getSelectedItem();
				return name2operation.get(selection);
			}
		});
		operationReplaceB.setVisible(false);
		thresholdingAction = ThresholdingAction.createThresholdingAction(igbService);
		threshB.setAction(thresholdingAction);
		resetSelectedGlyphs(Collections.<RootSeqSymmetry>emptyList());
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
		List<AbstractGraphGlyph> graphGlyphs = new ArrayList<AbstractGraphGlyph>();
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
					if (vg instanceof AbstractGraphGlyph) {
						graphGlyphs.add((AbstractGraphGlyph)vg);
					}
				}
			}
		}
		for (Glyph glyph : igbService.getSelectedTierGlyphs()) { // should not have to do this
			ViewModeGlyph vg = ((TierGlyph) glyph).getViewModeGlyph();
			if (vg instanceof MultiGraphGlyph) {
				glyphs.add(vg);
			}
		}
		thresholdingAction.setGraphs(graphGlyphs);
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

	private void loadOperators() {
		operationCB.removeAllItems();
		name2operation.clear();
		if (rootSyms.isEmpty()) {
			return;
		}
		TreeSet<Operator> operators = new TreeSet<Operator>(new OperatorComparator());
		operators.addAll(ExtensionPointHandler.getExtensionPoint(Operator.class).getExtensionPointImpls());
		List<RootSeqSymmetry> transformSyms = new ArrayList<RootSeqSymmetry>(); // fake List to test compatibility of Transform operations
		transformSyms.add(rootSyms.get(0));
		for (Operator operator : operators) {
			if (TrackUtils.getInstance().checkCompatible(rootSyms, operator, true)) {
				name2operation.put(operator.getDisplay(), operator);
				operationCB.addItem(operator.getDisplay());
			}
		}
	}

	public void setPanelEnabled() {
		is_listening = false; // turn off propagation of events from the GUI while we modify the settings
		FileTypeCategory saveCategory = null;

		boolean any_are_combined = false; // are any selections inside a combined tier
		boolean all_are_combined = true;  // are all selections inside (a) combined tier(s)
		boolean any_graphs = false;       // are any selections graph tracks

		// Now loop through other glyphs if there are more than one
		// and see if the graph_style and heatmap are the same in all selections
		for (ViewModeGlyph gl : glyphs) {
			boolean this_one_is_combined = gl instanceof MultiGraphGlyph;
			any_are_combined = any_are_combined || this_one_is_combined;
			all_are_combined = all_are_combined && this_one_is_combined;
			if (gl instanceof AbstractGraphGlyph && !(gl instanceof MultiGraphGlyph)) {
				any_graphs = true;
			}
			RootSeqSymmetry rootSym = (RootSeqSymmetry)gl.getInfo();
			if (rootSym != null) {
				FileTypeCategory category = rootSym.getCategory();
				if (saveCategory == null) {
					saveCategory = category;
				}
			}
		}

		combineB.setEnabled(!all_are_combined && any_graphs);
		splitB.setEnabled(any_are_combined);
		threshB.setEnabled(any_graphs);
		int operatorCount = name2operation.size();
		operationLabel.setEnabled(operatorCount > 0);
		operationCB.setEnabled(operatorCount > 0);
		operationGoB.setEnabled(operatorCount > 0);

		is_listening = true; // turn back on GUI events
	}

	private void setOperationDisplay() {
		String selection = (String) operationCB.getSelectedItem();
		if (selection == null) {
			operationParamLabel.setText(" ");
			operationParamLabel.setEnabled(false);
			operationParam.setEditable(false);
			operationParam.setEnabled(false);
		} else {
			Operator operator = name2operation.get(selection);
			operationGoB.setToolTipText(getTooltipMessage(operator));
			Map<String, Class<?>> params = operator.getParameters();
			if (params == null || params.isEmpty()) {
				operationParamLabel.setText(" ");
				operationParamLabel.setEnabled(false);
				operationParam.setEditable(false);
				operationParam.setEnabled(false);
			} else {
				operationParamLabel.setText(params.keySet().iterator().next() + " :"); // only one parameter, for now
				operationParamLabel.setEnabled(true);
				operationParam.setEditable(true);
				operationParam.setText("");
				operationParam.setEnabled(true);
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

	public List<ViewModeGlyph> getSelectedGlyphss() {
		return glyphs;
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
			if (operator == null) {
				return;
			}

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
			if (operator != null && operator.getOperandCountMin(FileTypeCategory.Graph) == 2 && operator.getOperandCountMax(FileTypeCategory.Graph) == 2) {
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
			if (operator != null && operator.getOperandCountMin(FileTypeCategory.Graph) == 2 && operator.getOperandCountMax(FileTypeCategory.Graph) == 2) {
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
