package com.affymetrix.igb.action;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.parsers.AnnotationWriter;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.RootSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.CompositeGraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.TypeContainerAnnot;
import com.affymetrix.genometry.util.ExportFileModel;
import com.affymetrix.genometry.util.FileTypeCategoryUtils;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.IgbServiceImpl;
import org.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import java.awt.event.KeyEvent;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ExportFileAction
        extends AbstractExportFileAction {

    private static final long serialVersionUID = 1L;
    private static final ExportFileAction ACTION = new ExportFileAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
        ACTION.setEnabled(false);
        GenometryModel.getInstance().addSymSelectionListener(ACTION);
    }

    public static ExportFileAction getAction() {
        return ACTION;
    }

    private ExportFileAction() {
        super(BUNDLE.getString("saveTrackAction"), null,
                "16x16/actions/save.png",
                "22x22/actions/save.png",
                KeyEvent.VK_UNDEFINED, null, true);
        putValue(SHORT_DESCRIPTION, BUNDLE.getString("saveTrackActionTooltip"));
        this.ordinal = -9007100;
        setKeyStrokeBinding("ctrl S");
    }

    @Override
    protected void exportFile(AnnotationWriter annotationWriter, DataOutputStream dos, BioSeq aseq, TierGlyph atier) throws java.io.IOException {
        List<SeqSymmetry> syms = new ArrayList<>();
        RootSeqSymmetry rootSeqSymmetry = (RootSeqSymmetry) atier.getInfo();
        FileTypeCategory category = rootSeqSymmetry.getCategory();
        GenomeVersion genomeVersion = aseq.getGenomeVersion(); //removed from if block in IGBF-1090
        List<BioSeq> seql = genomeVersion.getSeqList(); //removed from if block in IGBF-1090
        // If the selected track is an annotation (if block) or a graph track (else-if block), 
        // then collect and save all loaded data from that data set from all chromosomes.
        // Otherwise (else block), just save the current chromosomes data. (Not sure when this would be the case.)
        if (FileTypeCategoryUtils.isFileTypeCategoryContainer(category) && (rootSeqSymmetry instanceof TypeContainerAnnot)) {
            for (BioSeq aseql : seql) {
                RootSeqSymmetry rootSym = aseql.getAnnotation(((TypeContainerAnnot) atier.getInfo()).getType());
                if (rootSym != null) {
                    syms.clear(); //syms = new ArrayList<>();//IGBF-1090
                    ExportFileModel.collectSyms(rootSym, syms, atier.getAnnotStyle().getGlyphDepth());
                    annotationWriter.writeAnnotations(syms, aseql, "", dos);
                }
            }
        //This section was heavily modified for IGBF-1090 <Ivory Blakley, Jennifer Daly, Devdatta Kalkarni>
        } else if (rootSeqSymmetry instanceof GraphSym) {
                //reference the selected track to test for matching tracks from each chromosome
                String selectedSym = rootSeqSymmetry.getID(); 
                seql.forEach(aseql -> {
                    syms.addAll(aseql.getAnnotations(Pattern.compile(".*"))//This regex allows for all tracks to be returned.
                            .stream()
                            .filter(s -> selectedSym.equals(s.getID())) //only save the tracks that match the selected one
                            .collect(Collectors.toList()));
                });
                annotationWriter.writeAnnotations(syms, aseq, genomeVersion.getName(), dos);
        } else {
                syms.add(rootSeqSymmetry);
                annotationWriter.writeAnnotations(syms, aseq, "", dos);
            }
    }
    
    
}
