package com.affymetrix.genometry.symmetry.impl;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.symmetry.SupportsGeneName;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.DESCRIPTION;
import static com.affymetrix.genometry.tooltip.ToolTipConstants.TITLE;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class UcscBedDetailSym extends UcscBedSym implements SupportsGeneName {

    private final String geneName;
    private String description;

    public UcscBedDetailSym(String type, BioSeq seq, int txMin, int txMax,
            String name, float score, boolean forward, int cdsMin, int cdsMax,
            int[] blockMins, int[] blockMaxs, String geneName, String description) {
        super(type, seq, txMin, txMax, name, score, forward, cdsMin, cdsMax, blockMins,
                blockMaxs);
        this.geneName = geneName;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getGeneName() {
        return geneName;
    }

    public Map<String, Object> cloneProperties() {
        Map<String, Object> tprops = super.cloneProperties();
        tprops.put(TITLE, geneName);
        tprops.put(DESCRIPTION, description);
        return tprops;
    }

    public Object getProperty(String key) {
        switch (key) {
            case TITLE:
                return geneName;
            case DESCRIPTION:
                return description;
            default:
                return super.getProperty(key);
        }
    }

    public void outputBedDetailFormat(DataOutputStream out) throws IOException {
        outputBedFormat(out);
        out.write('\t');
        out.write(geneName.getBytes());
        out.write('\t');
        out.write(description.getBytes());
    }

    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            outputBedDetailFormat(new DataOutputStream(baos));
        } catch (IOException x) {
            return x.getMessage();
        }
        return baos.toString();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this.geneName);
        hash = 67 * hash + Objects.hashCode(this.description);
        hash = 67 * hash + Objects.hashCode(this.getID());
        hash = 67 * hash + Objects.hashCode(this.getStart());
        hash = 67 * hash + Objects.hashCode(this.getEnd());
        hash = 67 * hash + Objects.hashCode(this.seq);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UcscBedDetailSym other = (UcscBedDetailSym) obj;
        if (!Objects.equals(this.geneName, other.geneName)) {
            return false;
        }
        if (!Objects.equals(this.getID(), other.getID())) {
            return false;
        }
        if (!Objects.equals(this.getStart(), other.getStart())) {
            return false;
        }
        if (!Objects.equals(this.getEnd(), other.getEnd())) {
            return false;
        }
        if (!Objects.equals(this.seq, other.seq)) {
            return false;
        }
        if (!Objects.equals(this.description, other.description)) {
            return false;
        }
        return true;
    }

}
