package com.gene.sampleselection;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.quickload.QuickLoadSymLoader;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symloader.SymLoaderTabix;
import com.affymetrix.genometry.symloader.VCF;
import com.affymetrix.igb.shared.TrackClickListener;
import com.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import com.lorainelab.igb.services.IgbService;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

public class VCFListener implements TrackClickListener, SampleSelectionCallback {

    private final List<VCF> vcfs = new ArrayList<>();
    private final IgbService igbService;
    private final Map<String, List<String>> selections;
    private TierGlyph lastClickedGlyph;

    public VCFListener(IgbService igbService) {
        super();
        this.igbService = igbService;
        selections = new HashMap<>();
    }

    private VCF getVCF(String name) {
        if (name == null) {
            return null;
        }
        for (SymLoader symLoader : vcfs) {
            if (name.equals(symLoader.getFeatureName())) {
                if (symLoader instanceof SymLoaderTabix) {
                    return (VCF) ((SymLoaderTabix) symLoader).getLineProcessor();
                }
                if (symLoader instanceof VCF) {
                    return (VCF) symLoader;
                }
                return null;
            }
        }
        return null;
    }

    private void addTrackItem(JMenu parentMenu, final String fullTrackName, final String trackName, final List<String> selectedFields) {
        final JCheckBoxMenuItem dataItem = new JCheckBoxMenuItem(trackName);
        dataItem.setSelected(selectedFields.contains(fullTrackName));
        dataItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GenomeVersion genomeVersion = GenometryModel.getInstance().getSelectedGenomeVersion();
                if (genomeVersion != null) {
                    Set<DataContainer> versions = genomeVersion.getAvailableDataContainers();
                    if (versions != null) {
                        for (DataContainer dataContainer : versions) {
                            for (DataSet feature : dataContainer.getDataSets()) {
                                feature.clear();
                                feature.setVisible();
                            }
                        }
                    }
                }
                if (dataItem.isSelected()) {
                    selectedFields.add(fullTrackName);
                } else {
                    selectedFields.remove(fullTrackName);
                }
                igbService.getSeqMap().updateWidget();
            }
        }
        );
        parentMenu.add(dataItem);
    }

    public void addSymLoader(VCF vcf) {
        vcfs.add(vcf);
    }

    @Override
    public void select(String name, boolean separateTracks, Map<String, List<String>> selections) { // callback from SampleSelectionView
        GenomeVersion genomeVersion = GenometryModel.getInstance().getSelectedGenomeVersion();
        if (genomeVersion != null) {
            Set<DataContainer> versions = genomeVersion.getAvailableDataContainers();
            if (versions != null) {
                for (DataContainer dataContainer : versions) {
                    for (DataSet feature : dataContainer.getDataSets()) {
                        if (feature.getSymL() != null && name.equals(feature.getSymL().featureName)) {
                            feature.clear();
                            feature.setVisible();
                        }
                    }
                }
            }
        }

        VCF VCF = getVCF(name);
        if (VCF != null) {
            VCF.select(name, separateTracks, selections);
        }
        igbService.getSeqMap().updateWidget();
    }

    @Override
    public void trackClickNotify(JPopupMenu popup, List<TierGlyph> selectedGlyphs) {
        if (selectedGlyphs == null || selectedGlyphs.size() != 1) {
            return;
        }
        lastClickedGlyph = selectedGlyphs.get(0);
        String name = null;
        if (lastClickedGlyph != null && lastClickedGlyph.getAnnotStyle().getTrackName() != null) {
            name = lastClickedGlyph.getAnnotStyle().getTrackName();
        }

        SampleSelectionView samplesPanel = (SampleSelectionView) igbService.getTabPanel(SampleSelectionView.class.getName());
        vcfs.clear();
        for (TierGlyph tierGlyph : selectedGlyphs) {
            ITrackStyleExtended style = tierGlyph.getAnnotStyle();
            if (style != null) {
                SymLoader symL = style.getFeature().getSymL();
                if (symL instanceof QuickLoadSymLoader) {
                    symL = ((QuickLoadSymLoader) symL).getSymLoader();
                }
                if (symL instanceof SymLoaderTabix && ((SymLoaderTabix) symL).getLineProcessor() instanceof VCF) {
                    vcfs.add((VCF) ((SymLoaderTabix) symL).getLineProcessor());
                } else if (symL instanceof VCF) {
                    vcfs.add((VCF) symL);
                }
            }
        }
        VCF VCF = getVCF(name);
        if (VCF == null) {
            samplesPanel.clear();
        } else {
            final List<String> selectedFields = VCF.getSelectedFields();
            JMenu vcfMenu = new JMenu("Variants");
            popup.add(new JSeparator());
            for (String trackName : VCF.getAllFields()) {
                addTrackItem(vcfMenu, trackName, trackName, selectedFields);
            }
            if (VCF.getSamples().size() > 0) {
                final JCheckBoxMenuItem dataItem = new JCheckBoxMenuItem("Genotypes");
                dataItem.addActionListener(
                        new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                igbService.selectTab(igbService.getTabPanel("com.gene.sampleselection.SampleSelectionView"));
                            }
                        }
                );
                vcfMenu.add(dataItem);
            }
            popup.add(vcfMenu);
            final List<String> samples = VCF.getSamples();
            final List<String> genotypeFields = VCF.getGenotypes();
            if (genotypeFields.isEmpty()) {
                samplesPanel.clear();
            } else {
                samplesPanel.setData("Variants for {0}", name,
                        genotypeFields, samples, selections, VCFListener.this);
            }
        }
    }
}
