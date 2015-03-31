package apollo.action;

import apollo.analysis.RemoteBlastNCBI;
import com.affymetrix.genometry.AminoAcid;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SupportsCdsSpan;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleMutableSeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;
import com.lorainelab.igb.genoviz.extensions.SeqMapViewI;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class BlastPSearchAction extends BlastSearchAction {

    private static final long serialVersionUID = 1L;

    public BlastPSearchAction(SeqMapViewI smv) {
        super(smv, RemoteBlastNCBI.BlastType.blastp);
    }

    @Override
    protected SeqSymmetry getResidueSym() {
        return smv.getSelectedSyms().get(0);
    }

    @Override
    public String getSequence(SeqSymmetry residues_sym) {
        SeqSpan cdsSpan = null;
        if ((residues_sym instanceof SupportsCdsSpan) && ((SupportsCdsSpan) residues_sym).hasCdsSpan()) {
            cdsSpan = ((SupportsCdsSpan) residues_sym).getCdsSpan();
        }

        if (cdsSpan == null) {
            throw new IllegalArgumentException("No CDS span present");
        }

        SimpleMutableSeqSymmetry cds_sym = new SimpleMutableSeqSymmetry();
        cds_sym.addSpan(new SimpleMutableSeqSpan(cdsSpan));
        BioSeq aseq = smv.getAnnotatedSeq();
        SeqSpan span = residues_sym.getSpan(aseq);
        StringBuilder residues = new StringBuilder();
        for (int i = 0; i < residues_sym.getChildCount(); i++) {
            SeqSymmetry child = residues_sym.getChild(i);
            SeqSpan cspan = child.getSpan(aseq);
            if (SeqUtils.contains(cdsSpan, cspan)) {
                if (cspan.isForward()) {
                    residues.append(SeqUtils.getResidues(child, aseq));
                } else {
                    residues.insert(0, SeqUtils.getResidues(child, aseq));
                }
            } else if (SeqUtils.overlap(cdsSpan, cspan)) {
                SimpleMutableSeqSymmetry cds_sym_2 = new SimpleMutableSeqSymmetry();
                SeqUtils.intersection(cds_sym, child, cds_sym_2, aseq, cspan.isForward());
                if (cspan.isForward()) {
                    residues.append(SeqUtils.getResidues(cds_sym_2, aseq));
                } else {
                    residues.insert(0, SeqUtils.getResidues(cds_sym_2, aseq));
                }
            }
        }

        if (residues.length() % 3 != 0) {
            throw new IllegalArgumentException("Residues length is not in multiple of 3");
        }

        StringBuilder aminoAcid = AminoAcid.getAminoAcid(residues.toString(), 1, span.isForward(), "");
        if (span.isForward()) {
            return aminoAcid.toString();
        }
        return aminoAcid.reverse().toString();
    }

    @Override
    public boolean isEnabled() {
        List<SeqSymmetry> syms = smv.getSelectedSyms();
        if (syms.size() == 1) {
            SeqSymmetry sym = syms.get(0);
            if ((sym instanceof SupportsCdsSpan)
                    && ((SupportsCdsSpan) sym).hasCdsSpan()) {
//				SeqSpan span = ((SupportsCdsSpan) sym).getCdsSpan();
//				if(span.getLength() % 3 == 0) {
                return true;
//				}
            }
        }
        return false;
    }
}
