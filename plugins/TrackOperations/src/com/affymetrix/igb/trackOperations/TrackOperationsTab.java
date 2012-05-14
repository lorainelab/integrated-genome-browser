package com.affymetrix.igb.trackOperations;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.genoviz.swing.recordplayback.JRPComboBoxWithSingleListener;
import com.affymetrix.genoviz.swing.recordplayback.JRPTextField;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.operator.AbstractFloatTransformer;
import com.affymetrix.genometryImpl.operator.AbstractGraphOperator;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.operator.Operator.Order;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.style.SimpleTrackStyle;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.MisMatchGraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.SeqMapViewI;
import com.affymetrix.igb.shared.*;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.Map.Entry;
import javax.swing.*;

public final class TrackOperationsTab implements SeqSelectionListener, SymSelectionListener {

	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("trackOperations");
	private static TrackOperationsTab singleton;
	private BioSeq current_seq;
	private GenometryModel gmodel;
	boolean is_listening = true; // used to turn on and off listening to GUI events
	boolean DEBUG_EVENTS = false;
	public final List<GraphSym> grafs = new ArrayList<GraphSym>();
	public final List<AbstractGraphGlyph> glyphs = new ArrayList<AbstractGraphGlyph>();
	public final JRPButton threshB = new JRPButton("SimpleGraphTab_threshB");
	public final JRPTextField paramT = new JRPTextField("SimpleGraphTab_paramT", "", 2);
	public final JRPButton combineB = new JRPButton("SimpleGraphTab_combineB", BUNDLE.getString("combineButton"));
	public final JRPButton splitB = new JRPButton("SimpleGraphTab_splitB", BUNDLE.getString("splitButton"));
	private IGBService igbService;
	public AdvancedGraphPanel advanced_panel;

	public void setThresholdAction(GenericAction thresholdAction) {
		threshB.setAction(thresholdAction);
	}

	public static void init(IGBService igbService) {
		singleton = new TrackOperationsTab(igbService);
	}

	public static synchronized TrackOperationsTab getSingleton() {
		return singleton;
	}

	public TrackOperationsTab(IGBService igbS) {
		igbService = igbS;
		advanced_panel = new TrackOperationsTab.AdvancedGraphPanel();
		resetSelectedGraphGlyphs(Collections.EMPTY_LIST);
		gmodel = GenometryModel.getGenometryModel();
		gmodel.addSeqSelectionListener(this);
		gmodel.addSymSelectionListener(this);
	}

	public boolean isTierGlyph(GlyphI glyph) {
		return glyph instanceof TierGlyph;
	}

	public void addOperator(Operator operator) {
		advanced_panel.loadOperators();
	}

	public void removeOperator(Operator operator) {
		advanced_panel.loadOperators();
	}

	public void symSelectionChanged(SymSelectionEvent evt) {
		List<SeqSymmetry> selected_syms = evt.getSelectedSyms();
		// Only pay attention to selections from the main SeqMapView or its map.
		// Ignore the splice view as well as events coming from this class itself.
		Object src = evt.getSource();
		if (!(src == igbService.getSeqMapView() || src == igbService.getSeqMap())) {
			return;
		}
		resetSelectedGraphGlyphs(selected_syms);
	}

	private void resetSelectedGraphGlyphs(List<?> selected_syms) {
		int symcount = selected_syms.size();
		is_listening = false; // turn off propagation of events from the GUI while we modify the settings
		collectGraphsAndGlyphs(selected_syms, symcount);

		boolean all_are_floating = false;
		boolean all_show_axis = false;
		boolean all_show_label = false;
		boolean any_are_combined = false; // are any selections inside a combined tier
		boolean all_are_combined = false; // are all selections inside (a) combined tier(s)

		// Take the first glyph in the list as a prototype
		AbstractGraphGlyph first_glyph = null;
		HeatMap hm = null;
		if (!glyphs.isEmpty()) {
			first_glyph = glyphs.get(0);
			all_are_floating = first_glyph.getGraphState().getTierStyle().getFloatGraph();
			all_show_axis = first_glyph.getGraphState().getShowAxis();
			all_show_label = first_glyph.getGraphState().getShowLabel();
			boolean this_one_is_combined = (first_glyph.getGraphState().getComboStyle() != null);
			any_are_combined = this_one_is_combined;
			all_are_combined = this_one_is_combined;
		}

		// Now loop through other glyphs if there are more than one
		// and see if the graph_style and heatmap are the same in all selections
		for (AbstractGraphGlyph gl : glyphs) {
			all_are_floating = all_are_floating && gl.getGraphState().getTierStyle().getFloatGraph();
			all_show_axis = all_show_axis && gl.getGraphState().getShowAxis();
			all_show_label = all_show_label && gl.getGraphState().getShowLabel();
			boolean this_one_is_combined = (gl.getGraphState().getComboStyle() != null);
			any_are_combined = any_are_combined || this_one_is_combined;
			all_are_combined = all_are_combined && this_one_is_combined;
		}

		boolean b = !(grafs.isEmpty());
		threshB.setEnabled(b);
		boolean type = b;
		for (GraphSym graf : grafs) {
			type = !(graf instanceof MisMatchGraphSym);
			if (type) {
				break;
			}
		}
		advanced_panel.setPanelEnabled();
		combineB.setEnabled(!all_are_combined && grafs.size() > 1);
		splitB.setEnabled(any_are_combined);
		is_listening = true; // turn back on GUI events
	}

	private void collectGraphsAndGlyphs(List<?> selected_syms, int symcount) {
		if (grafs != selected_syms) {
			// in certain cases selected_syms arg and grafs list may be same, for example when method is being
			//     called to catch changes in glyphs representing selected sym, not the syms themselves)
			//     therefore don't want to change grafs list if same as selected_syms (especially don't want to clear it!)
			grafs.clear();
		}
		glyphs.clear();
		// First loop through and collect graphs and glyphs
		for (int i = 0; i < symcount; i++) {
			if (selected_syms.get(i) instanceof GraphSym) {
				GraphSym graf = (GraphSym) selected_syms.get(i);
				// only add to grafs if list is not identical to selected_syms arg
				if (grafs != selected_syms) {
					grafs.add(graf);
				}
				// add all graph glyphs representing graph sym
				//	  System.out.println("found multiple glyphs for graph sym: " + multigl.size());
				for (Glyph g : igbService.getVisibleTierGlyphs()) {
					ViewModeGlyph vg = ((TierGlyph) g).getViewModeGlyph();
					if (vg instanceof MultiGraphGlyph) {
						for (GlyphI child : vg.getChildren()) {
							if (grafs.contains(child.getInfo())) {
								glyphs.add((AbstractGraphGlyph) child);
							}
						}
					} else if (grafs.contains(vg.getInfo())) {
						glyphs.add((AbstractGraphGlyph) vg);
					}
				}
			}
		}
	}

	public void seqSelectionChanged(SeqSelectionEvent evt) {
		if (DEBUG_EVENTS) {
			System.out.println("SeqSelectionEvent, selected seq: " + evt.getSelectedSeq() + " received by " + this.getClass().getName());
		}
		current_seq = evt.getSelectedSeq();
		resetSelectedGraphGlyphs(gmodel.getSelectedSymmetries(current_seq));
	}

	public AdvancedGraphPanel getAdvancedPanel() {
		return advanced_panel;
	}

	private void updateViewer() {
		final List<GraphSym> previous_graph_syms = new ArrayList<GraphSym>(grafs);
		// set selections to empty so that options get turned off
		resetSelectedGraphGlyphs(Collections.EMPTY_LIST);
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				igbService.getSeqMapView().setAnnotatedSeq(gmodel.getSelectedSeq(), true, true);
				resetSelectedGraphGlyphs(previous_graph_syms);
			}
		});
	}

	public void trackstylePropertyChanged(EventObject eo) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	final class AdvancedGraphPanel {

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

				if (grafs.size() >= operator.getOperandCountMin(FileTypeCategory.Graph)
						&& grafs.size() <= operator.getOperandCountMax(FileTypeCategory.Graph)) {
					setGraphName(comp, operator);
				} else {
					comp.setToolTipText(GeneralUtils.getOperandMessage(grafs.size(), operator.getOperandCountMin(FileTypeCategory.Graph), operator.getOperandCountMax(FileTypeCategory.Graph), "graph"));
				}
			}

			public void mouseExited(MouseEvent e) {
				JRPComboBoxWithSingleListener comp = (JRPComboBoxWithSingleListener) e.getComponent();
				String selection = (String) comp.getSelectedItem();
				unsetGraphName(name2operator.get(selection));
			}

			public void setGraphName(JRPComboBoxWithSingleListener comp, Operator operator) {
				if (operator.getOperandCountMin(FileTypeCategory.Graph) == 2 && operator.getOperandCountMax(FileTypeCategory.Graph) == 2) {
					A = grafs.get(0).getGraphName();
					B = grafs.get(1).getGraphName();

					grafs.get(0).setGraphName("A");
					grafs.get(1).setGraphName("B");

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
					if (A != null && B != null && grafs.size() > 1) {
						grafs.get(0).setGraphName(A);
						grafs.get(1).setGraphName(B);

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
		private final Map<String, Operator> name2transform;
		private final Map<String, Operator> name2operator;
		public final JRPButton transformationGoB = new JRPButton("SimpleGraphTab_transformationGoB", BUNDLE.getString("goButton"));
		public final JLabel transformation_label = new JLabel(BUNDLE.getString("transformationLabel"));
		public final JRPComboBoxWithSingleListener transformationCB = new JRPComboBoxWithSingleListener("SimpleGraphTab_transformation");
		public final JLabel operation_label = new JLabel(BUNDLE.getString("operationLabel"));
		public final JRPComboBoxWithSingleListener operationCB = new JRPComboBoxWithSingleListener("SimpleGraphTab_operation");
		public final JRPButton operationGoB = new JRPButton("SimpleGraphTab_operationGoB", BUNDLE.getString("goButton"));
		private final HoverEffect hovereffect;
		private final ItemListener operationListener = new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				setPanelEnabled();
			}
		};

		public AdvancedGraphPanel() {
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
		}

		private void loadOperators() {
			transformationCB.removeAllItems();
			name2transform.clear();
			operationCB.removeAllItems();
			name2operator.clear();
			TreeSet<Operator> operators = new TreeSet<Operator>(
					new Comparator<Operator>() {

						@Override
						public int compare(Operator o1, Operator o2) {
							if (o1 instanceof Order && o2 instanceof Order) {
								if (((Order) o1).getOrder() == ((Order) o2).getOrder()) {
									return 0;
								} else if (((Order) o1).getOrder() > ((Order) o2).getOrder()) {
									return 1;
								}

								return -1;
							}

							if (o1 instanceof Order && !(o2 instanceof Order)) {
								return -1;
							}

							if (!(o1 instanceof Order) && o2 instanceof Order) {
								return 1;
							}

							return o1.getDisplay().compareTo(o2.getDisplay());
						}
					});
			operators.addAll(ExtensionPointHandler.getExtensionPoint(Operator.class).getExtensionPointImpls());
			for (Operator operator : operators) {
				if (AbstractGraphOperator.isGraphOperator(operator)) {
					name2operator.put(operator.getDisplay(), operator);
					operationCB.addItem(operator.getDisplay());
				}
				if (AbstractFloatTransformer.isGraphTransform(operator)) {
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
			int gcount = grafs.size();
			float height = 0;

			// Note that the combo_style does not implement IFloatableTierStyle
			// because the glyph factory doesn't support floating combo graphs anyway.
			ITrackStyleExtended combo_style = null;
			String viewMode = "combo";

			Map<Color, Integer> colorMap = new HashMap<Color, Integer>();
			// If any of them already has a combo style, use that one
			for (int i = 0; i < gcount && combo_style == null; i++) {
				GraphSym gsym = grafs.get(i);
				combo_style = gsym.getGraphState().getComboStyle();
				Color col = gsym.getGraphState().getTierStyle().getBackground();
				int c = 0;
				if (colorMap.containsKey(col)) {
					c = colorMap.get(col) + 1;
				}
				colorMap.put(col, c);
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
			for (GraphSym gsym : grafs) {
				GraphState gstate = gsym.getGraphState();
				gstate.setComboStyle(combo_style, i++);
				gstate.getTierStyle().setFloatGraph(false); // ignored since combo_style is set
				height += gsym.getGraphState().getTierStyle().getHeight();
			}
			combo_style.setHeight(height / i);

			updateViewer();
		}

		/**
		 *  Puts all selected graphs in separate tiers by setting the
		 *  combo state of each graph's state to null.
		 */
		private void splitGraphs() {
			if (grafs.isEmpty()) {
				return;
			}

			for (GraphSym gsym : grafs) {
				GraphState gstate = gsym.getGraphState();
				gstate.setComboStyle(null, 0);

				// For simplicity, set the floating state of all new tiers to false.
				// Otherwise, have to calculate valid, non-overlapping y-positions and heights.
				gstate.getTierStyle().setFloatGraph(false); // for simplicity
			}
			updateViewer();
		}

		private void floatGraphs(boolean do_float) {
			boolean something_changed = false;
			for (AbstractGraphGlyph gl : glyphs) {
				GraphState gstate = gl.getGraphState();
//				if (gstate.getComboStyle() != null) {
//					gstate.setComboStyle(null);
//					something_changed = true;
//				}
				boolean is_floating = gstate.getTierStyle().getFloatGraph();
				if (do_float && (!is_floating)) {
					//GraphGlyphUtils.floatGraph(gl, gviewer);

					// figure out correct height
					Rectangle2D.Double coordbox = gl.getCoordBox();
					Rectangle pixbox = new Rectangle();
					igbService.getView().transformToPixels(coordbox, pixbox);
					gstate.getTierStyle().setY(pixbox.y);
					gstate.getTierStyle().setHeight(pixbox.height);

					gstate.getTierStyle().setFloatGraph(true);
					something_changed = true;
				} else if ((!do_float) && is_floating) {
					//GraphGlyphUtils.attachGraph(gl, gviewer);

					// figure out correct height
					Rectangle2D.Double tempbox = gl.getCoordBox();  // pixels, since in PixelFloaterGlyph 1:1 mapping of pixel:coord
					Rectangle pixbox = new Rectangle((int) tempbox.x, (int) tempbox.y, (int) tempbox.width, (int) tempbox.height);
					Rectangle2D.Double coordbox = new Rectangle2D.Double();
					igbService.getView().transformToCoords(pixbox, coordbox);
					gstate.getTierStyle().setY(coordbox.y); // currently y has no effect on attached graphs, but will someday
					gstate.getTierStyle().setHeight(coordbox.height);

					gstate.getTierStyle().setFloatGraph(false);
					something_changed = true;
				}
			}
			if (something_changed) {
				updateViewer();
			}
		}

		public void setPanelEnabled() {
			transformationGoB.setEnabled(!(grafs.isEmpty()));
			transformationCB.setEnabled(transformationGoB.isEnabled());
			paramT.setEnabled(transformationGoB.isEnabled());
			operationCB.setEnabled(grafs.size() >= 2);
			String selection = (String) operationCB.getSelectedItem();
			Operator operator = name2operator.get(selection);
			boolean canGraph = (operator != null
					&& grafs.size() >= operator.getOperandCountMin(FileTypeCategory.Graph)
					&& grafs.size() <= operator.getOperandCountMax(FileTypeCategory.Graph));
			operationGoB.setEnabled(operationCB.isEnabled() && canGraph);
			if (canGraph || operator == null) {
				operationGoB.setToolTipText(null);
			} else {
				operationGoB.setToolTipText(GeneralUtils.getOperandMessage(grafs.size(), operator.getOperandCountMin(FileTypeCategory.Graph), operator.getOperandCountMax(FileTypeCategory.Graph), "graph"));
			}
		}
	}
}
