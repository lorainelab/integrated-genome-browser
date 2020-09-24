package org.lorainelab.igb.genotyping;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.Scored;
import com.affymetrix.genometry.symmetry.BasicSeqSymmetry;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SingletonSeqSymmetry;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;


public class TwentyThreeAndMeVariationSym extends BasicSeqSymmetry {


    private SeqSymmetry children[];

    public TwentyThreeAndMeVariationSym(String type, BioSeq seq, int txMin, int txMax, String name, boolean forward, int[] blockMins, int[] blockMaxs) {
        super(type, seq, txMin, txMax, name, forward, blockMins, blockMaxs);
    }


    @Override
    public SeqSymmetry getChild(int index) {
        if (blockMins == null || (blockMins.length <= index)) {
            return null;
        }
        if (children == null) {
            children = new SeqSymmetry[blockMins.length];
        }

        if (children[index] == null) {
            if (forward) {
                children[index] = new ChildSum(blockMins[index], blockMaxs[index], seq);
            } else {
                children[index] = new ChildSum(blockMaxs[index], blockMins[index], seq);
            }
        }
        return children[index];
    }

    protected class ChildSum extends SingletonSeqSymmetry implements SymWithProps, Scored {

        public ChildSum(int start, int end, BioSeq seq) {
            super(start, end, seq);
        }

        public String getID() {
            return this.getID();
        }

        public Map<String, Object> getProperties() {
            return this.getProperties();
        }

        public Map<String, Object> cloneProperties() {
            return this.cloneProperties();
        }

        public Object getProperty(String key) {
            return this.getProperty(key);
        }

        public boolean setProperty(String key, Object val) {
            return this.setProperty(key, val);
        }

        public float getScore() {
            return this.getScore();
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone(); //To change body of generated methods, choose Tools | Templates.
        }
    }

    @Override
    public Map<String, Object> cloneProperties() {
        Map<String, Object> tprops = super.cloneProperties();
        return tprops;
    }

    @Override
    public Object getProperty(String key) {

        return super.getProperty(key);
    }

    public void outputBedFormat(DataOutputStream out) throws IOException {
        out.write(seq.getId().getBytes());
        out.write('\t');
        out.write(Integer.toString(txMin).getBytes());
        out.write('\t');
        out.write(Integer.toString(txMax).getBytes());
        // only first three fields are required

        // only keep going if has name
        if (name != null) {
            out.write('\t');
            out.write(getName().getBytes());
            // only keep going if has score field
        }
    }

    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            outputBedFormat(new DataOutputStream(baos));
        } catch (IOException x) {
            return x.getMessage();
        }
        return baos.toString();
    }
}
