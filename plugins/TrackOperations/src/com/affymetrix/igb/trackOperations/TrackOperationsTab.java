package com.affymetrix.igb.trackOperations;

import java.awt.Color;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.Map.Entry;
import java.util.*;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.affymetrix.common.ExtensionPointHandler;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.*;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.operator.OperatorComparator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.style.SimpleTrackStyle;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.ThreadUtils;

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.genoviz.swing.recordplayback.JRPComboBoxWithSingleListener;
import com.affymetrix.genoviz.swing.recordplayback.JRPTextField;

import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.SeqMapViewI;
import com.affymetrix.igb.shared.*;

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
	public final JRPTextField paramT = new JRPTextField("TrackOperationsTab_paramT", "", 2);
	public final JRPButton combineB = new JRPButton("TrackOperationsTab_combineB", BUNDLE.getString("combineButton"));
	public final JRPButton splitB = new JRPButton("TrackOperationsTab_splitB", BUNDLE.getString("splitButton"));
	private IGBService igbService;
	private final Map<String, Operator> name2transform;
	private final Map<String, Operator> name2operator;
	public final JRPButton transformationGoB = new JRPButton("TrackOperationsTab_transformationGoB", BUNDLE.getString("goButton"));
	public final JLabel transformation_label = new JLabel(BUNDLE.getString("transformationLabel"));
	public final JRPComboBoxWithSingleListener transformationCB = new JRPComboBoxWithSingleListener("TrackOperationsTab_transformation");
	public final JLabel operation_label = new JLabel(BUNDLE.getString("operationLabel"));
	public final JRPComboBoxWithSingleListener operationCB = new JRPComboBoxWithSingleListener("TrackOperationsTab_operation");
	public final JRPButton operationGoB = new JRPButton("TrackOperationsTab_operationGoB", BUNDLE.getString("goButton"));
	private final HoverEffect hovereffect;
	private final ItemListener operationListener = new ItemListener() {

		@Override
		public void itemStateChanged(ItemEvent e) {
			setOperatorTooltip();
		}
	};

	public void setThresholdAction(GenericAction thresholdAction) {
		threshB.setAction(thresholdAction);
	}

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
		name2transform = new HashMap<String, Operator>();
		name2operator = new HashMap<String, Operator>();
		hovereffect = new HoverEffect();
		paramT.setText("");
		paramT.setEditable(false);

		transformationCB.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String selection = (String) transformationCB.getSelectedItem();
				if (selection == null) {
					paramT.setEditable(false);
				} else {
					Operator trans = name2transform.get(selection);
					Map<String, Class<?>> params = trans.getParameters();
					if (params == null) {
						paramT.setEditable(false);
					} else {
						paramT.setEditable(true);
					}
				}
			}
		});

		operationCB.addMouseListener(hovereffect);
		operationCB.addItemListener(operationListener);

		transformationGoB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String selection = (String) transformationCB.getSelectedItem();
				Operator operator = name2transform.get(selection);
				SeqMapViewI gviewer = igbService.getSeqMapView();
				new TrackTransformAction(gviewer, operator).actionPerformed(e);
			}
		});

		operationGoB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String selection = (String) operationCB.getSelectedItem();
				Operator operator = name2operator.get(selection);
				SeqMapViewI gviewer = igbService.getSeqMapView();
				new TrackOperationAction(gviewer, operator).actionPerformed(e);
			}
		});



		combineB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				combineGraphs();
			}
		});
		splitB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				splitGraphs();
			}
		});
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
		setOperatorTooltip();
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

	private void updateViewer() {
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
		transformationCB.removeAllItems();
		name2transform.clear();
		operationCB.removeAllItems();
		name2operator.clear();
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
			if (operatorOK) {
				name2operator.put(operator.getDisplay(), operator);
				operationCB.addItem(operator.getDisplay());
			}
			if (transformOK) {
				name2transform.put(operator.getDisplay(), operator);
				transformationCB.addItem(operator.getDisplay());
			}
		}
	}

	/**
	 *  Puts all selected graphs in the same tier.
	 *  Current glyph factories do not support floating the combined graphs.
	 */
	private void combineGraphs() {
		int gcount = rootSyms.size();
		float height = 0;

		// Note that the combo_style does not implement IFloatableTierStyle
		// because the glyph factory doesn't support floating combo graphs anyway.
		ITrackStyleExtended combo_style = null;
		String viewMode = "combo";

		Map<Color, Integer> colorMap = new HashMap<Color, Integer>();
		// If any of them already has a combo style, use that one
		for (int i = 0; i < gcount && combo_style == null; i++) {
			if (rootSyms.get(i) instanceof GraphSym) {
				GraphSym gsym = (GraphSym)rootSyms.get(i);
				combo_style = gsym.getGraphState().getComboStyle();
				Color col = gsym.getGraphState().getTierStyle().getBackground();
				int c = 0;
				if (colorMap.containsKey(col)) {
					c = colorMap.get(col) + 1;
				}
				colorMap.put(col, c);
			}
		}

		// otherwise, construct a new combo style
		if (combo_style == null) {
			combo_style = new SimpleTrackStyle("Joined Graphs", true);
			combo_style.setTrackName("Joined Graphs");
			combo_style.setExpandable(true);
			//	combo_style.setCollapsed(true);
			combo_style.setForeground(igbService.getDefaultForegroundColor());
			Color background = igbService.getDefaultBackgroundColor();
			int c = -1;
			for (Entry<Color, Integer> color : colorMap.entrySet()) {
				if (color.getValue() > c) {
					background = color.getKey();
				}
			}
			combo_style.setBackground(background);
			combo_style.setViewMode(viewMode);
		}

		// Now apply that combo style to all the selected graphs
		int i = 0;
		for (SeqSymmetry sym : rootSyms) {
			if (sym instanceof GraphSym) {
				GraphSym gsym = (GraphSym)sym;
				GraphState gstate = gsym.getGraphState();
				gstate.setComboStyle(combo_style, i++);
				gstate.getTierStyle().setFloatTier(false); // ignored since combo_style is set
				height += gsym.getGraphState().getTierStyle().getHeight();
			}
		}
		combo_style.setHeight(height / i);

		updateViewer();
	}

	/**
	 *  Puts all selected graphs in separate tiers by setting the
	 *  combo state of each graph's state to null.
	 */
	private void splitGraphs() {
		if (rootSyms.isEmpty()) {
			return;
		}

		for (SeqSymmetry sym : rootSyms) {
			if (sym instanceof GraphSym) {
				GraphSym gsym = (GraphSym)sym;
				GraphState gstate = gsym.getGraphState();
				gstate.setComboStyle(null, 0);

				// For simplicity, set the floating state of all new tiers to false.
				// Otherwise, have to calculate valid, non-overlapping y-positions and heights.
				gstate.getTierStyle().setFloatTier(false); // for simplicity
			}
		}
		updateViewer();
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
		int operatorCount = name2operator.size();
		transformationCB.setEnabled(all_same && operatorCount > 0);
		transformationGoB.setEnabled(all_same && operatorCount > 0);
		operationCB.setEnabled(operatorCount > 0);
		operationGoB.setEnabled(operatorCount > 0);

		paramT.setEnabled(transformationGoB.isEnabled());
		is_listening = true; // turn back on GUI events
	}

	private void setOperatorTooltip() {
		String selection = (String) operationCB.getSelectedItem();
		Operator operator = name2operator.get(selection);
		if (operator == null) {
			operationGoB.setToolTipText(null);
		} else {
			operationGoB.setToolTipText(getTooltipMessage(operator));
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
					return MessageFormat.format(selectExactGraphsMessage, minCount, categoryName);
				}
				else if (minCount == 0) {
					return MessageFormat.format(selectMaxGraphsMessage, maxCount, categoryName);
				}
				else if (maxCount == Integer.MAX_VALUE) {
					return MessageFormat.format(selectMinGraphsMessage, minCount, categoryName);
				}
				else {
					return MessageFormat.format(selectRangeGraphsMessage, minCount, maxCount, categoryName);
				}
			}
		}
		return sb.toString();
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
			Operator operator = name2operator.get(selection);

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
			unsetGraphName(name2operator.get(selection));
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
