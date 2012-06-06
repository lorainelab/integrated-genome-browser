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

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.*;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.style.SimpleTrackStyle;
import com.affymetrix.genometryImpl.symmetry.MisMatchGraphSym;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.color.ColorSchemeComboBox;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;
import com.affymetrix.igb.shared.*;
import com.jidesoft.combobox.ColorComboBox;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public final class TrackAdjusterTab extends IGBTabPanel
		implements SeqSelectionListener, SymSelectionListener, TrackstylePropertyMonitor.TrackStylePropertyListener, ListSelectionListener {
	private static final long serialVersionUID = 1L;
	//System.out.println() statements do not show on the screen, they are not translated.
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("trackAdjuster");
	private static final int TAB_POSITION = 3;
	private static final String SELECT_ALL_PROMPT = "Select All";
	private static final String SELECT_ALL_SUFFIX = " tracks";
	private static final String SELECT_ALL_ITEM = "";
	private static final String SELECT_NONE_ITEM = "no";
	private static final String[] SELECT_ALL_ITEMS = new String[FileTypeCategory.values().length + 3];
	static {
		SELECT_ALL_ITEMS[0] = SELECT_ALL_PROMPT;
		SELECT_ALL_ITEMS[1] = SELECT_ALL_ITEM + SELECT_ALL_SUFFIX;
		SELECT_ALL_ITEMS[2] = SELECT_NONE_ITEM + SELECT_ALL_SUFFIX;
		for (int i = 0; i < FileTypeCategory.values().length; i++) {
			SELECT_ALL_ITEMS[i + 3] = FileTypeCategory.values()[i].name() + SELECT_ALL_SUFFIX;
		}
	}
	private static final Map<GraphType, String> graphType2ViewMode = new EnumMap<GraphType, String>(GraphType.class);

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
		PLUGIN;
	}

//	private BioSeq current_seq;
	private GenometryModel gmodel;
	public boolean is_listening = true; // used to turn on and off listening to GUI events
	public GraphVisibleBoundsSetter vis_bounds_setter;
	private boolean DEBUG_EVENTS = false;

	private JComboBox selectAllCB;
	private JRadioButton graphP_mmavgB;
	private JRadioButton graphP_lineB;
	private JRadioButton graphP_barB;
	private JRadioButton graphP_dotB;
	private JRadioButton graphP_hmapB;
	private JRadioButton graphP_sstepB;
	private JRadioButton graphP_hidden_styleB;
	private JPanel stylePanel;
	private JPanel graphPanel;
	private JPanel annotationPanel;
	private ColorComboBox styleP_fgColorComboBox;
	private ColorComboBox styleP_bgColorComboBox;
	private ColorComboBox styleP_labelFGComboBox;
	private ColorSchemeComboBox styleP_colorSchemeBox;
	private JTextField annotP_maxStackDepthTextField;
	private JTextField trackName;
	private JComboBox styleP_trackNameSizeComboBox;
	private JSlider height_slider;
	private JCheckBox graphP_yaxisCB;
	private JComboBox graphP_heat_mapCB;
	private JCheckBox graphP_floatCB;

	public JPanel rangePanel = new JPanel();
    public ButtonGroup stylegroup = new ButtonGroup();

	private final List<RootSeqSymmetry> rootSyms = new ArrayList<RootSeqSymmetry>();
	private final List<RootSeqSymmetry> graphSyms = new ArrayList<RootSeqSymmetry>();
	private final List<RootSeqSymmetry> annotSyms = new ArrayList<RootSeqSymmetry>();
	private final List<TierGlyph> selectedTiers = new ArrayList<TierGlyph>();
	private final List<ViewModeGlyph> allGlyphs = new ArrayList<ViewModeGlyph>();
	private final List<AbstractGraphGlyph> graphGlyphs = new ArrayList<AbstractGraphGlyph>();
	private IGBService igbService;
	private TrackPreferencesGUI trackPreferencesGUI;
	private YScaleAxisGUI yScaleAxisGUI;

	public TrackAdjusterTab(IGBService _igbService) {
		super(_igbService, BUNDLE.getString("trackAdjusterTab"), BUNDLE.getString("trackAdjusterTab"), false, TAB_POSITION);
		igbService = _igbService;
		vis_bounds_setter = new GraphVisibleBoundsSetter(igbService.getSeqMap());
		setLayout(new FlowLayout());
		trackPreferencesGUI = new TrackPreferencesGUI();
		yScaleAxisGUI = new YScaleAxisGUI(this);
	    add(trackPreferencesGUI);
	    add(yScaleAxisGUI);
	    assignTrackPreferences(trackPreferencesGUI);
		igbService.addListSelectionListener(styleP_colorSchemeBox);
		graphP_hidden_styleB = new JRadioButton();
		trackPreferencesGUI.getButtonGroup1().add(graphP_hidden_styleB);
		graphP_hidden_styleB.setSelected(true); // deselect all visible radio buttons
		refreshSelection(Collections.<RootSeqSymmetry>emptyList());
		gmodel = GenometryModel.getGenometryModel();
		gmodel.addSeqSelectionListener(this);
		gmodel.addSymSelectionListener(this);
		TrackstylePropertyMonitor.getPropertyTracker().addPropertyListener(this);
		igbService.addListSelectionListener(this);
	}

	private void assignTrackPreferences(TrackPreferencesGUI trackPreferencesGUI) {
		selectAllCB = trackPreferencesGUI.getSelectAllComboBox();
	    selectAllCB.setModel(new DefaultComboBoxModel(SELECT_ALL_ITEMS));
	    selectAllCB.addActionListener(
	    	new ActionListener() {
	    		@Override
	    		public void actionPerformed(ActionEvent evt) {
	    			JComboBox selectAllCB = (JComboBox)evt.getSource();
	    			String displayItem = (String)selectAllCB.getSelectedItem();
	    			if (SELECT_ALL_PROMPT.equals(displayItem)) {
	    				return;
	    			}
	    			String item = displayItem.substring(0, displayItem.length() - SELECT_ALL_SUFFIX.length());
	    			if (SELECT_ALL_ITEM.equals(item)) {
	    				SelectAllAction.getAction().execute();
	    			}
	    			else if (SELECT_NONE_ITEM.equals(item)) {
	    				DeselectAllAction.getAction().execute();
	    			}
	    			else {
	    				SelectAllAction.getAction(FileTypeCategory.valueOf(item)).execute();
	    			}
	    			selectAllCB.setSelectedIndex(0);
	    		}
	    	}
	    );
		graphP_mmavgB = trackPreferencesGUI.getGraphStyleMinMaxAvgRadioButton();
		graphP_lineB = trackPreferencesGUI.getGraphStyleLineRadioButton();
		graphP_barB = trackPreferencesGUI.getGraphStyleBarRadioButton();
		graphP_dotB = trackPreferencesGUI.getGraphStyleDotRadioButton();
		graphP_hmapB = trackPreferencesGUI.getGraphStyleHeatMapRadioButton();
		graphP_sstepB = trackPreferencesGUI.getGraphStyleStaiStepRadioButton();
		graphP_barB.addActionListener(new GraphStyleSetter(GraphType.BAR_GRAPH));
		graphP_dotB.addActionListener(new GraphStyleSetter(GraphType.DOT_GRAPH));
		graphP_hmapB.addActionListener(new GraphStyleSetter(GraphType.HEAT_MAP));
		graphP_lineB.addActionListener(new GraphStyleSetter(GraphType.LINE_GRAPH));
		graphP_mmavgB.addActionListener(new GraphStyleSetter(GraphType.MINMAXAVG));
		graphP_sstepB.addActionListener(new GraphStyleSetter(GraphType.STAIRSTEP_GRAPH));
		stylePanel = trackPreferencesGUI.getStylePanel();
		graphPanel = trackPreferencesGUI.getGraphPanel();
		annotationPanel = trackPreferencesGUI.getAnnotationsPanel();
		styleP_fgColorComboBox = trackPreferencesGUI.getForegroundColorComboBox();
		styleP_fgColorComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				if (igbService.getSeqMap() == null) {
					return;
				}
				Color color = styleP_fgColorComboBox.getSelectedColor();
				if (color != null) {
					for (TierGlyph tier : selectedTiers) {
						tier.getAnnotStyle().setForeground(color);
					}
				}
				igbService.getSeqMapView().updatePanel();
			}
			}
		);
		styleP_bgColorComboBox = trackPreferencesGUI.getBackgroundColorComboBox();
		styleP_bgColorComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				if (igbService.getSeqMap() == null) {
					return;
				}
				Color color = styleP_bgColorComboBox.getSelectedColor();
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
			}
		);
		styleP_labelFGComboBox = trackPreferencesGUI.getLabelColorComboBox();
		styleP_labelFGComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				if (igbService.getSeqMap() == null) {
					return;
				}
				Color color = styleP_labelFGComboBox.getSelectedColor();
				if (color != null) {
					for (TierGlyph tier : selectedTiers) {
						tier.getAnnotStyle().setLabelForeground(color);
					}
				}
				ThreadUtils.runOnEventQueue(new Runnable() {
		
					public void run() {
						igbService.getSeqMap().updateWidget();
					}
				});
			}
			}
		);
		styleP_colorSchemeBox = (ColorSchemeComboBox)trackPreferencesGUI.getColorSchemeComboBox();
		annotP_maxStackDepthTextField = trackPreferencesGUI.getStackDepthTextField();
		annotP_maxStackDepthTextField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				String mdepth_string = ((JTextField)evt.getSource()).getText();
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
			
		});
		trackName = trackPreferencesGUI.getTrackNameTextField();
		trackName.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				String name = ((JTextField)evt.getSource()).getText();
				if (igbService.getSeqMapView() == null) {
					return;
				}
				if (selectedTiers != null) {
					igbService.getSeqMapView().renameTier(selectedTiers.get(0), name);
				}
			}
		});
		styleP_trackNameSizeComboBox = trackPreferencesGUI.getLabelSizeComboBox();
		styleP_trackNameSizeComboBox.addItemListener(
			new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent ie) {
					if (ie.getStateChange() == ItemEvent.SELECTED) {
						String fontstring = (String) ie.getItem();
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
				}
			}
		);
		height_slider = trackPreferencesGUI.getHeightSlider();
		height_slider.addChangeListener(
			new ChangeListener() {
			   public void stateChanged(ChangeEvent e) {
				   int height = height_slider.getValue();
					for (ViewModeGlyph gl : allGlyphs) {
						Rectangle2D.Double cbox = gl.getCoordBox();
						gl.setCoords(cbox.x, cbox.y, cbox.width, height);

						// If a graph is joined with others in a combo tier, repack that tier.
						GlyphI parentgl = gl.getParent();
						if (isTierGlyph(parentgl)) {
							parentgl.pack(igbService.getView());
						}
					}
					igbService.packMap(false, true);
			   }
			}
		);
		graphP_yaxisCB = trackPreferencesGUI.getyAxisCheckBox();
		graphP_yaxisCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean b = ((JCheckBox)e.getSource()).isSelected();
				for (AbstractGraphGlyph gl : graphGlyphs) {
					gl.setShowAxis(b);
				}
				igbService.getSeqMap().updateWidget();
			}
		});
		graphP_floatCB = trackPreferencesGUI.getFloatCheckBox();
		graphP_floatCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (graphP_floatCB.isSelected()) {
					GenericAction floatAction = GenericActionHolder.getInstance().getGenericAction("com.affymetrix.igb.action.FloatTiersAction");
					if (floatAction != null) {
						floatAction.actionPerformed(null);
					}
				}
				else {
					GenericAction unFloatAction = GenericActionHolder.getInstance().getGenericAction("com.affymetrix.igb.action.UnFloatTiersAction");
					if (unFloatAction != null) {
						unFloatAction.actionPerformed(null);
					}
				}
			}
		});
		graphP_heat_mapCB = trackPreferencesGUI.getGraphStyleHeatMapComboBox();
		graphP_heat_mapCB.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (graphGlyphs.isEmpty() || !is_listening) {
					return;
				}

				if (e.getStateChange() == ItemEvent.SELECTED) {
					String name = (String) e.getItem();
					HeatMap hm = HeatMap.getStandardHeatMap(name);

					if (hm != null) {
						for (AbstractGraphGlyph gl : graphGlyphs) {
							if ("heatmapgraph".equals(gl.getName())) {
								gl.setShowGraph(true);
								gl.setHeatMap(hm);
							}
						}
						igbService.getSeqMap().updateWidget();
					}
				}
			}
		});
	}

	private boolean isTierGlyph(GlyphI glyph) {
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

	@Override
	public void symSelectionChanged(SymSelectionEvent evt) {
		List<RootSeqSymmetry> selected_syms = evt.getAllSelectedSyms();
		// Only pay attention to selections from the main SeqMapView or its map.
		// Ignore the splice view as well as events coming from this class itself.

		Object src = evt.getSource();
		if (!(src == igbService.getSeqMapView() || src == igbService.getSeqMap())
				|| igbService.getSeqMap() == null || igbService.getSeqMapView() == null) {
			return;
		}

		refreshSelection(selected_syms);
	}

	private void refreshSelection(List<RootSeqSymmetry> selected_syms) {
		is_listening = false; // turn off propagation of events from the GUI while we modify the settings
		int symcount = selected_syms.size();
		collectSymsAndGlyphs(selected_syms, symcount);
		@SuppressWarnings("unchecked")
		List<TierGlyph> tierList = (List<TierGlyph>) igbService.getSeqMapView().getSelectedTiers();
		if (selectedTiers != tierList) {
			selectedTiers.clear();
			for (TierGlyph tier : tierList) {
				if (!selectedTiers.contains(tier)) {
					selectedTiers.add(tier);
				}
			}
		}
		resetMainPanel();
		resetStylePanel();
		resetAnnotationPanel();
		resetGraphAndRangePanel();
		is_listening = true; // turn back on GUI events
	}

	private void enablePanel(JPanel panel, boolean enabled) {
		panel.setEnabled(enabled);
		for (Component c : panel.getComponents()) {
			c.setEnabled(enabled);
		}
	}

	private void resetMainPanel() {
		// track name, view mode
		int selectedTrackCount = selectedTiers.size();
		boolean select = selectedTrackCount > 0;
		if (selectedTrackCount == 1) {
			ITrackStyleExtended style = selectedTiers.get(0).getAnnotStyle();
			if (style == null || selectedTiers.get(0).getInfo() == null) {
				return;
			}
			RootSeqSymmetry rootSym = (RootSeqSymmetry)selectedTiers.get(0).getInfo();
			FileTypeCategory category = rootSym.getCategory();
			trackName.setText(style.getTrackName());
			String viewmode = style.getViewMode();
		}
		else {
			trackName.setText("");
		}

		if (select) {
			double the_height = allGlyphs.get(0).getAnnotStyle().getHeight();
			height_slider.setValue((int) the_height);
		}
		else {
			height_slider.setValue(0);
		}
		trackName.setEnabled(select);
		height_slider.setEnabled(select);
	}

	private void resetStylePanel() {
		int selectedTrackCount = selectedTiers.size();
		boolean select = selectedTrackCount > 0;
		enablePanel(stylePanel, select);
		if (selectedTrackCount == 1) {
			ITrackStyleExtended style = selectedTiers.get(0).getAnnotStyle();
			if (style == null || style instanceof SimpleTrackStyle) {
				return;
			}
			styleP_fgColorComboBox.setSelectedColor(style.getForeground());
			styleP_bgColorComboBox.setSelectedColor(style.getBackground());
			styleP_labelFGComboBox.setSelectedColor(style.getLabelForeground());
			styleP_trackNameSizeComboBox.setSelectedItem((int) style.getTrackNameSize());
		}
		else {
			styleP_fgColorComboBox.setSelectedColor(null);
			styleP_labelFGComboBox.setSelectedColor(null);
			styleP_bgColorComboBox.setSelectedColor(null);
			styleP_trackNameSizeComboBox.setSelectedItem(null);
		}
	}

	private ITrackStyleExtended getStyle(RootSeqSymmetry rootSym) {
		ITrackStyleExtended style = null;
		for (ViewModeGlyph vg : allGlyphs) {
			if (vg.getInfo() == rootSym) {
				style = vg.getAnnotStyle();
			}
		}
		return style;
	}

	private void resetAnnotationPanel() {
		int selectedTrackCount = annotSyms.size();
		boolean select = selectedTrackCount > 0;
		enablePanel(annotationPanel, select);
		if (selectedTrackCount == 1) {
			ITrackStyleExtended style = getStyle(annotSyms.get(0));
			if (style == null) {
				return;
			}
			annotP_maxStackDepthTextField.setText(Integer.toString(style.getMaxDepth()));
		} else {
			annotP_maxStackDepthTextField.setText("");
		}
	}

	private void resetGraphAndRangePanel() {
		boolean select = graphGlyphs.size() > 0;
		enablePanel(graphPanel, select);
		enablePanel(rangePanel, select);
		// float check box, not in graph panels
		boolean allFloat = true;
		boolean anySelected = false;
		for (AbstractGraphGlyph gg : graphGlyphs) {
			anySelected = true;
			if (!gg.getAnnotStyle().getFloatTier()) {
				allFloat = false;
			}
		}
		if (igbService.getSeqMapView().getPixelFloater().getChildren() != null) {
			for (GlyphI gl : igbService.getSeqMapView().getPixelFloater().getChildren()) {
				ViewModeGlyph vg = (ViewModeGlyph)gl;
				if (vg.isSelected()) {
					anySelected = true;
					if (!((ViewModeGlyph)gl).getAnnotStyle().getFloatTier()) {
						allFloat = false;
					}
				}
			}
		}
		graphP_floatCB.setEnabled(anySelected);
		graphP_floatCB.setSelected(anySelected && allFloat);
		// graph and range panels

		boolean all_are_floating = false;
		boolean all_show_axis = false;
		boolean all_show_label = false;

		// Take the first glyph in the list as a prototype
		AbstractGraphGlyph first_glyph = null;
		GraphType graph_style = GraphType.LINE_GRAPH;
		HeatMap hm = null;
		if (select) {
			first_glyph = graphGlyphs.get(0);
			graph_style = first_glyph.getGraphStyle();
			if (graph_style == GraphType.HEAT_MAP) {
				hm = first_glyph.getHeatMap();
			}
			all_are_floating = first_glyph.getGraphState().getTierStyle().getFloatTier();
			all_show_axis = first_glyph.getGraphState().getShowAxis();
			all_show_label = first_glyph.getGraphState().getShowLabel();
		}

		// Now loop through other glyphs if there are more than one
		// and see if the graph_style and heatmap are the same in all selections
		for (AbstractGraphGlyph gl : graphGlyphs) {
			all_are_floating = all_are_floating && gl.getGraphState().getTierStyle().getFloatTier();
			all_show_axis = all_show_axis && gl.getGraphState().getShowAxis();
			all_show_label = all_show_label && gl.getGraphState().getShowLabel();
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

		selectButtonBasedOnGraphStyle(graph_style);

		if (graph_style == GraphType.HEAT_MAP) {
			graphP_heat_mapCB.setEnabled(true);
			graphP_hmapB.setSelected(true);
			if (hm == null) {
				graphP_heat_mapCB.setSelectedIndex(-1);
			} else {
				graphP_heat_mapCB.setSelectedItem(hm.getName());
			}
		} else {
			graphP_heat_mapCB.setEnabled(false);
			graphP_hmapB.setSelected(false);
		}
		vis_bounds_setter.setGraphs(graphGlyphs);

		boolean type = select;
		for (AbstractGraphGlyph graf : graphGlyphs) {
			type = !(graf.getInfo() != null && graf.getInfo() instanceof MisMatchGraphSym);
			if (type) {
				break;
			}
		}

		if (select) {
			graphP_yaxisCB.setSelected(all_show_axis);
			graphP_floatCB.setSelected(all_show_label);
		}

	}

	private void collectSymsAndGlyphs(List<RootSeqSymmetry> selected_syms, int symcount) {
		if (rootSyms != selected_syms) {
			// in certain cases selected_syms arg and grafs list may be same, for example when method is being
			//     called to catch changes in glyphs representing selected sym, not the syms themselves)
			//     therefore don't want to change grafs list if same as selected_syms (especially don't want to clear it!)
			rootSyms.clear();
			annotSyms.clear();
			graphSyms.clear();
		}
		allGlyphs.clear();
		graphGlyphs.clear();
		for (Glyph glyph : igbService.getSelectedTierGlyphs()) {
			ViewModeGlyph vg = ((TierGlyph)glyph).getViewModeGlyph();
			allGlyphs.add(vg);
			if (vg instanceof AbstractGraphGlyph) {
				if (vg instanceof MultiGraphGlyph) {
					for (GlyphI child : vg.getChildren()) {
						if (rootSyms.contains(child.getInfo())) {
							graphGlyphs.add((AbstractGraphGlyph) child);
						}
					}
				}else if (!graphGlyphs.contains(vg)) {
					graphGlyphs.add((AbstractGraphGlyph)vg);
				}
			}
		}
		// First loop through and collect graphs and glyphs
		for (int i = 0; i < symcount; i++) {
			RootSeqSymmetry rootSym = selected_syms.get(i);
			// only add to grafs if list is not identical to selected_syms arg
			if (rootSyms != selected_syms) {
				rootSyms.add(rootSym);
				if (rootSym.getCategory() == FileTypeCategory.Annotation || rootSym.getCategory() == FileTypeCategory.Alignment) {
					annotSyms.add(rootSym);
				}
				if (rootSym.getCategory() == FileTypeCategory.Graph || rootSym.getCategory() == FileTypeCategory.ScoredContainer) {
					graphSyms.add(rootSym);
				}
			}
			// add all graph glyphs representing graph sym
			//	  System.out.println("found multiple glyphs for graph sym: " + multigl.size());
//				for (Glyph g : igbService.getVisibleTierGlyphs()) {
//					ViewModeGlyph vg = ((TierGlyph) g).getViewModeGlyph();
//					if (vg instanceof MultiGraphGlyph) {
//						for (GlyphI child : vg.getChildren()) {
//							if (grafs.contains(child.getInfo())) {
//								glyphs.add((AbstractGraphGlyph) child);
//							}
//						}
//					} else if (grafs.contains(vg.getInfo())) {
//						glyphs.add((AbstractGraphGlyph) vg);
//					}
//				}
			
			Glyph glyph = igbService.getSeqMap().getItem(rootSym);
			if (glyph != null) {
				if (glyph instanceof AbstractGraphGlyph) {
					if (glyph instanceof MultiGraphGlyph) {
						for (GlyphI child : glyph.getChildren()) {
							if (rootSyms.contains(child.getInfo())) {
								graphGlyphs.add((AbstractGraphGlyph) child);
							}
						}
					}else if (!graphGlyphs.contains(glyph)) {
						graphGlyphs.add((AbstractGraphGlyph)glyph);
					}
				}
			}
		}
	}

	private void selectButtonBasedOnGraphStyle(GraphType graph_style) {
		switch (graph_style) {
			case MINMAXAVG:
				graphP_mmavgB.setSelected(true);
				break;
			case LINE_GRAPH:
				graphP_lineB.setSelected(true);
				break;
			case BAR_GRAPH:
				graphP_barB.setSelected(true);
				break;
			case DOT_GRAPH:
				graphP_dotB.setSelected(true);
				break;
			case HEAT_MAP:
				//hmapB.setSelected(true);
				graphP_hmapB.setSelected(true);
				break;
//			case STAIRSTEP_GRAPH:
//				graphP_sstepB.setSelected(true);
//				break;
			default:
				graphP_hidden_styleB.setSelected(true);
				break;
		}
	}

	@Override
	public void seqSelectionChanged(SeqSelectionEvent evt) {
		if (DEBUG_EVENTS) {
			System.out.println("SeqSelectionEvent, selected seq: " + evt.getSelectedSeq() + " received by " + this.getClass().getName());
		}
//		current_seq = evt.getSelectedSeq();
//		refreshSelection(gmodel.getSelectedSymmetries(current_seq));
		refreshSelection(Collections.<RootSeqSymmetry>emptyList());
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
			if (graphGlyphs.isEmpty() || !is_listening) {
				return;
			}

			Runnable r = new Runnable() {

				public void run() {
					String viewMode = graphType2ViewMode.get(graphType);
					HeatMap hm = (graphGlyphs.get(0)).getHeatMap();
					for (AbstractGraphGlyph sggl : new ArrayList<AbstractGraphGlyph>(graphGlyphs)) {
						sggl.setShowGraph(true);
						igbService.changeViewMode(igbService.getSeqMapView(), sggl.getAnnotStyle(), viewMode, (RootSeqSymmetry) sggl.getInfo(), sggl.getGraphState().getComboStyle());
						if ((graphType == GraphType.HEAT_MAP) && (hm != sggl.getHeatMap())) {
							hm = null;
						}
					}
					if (graphType == GraphType.HEAT_MAP) {
						graphP_heat_mapCB.setEnabled(true);
						graphP_hmapB.setSelected(true);
						if (hm == null) {
							graphP_heat_mapCB.setSelectedIndex(-1);
						} else {
							graphP_heat_mapCB.setSelectedItem(hm.getName());
						}
					} else {
						graphP_heat_mapCB.setEnabled(false);
						graphP_hmapB.setSelected(false);
						// don't bother to change the displayed heat map name
					}
				}
			};

			SwingUtilities.invokeLater(r);
		}
	}

	@Override
	public void trackstylePropertyChanged(EventObject eo) {
		refreshSelection(rootSyms);
	}
}
