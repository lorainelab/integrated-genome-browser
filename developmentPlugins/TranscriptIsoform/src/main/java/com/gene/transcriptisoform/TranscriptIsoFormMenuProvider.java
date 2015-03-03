package com.gene.transcriptisoform;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.lorainelab.igb.services.IgbService;
import com.lorainelab.igb.services.window.menus.IgbMenuItemProvider;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

/**
 *
 * @author dcnorris
 */
@Component(name = TranscriptIsoFormMenuProvider.COMPONENT_NAME, immediate = true)
public class TranscriptIsoFormMenuProvider implements IgbMenuItemProvider {

    public static final String COMPONENT_NAME = "TranscriptIsoFormMenuProvider";
    private JRPMenuItem transcriptIsoformMenu;
    private TranscriptIsoformEvidenceVisualizationManager tievListener;
    private IgbService igbService;

    @Activate
    public void activate() {
        tievListener = new TranscriptIsoformEvidenceVisualizationManager(igbService);
        transcriptIsoformMenu = new JRPMenuItem("Transcript Isoform");
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
    }

    @Reference(optional = false)
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    @Deactivate
    public void deactivate() {
        tievListener.clearExonConnectorGlyphs();
        igbService.getSeqMap().updateWidget();
    }

    @Override
    public String getParentMenuName() {
        return "view";
    }

    @Override
    public JRPMenuItem getMenuItem() {
        return transcriptIsoformMenu;
    }

    @Override
    public int getMenuItemWeight() {
        return -1;
    }
}
