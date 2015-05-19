package com.lorainelab.das;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.lorainelab.das.DasResiduesHandler;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author dcnorris
 */
public class DasSymloader extends SymLoader {

    private static final String DAS_EXT = "DAS";
    private final GenometryModel gmodel;
    private final DataContainer container;

    public DasSymloader(DataContainer container, String featureName, GenomeVersion genomeVersion) {
        super(featureName, genomeVersion);
        this.extension = DAS_EXT;
        this.container = container;
        gmodel = GenometryModel.getInstance();
    }

    @Override
    public String getRegionResidues(SeqSpan span) throws Exception {
        Optional<BioSeq> selectedSeq = gmodel.getSelectedSeq();
        if (selectedSeq.isPresent()) {
            final BioSeq bioSeq = selectedSeq.get();
            String seqName = bioSeq.getId();
            DasResiduesHandler dasResiduesHandler = new DasResiduesHandler();
            String residues = dasResiduesHandler.getDasResidues(container, seqName, span.getMin(), span.getMax());
            if (residues != null) {
                return residues;
            }
        }
        return "";
    }

    @Override
    public boolean isResidueLoader() {
        return super.isResidueLoader(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) throws Exception {
        return super.getRegion(overlapSpan); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<? extends SeqSymmetry> getChromosome(BioSeq seq) throws Exception {
        return super.getChromosome(seq); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<? extends SeqSymmetry> getGenome() throws Exception {
        return super.getGenome(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<BioSeq> getChromosomeList() throws Exception {
        return super.getChromosomeList(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void init() throws Exception {
        super.init(); //To change body of generated methods, choose Tools | Templates.
    }

}
