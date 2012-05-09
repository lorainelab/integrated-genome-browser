/**
 * Copyright (c) 2006 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.trackAdjuster;

import com.affymetrix.igb.shared.SelectAllAction;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.*;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.MisMatchGraphSym;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.swing.recordplayback.*;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public final class TrackAdjusterTab
		implements SeqSelectionListener, SymSelectionListener, TrackstylePropertyMonitor.TrackStylePropertyListener, ListSelectionListener {

	//System.out.println() statements do not show on the screen, they are not translated.
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("trackAdjuster");
	private static TrackAdjusterTab singleton;
	private static final Map<GraphType, String> graphType2ViewMode = new EnumMap<GraphType, String>(GraphType.class);
	private static final String viewModePrefix = "viewmode_";
	private final static int xoffset_pop = 0;
	private final static int yoffset_pop = 30;
	private final static JPopupMenu popup = new JPopupMenu();

	static {
		graphType2ViewMode.put(GraphType.BAR_GRAPH, "bargraph");
		graphType2ViewMode.put(GraphType.DOT_GRAPH, "dotgraph");
		graphType2ViewMode.put(GraphType.FILL_BAR_GRAPH, "fillbargraph");
		graphType2ViewMode.put(GraphType.HEAT_MAP, "heatmapgraph");
		graphType2ViewMode.put(GraphType.LINE_GRAPH, "linegraph");
		graphType2ViewMode.put(GraphType.MINMAXAVG, "minmaxavggraph");
		graphType2ViewMode.put(GraphType.STAIRSTEP_GRAPH, "stairstepgraph");
	}

	enum DisplayType {

		ANNOTATION,
		GRAPH,
		AUTO,
		SEQUENCE,
		PLUGIN,
		NONE;
	}

	enum ViewMode {

		//Graphs
		viewmode_mismatch(viewModePrefix + "mismatch"),
		viewmode_bargraph(viewModePrefix + "bargraph"),
		viewmode_fillbargraph(viewModePrefix + "fillbargraph"),
		viewmode_heatmapgraph(viewModePrefix + "heatmapgraph"),
		viewmode_linegraph(viewModePrefix + "linegraph"),
		viewmode_minmaxavggraph(viewModePrefix + "minmaxavggraph"),
		viewmode_stairstepgraph(viewModePrefix + "stairstepgraph"),
		viewmode_dotgraph(viewModePrefix + "dotgraph"),
		viewmode_scored_bargraph(viewModePrefix + "scored_bargraph"),
		viewmode_scored_dotgraph(viewModePrefix + "scored_dotgraph"),
		viewmode_scored_fillbargraph(viewModePrefix + "scored_fillbargraph"),
		viewmode_scored_heatmapgraph(viewModePrefix + "scored_heatmapgraph"),
		viewmode_scored_linegraph(viewModePrefix + "scored_linegraph"),
		viewmode_scored_minmaxavggraph(viewModePrefix + "scored_minmaxavggraph"),
		viewmode_scored_stairstepgraph(viewModePrefix + "scored_stairstepgraph"),
		//Annotations
		viewmode_annotation(viewModePrefix + "annotation"),
		viewmode_alignment(viewModePrefix + "alignment"),
		viewmode_probeset(viewModePrefix + "probeset"),
		viewmode_sequence(viewModePrefix + "sequence"),
		//Auto 
		viewmode_semantic_zoom_annotation(viewModePrefix + "semantic_zoom_annotation"),
		viewmode_semantic_zoom_alignment(viewModePrefix + "semantic_zoom_alignment"),
		viewmode_bai_semantic_zoom(viewModePrefix + "bai_semantic_zoom"),
		//None
		viewmode_unloaded("unloaded");
		String stringValue;
		final static Map<String, ViewMode> string2ViewMode;

		static {
			string2ViewMode = new HashMap<String, ViewMode>();
			for (ViewMode type : values()) {
				string2ViewMode.put(type.stringValue, type);
			}
		}

		ViewMode(String stringValue) {
			this.stringValue = stringValue;
		}

		static ViewMode getViewMode(String stringValue) {
			ViewMode nr = string2ViewMode.get(stringValue);
			if (nr != null) {
				return nr;
			}
			return null;
		}

		@Override
		public String toString() {
			return stringValue;
		}
	}
	private static final Map<ViewMode, DisplayType> viewMode2DisplayType = new EnumMap<ViewMode, DisplayType>(ViewMode.class);

	static {
		//Graphs
		viewMode2DisplayType.put(ViewMode.viewmode_mismatch, DisplayType.GRAPH);
		viewMode2DisplayType.put(ViewMode.viewmode_scored_bargraph, DisplayType.GRAPH);
		viewMode2DisplayType.put(ViewMode.viewmode_scored_dotgraph, DisplayType.GRAPH);
		viewMode2DisplayType.put(ViewMode.viewmode_scored_fillbargraph, DisplayType.GRAPH);
		viewMode2DisplayType.put(ViewMode.viewmode_scored_heatmapgraph, DisplayType.GRAPH);
		viewMode2DisplayType.put(ViewMode.viewmode_scored_linegraph, DisplayType.GRAPH);
		viewMode2DisplayType.put(ViewMode.viewmode_scored_minmaxavggraph, DisplayType.GRAPH);
		viewMode2DisplayType.put(ViewMode.viewmode_scored_stairstepgraph, DisplayType.GRAPH);
		viewMode2DisplayType.put(ViewMode.viewmode_bargraph, DisplayType.GRAPH);
		viewMode2DisplayType.put(ViewMode.viewmode_fillbargraph, DisplayType.GRAPH);
		viewMode2DisplayType.put(ViewMode.viewmode_heatmapgraph, DisplayType.GRAPH);
		viewMode2DisplayType.put(ViewMode.viewmode_linegraph, DisplayType.GRAPH);
		viewMode2DisplayType.put(ViewMode.viewmode_minmaxavggraph, DisplayType.GRAPH);
		viewMode2DisplayType.put(ViewMode.viewmode_stairstepgraph, DisplayType.GRAPH);
		viewMode2DisplayType.put(ViewMode.viewmode_dotgraph, DisplayType.GRAPH);
		//Annotations
		viewMode2DisplayType.put(ViewMode.viewmode_annotation, DisplayType.ANNOTATION);
		viewMode2DisplayType.put(ViewMode.viewmode_probeset, DisplayType.ANNOTATION);
		viewMode2DisplayType.put(ViewMode.viewmode_alignment, DisplayType.ANNOTATION);
		viewMode2DisplayType.put(ViewMode.viewmode_sequence, DisplayType.ANNOTATION);
		//Auto
		viewMode2DisplayType.put(ViewMode.viewmode_semantic_zoom_annotation, DisplayType.AUTO);
		viewMode2DisplayType.put(ViewMode.viewmode_semantic_zoom_alignment, DisplayType.AUTO);
		viewMode2DisplayType.put(ViewMode.viewmode_bai_semantic_zoom, DisplayType.AUTO);
		//NONE
		viewMode2DisplayType.put(ViewMode.viewmode_unloaded, DisplayType.NONE);
	}
	BioSeq current_seq;
	GenometryModel gmodel;
	public boolean is_listening = true; // used to turn on and off listening to GUI events
	public GraphVisibleBoundsSetter vis_bounds_setter;
	boolean DEBUG_EVENTS = false;
	public JLabel selected_graphs_label = new JLabel(BUNDLE.getString("selectedGraphsLabel"));
	public JRPRadioButton mmavgB = new JRPRadioButton("SimpleGraphTab_mmavgB", BUNDLE.getString("minMaxAvgButton"));
	public JRPRadioButton lineB = new JRPRadioButton("SimpleGraphTab_lineB", BUNDLE.getString("lineButton"));
	public JRPRadioButton barB = new JRPRadioButton("SimpleGraphTab_barB", BUNDLE.getString("barButton"));
	public JRPRadioButton dotB = new JRPRadioButton("SimpleGraphTab_dotB", BUNDLE.getString("dotButton"));
	public JRPRadioButton sstepB = new JRPRadioButton("SimpleGraphTab_sstepB", BUNDLE.getString("stairStepButton"));
	public JRPRadioButton hmapB = new JRPRadioButton("SimpleGraphTab_hmapB", BUNDLE.getString("heatMapButton"));
	public JRPRadioButton hidden_styleB = new JRPRadioButton("SimpleGraphTab_hidden_styleB", BUNDLE.getString("hiddenStyleButton")); // this button will not be displayed
	public JRPRadioButton annotationB = new JRPRadioButton("SimpleGraphTab_annotationB", BUNDLE.getString("AnnotationButton"));
	public JRPRadioButton graphB = new JRPRadioButton("SimpleGraphTab_graphB", BUNDLE.getString("graphButton"));
	public JRPRadioButton autoB = new JRPRadioButton("SimpleGraphTab_autoB", BUNDLE.getString("AutoButton"));
	public JRPRadioButton pluginB = new JRPRadioButton(BUNDLE.getString("PluginButton"));
	public JRPRadioButton graphPluginB = new JRPRadioButton(BUNDLE.getString("GraphPluginButton"));
	public ButtonGroup stylegroup = new ButtonGroup();
	public ButtonGroup displayGroup = new ButtonGroup();
	public JPanel rangePanel = new javax.swing.JPanel();
	public JPanel graphPanel = new javax.swing.JPanel();
	public com.jidesoft.combobox.ColorComboBox fgColorComboBox = new com.jidesoft.combobox.ColorComboBox();
	public com.jidesoft.combobox.ColorComboBox bgColorComboBox = new com.jidesoft.combobox.ColorComboBox();
	public JRPTextField maxStackDepthTextField = new JRPNumTextField("SimpleGraphTab_max_depth_text_field");
	public JRPTextField trackName = new JRPTextField("SimpleGraphTab_track_name");
	public static final Object[] SUPPORTED_SIZE = {8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
	public JRPComboBox trackNameSizeComboBox = new JRPComboBox("SimpleGraphTab_track_name_size");
	public JRPSlider height_slider = new JRPSlider("SimpleGraphTab_height_slider", JSlider.HORIZONTAL, 10, 500, 50);
	public final List<GraphSym> grafs = new ArrayList<GraphSym>();
	public final List<TierGlyph> selectedTiers = new ArrayList<TierGlyph>();
	public final List<AbstractGraphGlyph> glyphs = new ArrayList<AbstractGraphGlyph>();
	public final JRPCheckBox labelCB = new JRPCheckBox("SimpleGraphTab_hidden_labelCB", BUNDLE.getString("labelCheckBox"));
	public final JRPCheckBox yaxisCB = new JRPCheckBox("SimpleGraphTab_hidden_yaxisCB", BUNDLE.getString("yAxisCheckBox"));
	public final JRPCheckBox floatCB = new JRPCheckBox("SimpleGraphTab_hidden_floatCB", BUNDLE.getString("floatingCheckBox"));
	private IGBService igbService;
	public final JRPTextField paramT = new JRPTextField("SimpleGraphTab_paramT", "", 2);
	public final JRPButton combineB = new JRPButton("SimpleGraphTab_combineB", BUNDLE.getString("combineButton"));
	public final JRPButton splitB = new JRPButton("SimpleGraphTab_splitB", BUNDLE.getString("splitButton"));
	public JRPComboBox heat_mapCB;
	public JPanel annotationPanel = new JPanel();

	public static void init(IGBService igbService) {
		singleton = new TrackAdjusterTab(igbService);
	}

	public static synchronized TrackAdjusterTab getSingleton() {
		return singleton;
	}

	public TrackAdjusterTab(IGBService igbS) {
		igbService = igbS;

		heat_mapCB = new JRPComboBox("SimpleGraphTab_heat_mapCB", HeatMap.getStandardNames());
		trackNameSizeComboBox.setModel(new DefaultComboBoxModel(SUPPORTED_SIZE));
		heat_mapCB.addItemListener(new HeatMapItemListener());

		barB.addActionListener(new GraphStyleSetter(GraphType.BAR_GRAPH));
		dotB.addActionListener(new GraphStyleSetter(GraphType.DOT_GRAPH));
		//hmapB.addActionListener(new GraphStyleSetter(GraphType.HEAT_MAP));
		graphPluginB.addActionListener(new GraphStyleSetter(GraphType.HEAT_MAP)); //will need to be re-written to function for plugins
		lineB.addActionListener(new GraphStyleSetter(GraphType.LINE_GRAPH));
		mmavgB.addActionListener(new GraphStyleSetter(GraphType.MINMAXAVG));
		sstepB.addActionListener(new GraphStyleSetter(GraphType.STAIRSTEP_GRAPH));

		stylegroup.add(barB);
		stylegroup.add(dotB);
		stylegroup.add(graphPluginB); //temp replacement for heatmap button
		stylegroup.add(lineB);
		stylegroup.add(mmavgB);
		stylegroup.add(sstepB);
		stylegroup.add(hidden_styleB); // invisible button

		displayGroup.add(annotationB);
		displayGroup.add(graphB);
		displayGroup.add(autoB);
		displayGroup.add(pluginB);

		hidden_styleB.setSelected(true); // deselect all visible radio buttons

		vis_bounds_setter = new GraphVisibleBoundsSetter(igbService.getSeqMap());


		resetSelectedGraphGlyphs(Collections.EMPTY_LIST);

		gmodel = GenometryModel.getGenometryModel();
		gmodel.addSeqSelectionListener(this);
		gmodel.addSymSelectionListener(this);
		TrackstylePropertyMonitor.getPropertyTracker().addPropertyListener(this);
		igbService.addListSelectionListener(this);

		for (FileTypeCategory category : FileTypeCategory.values()) {
			JRPMenuItem item = new JRPMenuItem("Track_Adjuster_Select_Menu_"
					+ category.name(), SelectAllAction.getAction(category));
			popup.add(item);
		}
	}

	public void selectAllBMouseClicked(JComponent c, MouseEvent e) {
		if (c.getWidth() * 0.75 - e.getX() < 0) {
			if (popup.getComponentCount() > 0) {
				popup.show(c, xoffset_pop, yoffset_pop);
			}
		} else {
			SelectAllAction.getAction().execute();
		}
	}

	public boolean isTierGlyph(GlyphI glyph) {
		return glyph instanceof TierGlyph;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getSource() instanceof TierGlyph) {
			if (((TierGlyph) e.getSource()).isSelected()) {
				System.out.println("add me");
			} else {
				System.out.println("Remove Me");
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void symSelectionChanged(SymSelectionEvent evt) {
		List<SeqSymmetry> selected_syms = evt.getSelectedSyms();
		// Only pay attention to selections from the main SeqMapView or its map.
		// Ignore the splice view as well as events coming from this class itself.

		Object src = evt.getSource();
		if (!(src == igbService.getSeqMapView() || src == igbService.getSeqMap())
				|| igbService.getSeqMap() == null || igbService.getSeqMapView() == null) {
			return;
		}

		List<TierGlyph> tierList = (List<TierGlyph>) igbService.getSeqMapView().getSelectedTiers();
		resetSelectedGraphGlyphs(selected_syms);
		refreshSelection(tierList);
	}

	private void refreshSelection(List<TierGlyph> tierList) {
		is_listening = false; // turn off propagation of events from the GUI while we modify the settings
		if (selectedTiers != tierList) {
			selectedTiers.clear();
			for (TierGlyph tier : tierList) {
				if (!selectedTiers.contains(tier)) {
					selectedTiers.add(tier);
				}
			}
		}
		int selectedTrackCount = selectedTiers.size();
		if (selectedTrackCount == 1) {
			ITrackStyleExtended style = selectedTiers.get(0).getAnnotStyle();
			if (style == null) {
				disableDisplayButtons(true, true);
				return;
			}
			fgColorComboBox.setSelectedColor(style.getForeground());
			bgColorComboBox.setSelectedColor(style.getBackground());
			trackName.setText(style.getTrackName());
			maxStackDepthTextField.setText(Integer.toString(style.getMaxDepth()));
			trackNameSizeComboBox.setSelectedItem((int) style.getTrackNameSize());
			MapViewGlyphFactoryI mode = MapViewModeHolder.getInstance().
					getViewFactory(style.getViewMode());
			String viewModeName = viewModePrefix + mode.getName();
			ViewMode viewMode = ViewMode.string2ViewMode.get(viewModeName);
			DisplayType type = viewMode2DisplayType.get(viewMode);
			if (type == null) {
				//Should fix a reported bug, but could not reproduce to test.
				disableDisplayButtons(true, true);
				return;
			}
			setDisplayTypeButton(type);
			setEnabledDisplayButtonsBySelection();

		} else if (selectedTrackCount > 1) {
			fgColorComboBox.setSelectedColor(null);
			bgColorComboBox.setSelectedColor(null);
			trackNameSizeComboBox.setSelectedItem(null);
			trackName.setText("");
			maxStackDepthTextField.setText("");
			disableDisplayButtons(true, true);
			setEnabledDisplayButtonsBySelection();
		}

		boolean b = !(selectedTiers.isEmpty());
		floatCB.setEnabled(b);
		fgColorComboBox.setEnabled(b);
		bgColorComboBox.setEnabled(b);
		trackName.setEnabled(b);
		maxStackDepthTextField.setEnabled(b);
		trackNameSizeComboBox.setEnabled(b);
		is_listening = true; // turn back on GUI events
	}

	private void setDisplayTypeButton(DisplayType type) {
		if (type == null) {
			return;
		}
		switch (type) {
			case ANNOTATION:
				annotationB.setSelected(true);
				break;
			case GRAPH:
				graphB.setSelected(true);
				break;
			case AUTO:
				autoB.setSelected(true);
				break;
			case PLUGIN:
				pluginB.setSelected(true);
				break;
			case NONE:
				disableDisplayButtons(true, true);
				break;
			default:
				//Should never happen
				disableDisplayButtons(true, true);
				break;
		}
	}

	private void setEnabledDisplayButtonsBySelection() {
		disableDisplayButtons(false, true);

		for (TierGlyph tier : selectedTiers) {
			final RootSeqSymmetry rootSym = (RootSeqSymmetry) tier.getInfo();
			final ITrackStyleExtended style = tier.getAnnotStyle();
			if (style != null && MapViewModeHolder.getInstance() != null && rootSym != null) {
				for (final MapViewGlyphFactoryI mode : MapViewModeHolder.getInstance().getAllViewModesFor(rootSym.getCategory(), style.getMethodName())) {
					String viewModeName = "viewmode_" + mode.getName();
					ViewMode viewMode = ViewMode.string2ViewMode.get(viewModeName);
					switch (viewMode2DisplayType.get(viewMode)) {
						case ANNOTATION:
							annotationB.setEnabled(true);
							break;
						case GRAPH:
							graphB.setEnabled(true);
							//TO DO: Enable graph panel and sliders
							//Reselection is currently required, but this is not acceptable for the future
							break;
						case AUTO:
							autoB.setEnabled(true);
							break;
						case PLUGIN:
							pluginB.setEnabled(true);
							break;
						case NONE:
							disableDisplayButtons(true, true);
							break;
						default:
							//Should never happen
							disableDisplayButtons(true, true);
							break;
					}
				}
			}
		}
	}

	private void disableDisplayButtons(boolean select, boolean enable) {
		if (select) {
			annotationB.setSelected(false);
			graphB.setSelected(false);
			autoB.setSelected(false);
			pluginB.setSelected(false);
		}
		if (enable) {
			annotationB.setEnabled(false);
			graphB.setEnabled(false);
			autoB.setEnabled(false);
			pluginB.setEnabled(false);
		}
	}

	private void resetSelectedGraphGlyphs(List<?> selected_syms) {
		int symcount = selected_syms.size();
		is_listening = false; // turn off propagation of events from the GUI while we modify the settings
		collectGraphsAndGlyphs(selected_syms, symcount);

		int num_glyphs = glyphs.size();
		//    System.out.println("number of selected graphs: " + num_glyphs);
		double the_height = -1; // -1 indicates unknown height

		boolean all_are_floating = false;
		boolean all_show_axis = false;
		boolean all_show_label = false;
		boolean any_are_combined = false; // are any selections inside a combined tier
		boolean all_are_combined = false; // are all selections inside (a) combined tier(s)

		// Take the first glyph in the list as a prototype
		AbstractGraphGlyph first_glyph = null;
		GraphType graph_style = GraphType.LINE_GRAPH;
		HeatMap hm = null;
		if (!glyphs.isEmpty()) {
			first_glyph = glyphs.get(0);
			graph_style = first_glyph.getGraphStyle();
			if (graph_style == GraphType.HEAT_MAP) {
				hm = first_glyph.getHeatMap();
			}
			the_height = first_glyph.getGraphState().getTierStyle().getHeight();
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

			if (graph_style == null) {
				graph_style = GraphType.LINE_GRAPH;
			} else if (first_glyph.getGraphStyle() != gl.getGraphStyle()) {
				graph_style = GraphType.LINE_GRAPH;
			}
			if (graph_style == GraphType.HEAT_MAP) {
				if (first_glyph.getHeatMap() != gl.getHeatMap()) {
					hm = null;
				}
			} else {
				hm = null;
			}
		}

		setColorCombobox();
		selectButtonBasedOnGraphStyle(graph_style);

		if (graph_style == GraphType.HEAT_MAP) {
			heat_mapCB.setEnabled(true);
			graphPluginB.setSelected(true);
			if (hm == null) {
				heat_mapCB.setSelectedIndex(-1);
			} else {
				heat_mapCB.setSelectedItem(hm.getName());
			}
		} else {
			heat_mapCB.setEnabled(false);
			graphPluginB.setSelected(false);
		}

		if (the_height != -1) {
			height_slider.setValue((int) the_height);
		}
		vis_bounds_setter.setGraphs(glyphs);

		if (!glyphs.isEmpty()) {
			floatCB.setSelected(all_are_floating);
			yaxisCB.setSelected(all_show_axis);
			labelCB.setSelected(all_show_label);
		}

		boolean b = !(grafs.isEmpty());
		height_slider.setEnabled(b);
		boolean type = b;
		for (GraphSym graf : grafs) {
			type = !(graf instanceof MisMatchGraphSym);
			if (type) {
				break;
			}
		}

		enableButtons(stylegroup, type);
		for (Component c : annotationPanel.getComponents()) {
			if (c instanceof JRPTextField) {
				JRPTextField textField = (JRPTextField) c;
				textField.setText("");
				textField.setEditable(!type);
			} else {
				c.setEnabled(!type);
			}
		}

		yaxisCB.setEnabled(b);
		labelCB.setEnabled(b);

		fgColorComboBox.setEnabled(b);
		bgColorComboBox.setEnabled(b);
		trackName.setEnabled(b);

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

	private void selectButtonBasedOnGraphStyle(GraphType graph_style) {
		switch (graph_style) {
			case MINMAXAVG:
				mmavgB.setSelected(true);
				break;
			case LINE_GRAPH:
				lineB.setSelected(true);
				break;
			case BAR_GRAPH:
				barB.setSelected(true);
				break;
			case DOT_GRAPH:
				dotB.setSelected(true);
				break;
			case HEAT_MAP:
				//hmapB.setSelected(true);
				graphPluginB.setSelected(true);
				break;
			case STAIRSTEP_GRAPH:
				sstepB.setSelected(true);
				break;
			default:
				hidden_styleB.setSelected(true);
				break;
		}
	}

	public void seqSelectionChanged(SeqSelectionEvent evt) {
		if (DEBUG_EVENTS) {
			System.out.println("SeqSelectionEvent, selected seq: " + evt.getSelectedSeq() + " received by " + this.getClass().getName());
		}
		current_seq = evt.getSelectedSeq();
		resetSelectedGraphGlyphs(gmodel.getSelectedSymmetries(current_seq));
	}

	private static void enableButtons(ButtonGroup g, boolean b) {
		Enumeration<AbstractButton> e = g.getElements();
		while (e.hasMoreElements()) {
			e.nextElement().setEnabled(b);
		}
	}

	private void setColorCombobox() {
		if (igbService.getSeqMap() == null) {
			return;
		}
		int num_glyphs = glyphs.size();

		if (num_glyphs == 0) {
			// Do Nothing
		} else if (num_glyphs == 1) {
			GlyphI g = igbService.getSeqMap().getItem(grafs.get(0));
			AbstractGraphGlyph gl = (AbstractGraphGlyph) g;
			Color color = gl.getGraphState().getTierStyle().getBackground();
			bgColorComboBox.setSelectedColor(color);
			color = gl.getGraphState().getTierStyle().getForeground();
			fgColorComboBox.setSelectedColor(color);
			trackName.setText(gl.getGraphState().getTierStyle().getTrackName());
		} else {
			fgColorComboBox.setSelectedColor(null);
			bgColorComboBox.setSelectedColor(null);
			trackName.setText("");
		}
	}

	private final class GraphStyleSetter extends GenericAction implements ActionListener {

		private static final long serialVersionUID = 1L;
		GraphType graphType = GraphType.LINE_GRAPH;

		public GraphStyleSetter(GraphType graphType) {
			super(null, null, null);
			this.graphType = graphType;
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			super.actionPerformed(event);
			if (DEBUG_EVENTS) {
				System.out.println(this.getClass().getName() + " got an ActionEvent: " + event);
			}
			if (glyphs.isEmpty() || !is_listening) {
				return;
			}

			Runnable r = new Runnable() {

				public void run() {
					String viewMode = graphType2ViewMode.get(graphType);
					HeatMap hm = (glyphs.get(0)).getHeatMap();
					for (AbstractGraphGlyph sggl : new ArrayList<AbstractGraphGlyph>(glyphs)) {
						sggl.setShowGraph(true);
						igbService.changeViewMode(igbService.getSeqMapView(), sggl.getAnnotStyle(), viewMode, (RootSeqSymmetry) sggl.getInfo(), sggl.getGraphState().getComboStyle());
						if ((graphType == GraphType.HEAT_MAP) && (hm != sggl.getHeatMap())) {
							hm = null;
						}
					}
					if (graphType == GraphType.HEAT_MAP) {
						heat_mapCB.setEnabled(true);
						graphPluginB.setSelected(true);
						if (hm == null) {
							heat_mapCB.setSelectedIndex(-1);
						} else {
							heat_mapCB.setSelectedItem(hm.getName());
						}
					} else {
						heat_mapCB.setEnabled(false);
						graphPluginB.setSelected(false);
						// don't bother to change the displayed heat map name
					}
				}
			};

			SwingUtilities.invokeLater(r);
		}
	}

	private final class HeatMapItemListener implements ItemListener {

		public void itemStateChanged(ItemEvent e) {
			if (glyphs.isEmpty() || !is_listening) {
				return;
			}

			if (e.getStateChange() == ItemEvent.SELECTED) {
				String name = (String) e.getItem();
				HeatMap hm = HeatMap.getStandardHeatMap(name);

				if (hm != null) {
					for (AbstractGraphGlyph gl : glyphs) {
						if ("heatmapgraph".equals(gl.getName())) {
							gl.setShowGraph(true);
							gl.setHeatMap(hm);
						}
					}
					igbService.getSeqMap().updateWidget();
				}
			}
		}
	}

	public void setGraphHeight(double height) {
		for (AbstractGraphGlyph gl : glyphs) {
			Rectangle2D.Double cbox = gl.getCoordBox();
			gl.setCoords(cbox.x, cbox.y, cbox.width, height);

			// If a graph is joined with others in a combo tier, repack that tier.
			GlyphI parentgl = gl.getParent();
			if (isTierGlyph(parentgl)) {
				//	  System.out.println("Glyph: " + gl.getLabel() + ", packer: " + parentgl.getPacker());
				parentgl.pack(igbService.getView());
			}
		}
		igbService.packMap(false, true);
	}

	public void fgColorComboBoxActionPerformed() {
		if (igbService.getSeqMap() == null) {
			return;
		}
		Color color = fgColorComboBox.getSelectedColor();
		if (color != null) {
			for (TierGlyph tier : selectedTiers) {
				tier.getAnnotStyle().setForeground(color);
			}
		}
		igbService.getSeqMapView().setAnnotatedSeq(igbService.getSeqMapView().getAnnotatedSeq(), true, true, true);
	}

	public void bgColorComboBoxActionPerformed() {
		if (igbService.getSeqMap() == null) {
			return;
		}
		Color color = bgColorComboBox.getSelectedColor();
		if (color != null) {
			for (TierGlyph tier : selectedTiers) {
				tier.getAnnotStyle().setBackground(color);
			}
		}
		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				igbService.getSeqMap().updateWidget();
			}
		});
	}

	public void setTrackName(String name) {
		if (igbService.getSeqMapView() == null) {
			return;
		}
		if (selectedTiers != null) {
			igbService.getSeqMapView().renameTier(selectedTiers.get(0), name);
		}
	}

	public void setMaxDepth(String mdepth_string) {
		if (selectedTiers == null || mdepth_string == null) {
			return;
		}
		for (TierGlyph tier : selectedTiers) {
			int prev_max_depth = tier.getAnnotStyle().getMaxDepth();
			try {
				tier.getAnnotStyle().setMaxDepth(Integer.parseInt(mdepth_string));
			} catch (Exception ex) {
				tier.getAnnotStyle().setMaxDepth(prev_max_depth);
			}
		}
		igbService.getSeqMapView().setTierStyles();
		igbService.getSeqMapView().repackTheTiers(true, true);
	}

	public void setNameSize(String fontstring) {
		if (selectedTiers == null || fontstring == null) {
			return;
		}
		for (TierGlyph tier : selectedTiers) {
			ITrackStyleExtended style = tier.getAnnotStyle();
			float prev_font_size = style.getTrackNameSize();
			try {
				style.setTrackNameSize(Float.parseFloat(fontstring));
			} catch (Exception ex) {
				style.setTrackNameSize(prev_font_size);
			}
		}
		igbService.getSeqMapView().setTierStyles();
		igbService.getSeqMapView().repackTheTiers(true, true);
	}

	public void trackstylePropertyChanged(EventObject eo) {
		setColorCombobox();
	}

	public void setViewMode(DisplayType displayType) {
		if (selectedTiers == null) {
			return;
		}
		is_listening = false;
		for (GlyphI g : selectedTiers) {
			final TierGlyph tier = (TierGlyph) g;
			final RootSeqSymmetry rootSym = (RootSeqSymmetry) tier.getInfo();
			final ITrackStyleExtended style = tier.getAnnotStyle();
			final ITrackStyleExtended comboStyle = (tier.getViewModeGlyph() instanceof AbstractGraphGlyph)
					? ((AbstractGraphGlyph) tier.getViewModeGlyph()).getGraphState().getComboStyle() : null;
			for (final MapViewGlyphFactoryI mode : MapViewModeHolder.getInstance().getAllViewModesFor(rootSym.getCategory(), style.getMethodName())) {
				String viewModeName = viewModePrefix + mode.getName();
				ViewMode viewMode = ViewMode.string2ViewMode.get(viewModeName);
				if (viewMode2DisplayType.get(viewMode).equals(displayType)) {
					if (style.getSeparate() && !mode.supportsTwoTrack()) {
						style.setSeparate(false);
					}
					igbService.changeViewMode(igbService.getSeqMapView(), style, mode.getName(), rootSym, comboStyle);
					break;
				}
			}
		}

		igbService.getSeqMapView().setAnnotatedSeq(igbService.getSeqMapView().getAnnotatedSeq(), true, false, false);
		is_listening = true;
	}
}
