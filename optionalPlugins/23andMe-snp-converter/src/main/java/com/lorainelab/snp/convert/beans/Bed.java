/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.snp.convert.beans;

/**
 *
 * @author Daniel
 */
public class Bed {

    /**
     * Chromosome the Snp is located on
     */
    private final String chrom;
    /**
     * The start position of the Snp on the chromosome
     */
    private final int chromStart;
    /**
     * The end position of the Snp on the chromosome
     */
    private final int chromEnd;

    /**
     * Unique name of the Bed which is the rsid from the Snp object
     */
    private final String name;
    /**
     * A score between 0 and 1000
     */
    private final int score;
    /**
     * Defines the strand as forward (+) or reverse (-)
     */
    private final String strand;
    /**
     * Tells the browser the point at which the feature is drawn thickly
     */
    private final int thickStart;
    /**
     * Tells the browser the point at which the feature is no longer drawn
     * thickly
     */
    private final int thickEnd;
    /**
     * The number of exons in the Bed line
     */
    private final int blockCount;
    /**
     * A list of block sizes, which must be as long as the number blocks
     * (blockCount)
     */
    private final int blockSizes;
    /**
     * A list of positions at which the blocks begin, which must be as long as
     * the number of blocks (blockCount)
     */
    private final int blockStarts;

    /**
     * Unique identifier for the Bed entry which is the rsid from the Snp object
     */
    private final String ID;
    /**
     * Contains the actual genotype of the Snp
     */
    private final String description;

    /**
     * An array of RGB color values which are assigned to each entry of the
     * .bedDetail file
     */
    private final String[] RGBColors = {"255,0,0", "0,0,255", "0,255,0"};

    /**
     * The constructor for the BED entry object.
     *
     * @param s the Snp object which contains the information relevant to the
 Snp from the source file
     */
    public Bed(Snp s) {
        this.chrom = s.getChromosome();
        this.chromStart = s.getPosition() - 1;
        this.chromEnd = s.getPosition();

        this.name = s.getRsid();
        this.score = 0;
        this.strand = "+";
        this.thickStart = s.getPosition();
        this.thickEnd = s.getPosition();
        this.blockCount = 1;
        this.blockSizes = 1;
        this.blockStarts = 0;

        this.ID = s.getRsid();
        this.description = s.getGenotype();
    }

    /**
     * This method creates the actual Bed entry strings that will be printed to
 the destination file.
     * A string is created for each nucleotide from the Snp's Genotype, which
 means that least one (if not two)
 String will be created for each line in the Snp file.
     *
     * @return BEDs: an array of string which will be printed to the .bedDetail
     * file.
     */
    public String[] getBED() {
        String[] BEDs = new String[description.length()];

        if (description.length() > 1) {
            if (description.charAt(0) == description.charAt(1)) {
                BEDs[0] = String.join("\t", chrom, Integer.toString(chromStart), Integer.toString(chromEnd), name, Integer.toString(score),
                        strand, Integer.toString(chromStart), Integer.toString(chromEnd), RGBColors[1], Integer.toString(blockCount),
                        Integer.toString(blockSizes), Integer.toString(blockStarts), ID, description);
                return BEDs;
            } else {
                for (int c = 0; c < description.length(); c++) {
                    BEDs[c] = String.join("\t", chrom, Integer.toString(chromStart), Integer.toString(chromEnd), name, Integer.toString(score),
                            strand, Integer.toString(thickStart), Integer.toString(thickEnd), RGBColors[c], Integer.toString(blockCount),
                            Integer.toString(blockSizes), Integer.toString(blockStarts), ID, Character.toString(description.charAt(c)));
                }
                return BEDs;
            }
        } else {
            BEDs[0] = String.join("\t", chrom, Integer.toString(chromStart), Integer.toString(chromEnd), name, Integer.toString(score),
                    strand, Integer.toString(thickStart), Integer.toString(thickEnd), RGBColors[1], Integer.toString(blockCount),
                    Integer.toString(blockSizes), Integer.toString(blockStarts), ID, description);
            return BEDs;
        }
    }
}
