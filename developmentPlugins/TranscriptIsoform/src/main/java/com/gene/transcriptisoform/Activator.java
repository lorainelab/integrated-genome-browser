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
import com.affymetrix.genoviz.swing.AMenuItem;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.XServiceRegistrar;

public class Activator extends XServiceRegistrar<IGBService> implements BundleActivator {

    private TranscriptIsoformEvidenceVisualizationManager tievListener;

    public Activator() {
        super(IGBService.class);
    }

    @Override
    protected ServiceRegistration<?>[] getServices(BundleContext bundleContext, final IGBService igbService) throws Exception {
        tievListener = new TranscriptIsoformEvidenceVisualizationManager(igbService);
        igbService.getSeqMapView().addToRefreshList(tievListener);
        igbService.getSeqMap().addMouseListener(tievListener);
        igbService.getSeqMap().addMouseMotionListener(tievListener);
        GenometryModel.getInstance().addSeqSelectionListener(tievListener);
        JMenu transcriptIsoformMenu = new JMenu("Transcript Isoform");
        final JMenuItem selectRefTiersMenuItem = new JMenuItem("Select reference tiers");
        selectRefTiersMenuItem.addActionListener(
                new ActionListener() {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        tievListener.setRefSeqTiers((List) igbService.getSelectedTierGlyphs());//can't use generics on List
                    }
                }
        );
        transcriptIsoformMenu.add(selectRefTiersMenuItem);
        final JCheckBoxMenuItem unfoundMenuItem = new JCheckBoxMenuItem("Show unfound");
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
        final JMenu densityMenu = new JMenu("Show density");
        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem thicknessMenuItem = new JRadioButtonMenuItem("Thickness");
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
        JRadioButtonMenuItem transparencyMenuItem = new JRadioButtonMenuItem("Transparency");
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
        JRadioButtonMenuItem brightnessMenuItem = new JRadioButtonMenuItem("Brightness");
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

        return new ServiceRegistration[]{
            bundleContext.registerService(AMenuItem.class, new AMenuItem(transcriptIsoformMenu, "view"), null)
        };
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        super.stop(bundleContext);
        tievListener.clearExonConnectorGlyphs();
        ServiceReference<IGBService> igbServiceReference = bundleContext.getServiceReference(IGBService.class);
        if (igbServiceReference != null) {
            IGBService igbService = bundleContext.getService(igbServiceReference);
            igbService.getSeqMap().updateWidget();
        }
    }
}
