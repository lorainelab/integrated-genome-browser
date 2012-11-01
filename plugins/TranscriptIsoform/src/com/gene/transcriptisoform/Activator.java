package com.gene.transcriptisoform;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genoviz.swing.MenuUtil;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.ServiceRegistrar;

public class Activator extends ServiceRegistrar implements BundleActivator {
	private TranscriptIsoformEvidenceVisualizationManager tievListener;
	private JMenu transcriptIsoformMenu;
	
	@Override
	protected ServiceRegistration<?>[] registerService(final IGBService igbService) throws Exception {
		tievListener = new TranscriptIsoformEvidenceVisualizationManager(igbService);
		igbService.getSeqMapView().addToRefreshList(tievListener);
		igbService.getSeqMap().addMouseListener(tievListener);
		igbService.getSeqMap().addMouseMotionListener(tievListener);
		GenometryModel.getGenometryModel().addSeqSelectionListener(tievListener);
		JMenu view_menu = igbService.getMenu("view");
		transcriptIsoformMenu = new JMenu("Transcript Isoform");
		view_menu.add(transcriptIsoformMenu);
		final JMenuItem selectRefTiersMenuItem = new JMenuItem("select reference tiers");
		selectRefTiersMenuItem.addActionListener(
	    	new ActionListener() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void actionPerformed(ActionEvent e) {
					tievListener.setRefSeqTiers((List)igbService.getSelectedTierGlyphs());//can't use generics on List
				}
			}
	    );
	    transcriptIsoformMenu.add(selectRefTiersMenuItem);
		final JCheckBoxMenuItem unfoundMenuItem = new JCheckBoxMenuItem("show unfound");
	    unfoundMenuItem.addActionListener(
	    	new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					tievListener.setShowUnfound(unfoundMenuItem.isSelected());
				}
			}
	    );
	    transcriptIsoformMenu.add(unfoundMenuItem);
	    unfoundMenuItem.setSelected(true);
	    final JMenu densityMenu = new JMenu("show density");
		ButtonGroup group = new ButtonGroup();
		JRadioButtonMenuItem thicknessMenuItem = new JRadioButtonMenuItem("thickness");
		thicknessMenuItem.addActionListener(
	    	new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					tievListener.setShowDensityThickness();
				}
			}
	    );
	    densityMenu.add(thicknessMenuItem);
	    group.add(thicknessMenuItem);
	    thicknessMenuItem.setSelected(true);
	   	JRadioButtonMenuItem transparencyMenuItem = new JRadioButtonMenuItem("transparency");
	   	transparencyMenuItem.addActionListener(
	    	new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					tievListener.setShowDensityTransparency();
				}
			}
	    );
	    densityMenu.add(transparencyMenuItem);
	    group.add(transparencyMenuItem);
	    transparencyMenuItem.setSelected(false);
	    JRadioButtonMenuItem brightnessMenuItem = new JRadioButtonMenuItem("brightness");
	    brightnessMenuItem.addActionListener(
	    	new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					tievListener.setShowDensityBrightness();
				}
			}
	    );
	    densityMenu.add(brightnessMenuItem);
	    group.add(brightnessMenuItem);
	    brightnessMenuItem.setSelected(false);
	    transcriptIsoformMenu.add(densityMenu);
		
		return null;
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		tievListener.clearExonConnectorGlyphs();
    	ServiceReference<IGBService> igbServiceReference = bundleContext.getServiceReference(IGBService.class);
        if (igbServiceReference != null) {
        	IGBService igbService = bundleContext.getService(igbServiceReference);
        	igbService.getSeqMap().updateWidget();
    		MenuUtil.removeFromMenu(igbService.getMenu("view"), transcriptIsoformMenu);
        }
	}
}
