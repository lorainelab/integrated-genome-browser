package com.affymetrix.genometry;

/**
 *
 * @author hiralv
 */
public class Residues {

    int start, end, length = 0;
    StringBuffer residues = null;

    public Residues(String residues) {
        setResidues(residues);
    }

    public Residues(String residues, int start) {
        setStart(start);
        setResidues(residues);
    }

    public final void setResidues(String residues) {
        if (residues != null) {
            this.residues = new StringBuffer(residues);
            length = residues.length();
            if (length == 0) {
                this.end = this.start;
            } else {
                this.end = this.start + this.length - 1;
            }
        }
    }

    public String getResidues() {
        if (residues == null) {
            return null;
        }
        return residues.toString();
    }

    public String getResidues(int start, int end) {
        if (length == 0) {
            return "";
        }
        char[] carray = new char[end - start];
        try {
            residues.getChars(start - this.start, end - this.start, carray, 0);
        } catch (Exception e) {
            System.out.println("exception in Sequence.getResidues(start, end)");
            System.out.println("start = " + start + ", end = " + end);
            return null;
        }
        return new String(carray);
    }

    public final void setStart(int start) {
        if (start < 0) {
            throw new IllegalArgumentException("Start cannot be negative");
        }

        this.start = start;
        this.end = this.start + this.length - 1;
    }

    public int getStart() {
        return start;
    }

    public int getLength() {
        return length;
    }

}
