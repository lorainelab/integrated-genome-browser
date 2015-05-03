/**
 * Copyright (c) 2006 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License").
 * A copy of the license must be included with any distribution of
 * this source code.
 * Distributions from Affymetrix, Inc., place this in the
 * IGB_LICENSE.html file.
 *
 * The license is also available at
 * http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.parsers;

import affymetrix.calvin.data.CHPTilingEntry;
import affymetrix.calvin.data.TilingSequenceData;
import affymetrix.calvin.parameter.ParameterNameValue;
import affymetrix.fusion.chp.FusionCHPData;
import affymetrix.fusion.chp.FusionCHPDataReg;
import affymetrix.fusion.chp.FusionCHPGenericData;
import affymetrix.fusion.chp.FusionCHPHeader;
import affymetrix.fusion.chp.FusionCHPLegacyData;
import affymetrix.fusion.chp.FusionCHPQuantificationData;
import affymetrix.fusion.chp.FusionCHPQuantificationDetectionData;
import affymetrix.fusion.chp.FusionCHPTilingData;
import affymetrix.fusion.chp.FusionExpressionProbeSetResults;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.GraphSymUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ChpParser {

    private static final boolean DEBUG = false;
    private static boolean reader_registered = false;

    public static List<? extends SeqSymmetry> parse(String file_name, boolean annotate_seq) throws IOException {
        List<? extends SeqSymmetry> results = null;
        if (!(reader_registered)) {
            FusionCHPTilingData.registerReader();
            FusionCHPQuantificationData.registerReader();
            FusionCHPQuantificationDetectionData.registerReader();
            FusionCHPLegacyData.registerReader();
            reader_registered = true;
        }

        System.out.println("Parsing CHP file: " + file_name);

        FusionCHPData chp = FusionCHPDataReg.read(file_name);
        if (chp == null) {
            ErrorHandler.errorPanel("Could not parse file: " + file_name);
            throw new IOException("Cannot load data from the file");
        }

        // The following function will determine if the CHP file read contains "legacy" format data. This
        // can be either a GCOS/XDA file or a Command Console file. The "legacy" format data is that
        // which contains a signal, detection call, detection p-value, probe pairs, probe pairs used and
        // comparison results if a comparison analysis was performed. This will be from the MAS5 algorithm.
        // Note: The file may also contain genotyping results from the GTYPE software. The ExtractData function
        // will perform a check to ensure it is an expression CHP file.
        FusionCHPQuantificationData qchp;
        FusionCHPQuantificationDetectionData qdchp;
        FusionCHPTilingData tilechp;
        FusionCHPLegacyData legchp;
        FusionCHPGenericData genchp;
        boolean has_coord_data = false;

        try {
            /**
             * For all chips other than tiling (and potentially resequencing?),
             * the genomic location of the
             * probesets is not specified in the CHP file. Therefore it needs to
             * be obtained from another
             * source, based on info that _is_ in the CHP file and the current
             * genome/AnnotatedSeqGroup (or
             * most up-to-date genome for the organism if current genome is not
             * for same organism as CHP file data
             *
             * Plan is to get this data from DAS/2 server in as optimized a
             * format as possible -- for instance,
             * "bp2" format for exon chips. It may be possible to optimize
             * formats even further if parser
             * can assume a particular ordering of data in CHP file will always
             * be followed for a particular
             * Affy chip, but I don't think we can make that assumption...
             * Therefore will have to join location info with CHP info based on
             * shared probeset IDs.
             */
            /**
             * expression CHP file (gene or WTA), without detection
             */
            /**
             * tiling CHP file
             */
            if ((tilechp = FusionCHPTilingData.fromBase(chp)) != null) {
                System.out.println("CHP file is for tiling array: " + tilechp);
                results = parseTilingChp(tilechp, annotate_seq, true);
                has_coord_data = true;
            } /**
             * legacy data
             */
            else if ((legchp = FusionCHPLegacyData.fromBase(chp)) != null) {
                System.out.println("CHP file is for legacy data: " + legchp);
                results = parseLegacyChp(legchp);
            } else if ((genchp = FusionCHPGenericData.fromBase(chp)) != null) {
                System.out.println("CHP file is generic: " + genchp);
                //      results = parseGenericChp(genchp);
                System.out.println("WARNING: generic CHP files currently not supported in IGB");
                ErrorHandler.errorPanel("CHP file is in generic format, cannot be loaded");
            } else {
                System.out.println("WARNING: not parsing file, CHP file type not recognized: " + chp);
                ErrorHandler.errorPanel("CHP file type not recognized, cannot be loaded");
            }
        } catch (Exception ex) {
            ErrorHandler.errorPanel("Chp parsing failed", "Chp parsing failed with the following exception:", ex);
        }
        if (!has_coord_data) {
            /**
             * make lazy stub annotations for each sequence in genome
             *
             */
        }
        if (results == null) {
            throw new IOException("Cannot load data from the file");
        }
        return results;
    }

    private static List<? extends SeqSymmetry> parseLegacyChp(FusionCHPLegacyData chp) throws Exception {
        FusionCHPHeader header = chp.getHeader();
        System.out.println("Alg name: " + header.getAlgName());
        System.out.println("Alg version: " + header.getAlgVersion());
        System.out.println("Assay type: " + header.getAssayType());
        System.out.println("Chip type: " + header.getChipType());
        System.out.println("Rows: " + header.getRows() + ", Columns: " + header.getCols());
        System.out.println("Parent .cel file: " + header.getParentCellFile());
        System.out.println("Program ID: " + header.getProgID());

        int probe_count = header.getNumProbeSets();
        System.out.println("number of probesets: " + probe_count);

        float[] pvals = new float[probe_count];
        float[] signals = new float[probe_count];
        FusionExpressionProbeSetResults exp = new FusionExpressionProbeSetResults();
        for (int i = 0; i < probe_count; i++) {
            chp.getExpressionResults(i, exp);
            pvals[i] = exp.getDetectionPValue();
            signals[i] = exp.getSignal();
            exp.clear();
        }
        System.out.println("Stopped loading, parsing Legacy CHP data only partially implemented!");
        ErrorHandler.errorPanel("CHP file is in legacy format, cannot be loaded");
        return null;
    }

    private static List<GraphSym> parseTilingChp(FusionCHPTilingData tchp, boolean annotate_seq, boolean ensure_unique_id) throws Exception {
        GenometryModel gmodel = GenometryModel.getInstance();
        List<GraphSym> results = new ArrayList<>();
        int seq_count = tchp.getNumberSequences();
        String alg_name = tchp.getAlgName();
        String alg_vers = tchp.getAlgVersion();

        System.out.println("seq_count = " + seq_count + ", alg_name = " + alg_name + ", alg_vers = " + alg_vers);

        Map<String, String> file_prop_hash = new LinkedHashMap<>();
        List<ParameterNameValue> alg_params = tchp.getAlgParams();
        for (ParameterNameValue param : alg_params) {
            String pname = param.getName();
            String pval = param.getValueText();
            // unfortunately, param.getValueText() is NOT Ascii text, as it is supposed to be.
            // the character encoding is unknown, so the text looks like garbage (at least on Unix).
            //      System.out.println("   param:  name = " + pname + ", val = " + pval);
            file_prop_hash.put(pname, pval);
        }

        GenomeVersion genomeVersion = null;
        BioSeq aseq = null;

        for (int i = 0; i < seq_count; i++) {
            tchp.openTilingSequenceDataSet(i);
            TilingSequenceData seq = tchp.getTilingSequenceData();
            int entry_count = tchp.getTilingSequenceEntryCount(i);

            String seq_name = seq.getName();
            String seq_group_name = seq.getGroupName();
            String seq_vers = seq.getVersion();
            System.out.println("seq " + i + ", name = " + seq_name + ", genomeVersion = " + seq_group_name
                    + ", version = " + seq_vers + ", datapoints = " + entry_count);

            // try and match up chp seq to a BioSeq and GenomeVersion in GenometryModel
            // if seq genomeVersion can't be matched, make a new seq genomeVersion
            // if seq can't be matched, make a new seq
            // trying three different ways of matching up to GenomeVersion
            // 1. concatenation of seq_group_name and seq_vers (standard way of mapping CHP ids to GenomeVersion)
            // 2. just seq_group_name
            // 3. just seq_vers
            String groupid = seq_group_name + ":" + seq_vers;
            genomeVersion = gmodel.getSeqGroup(groupid);
            if (genomeVersion == null) {
                genomeVersion = gmodel.getSeqGroup(seq_group_name);
            }
            if (genomeVersion == null) {
                genomeVersion = gmodel.getSeqGroup(seq_vers);
            }
            // if no GenomeVersion matches found, create a new one
            if (genomeVersion == null) {
                System.out.println("adding new seq genomeVersion: " + groupid);
                genomeVersion = gmodel.addGenomeVersion(groupid);
            }

            if (gmodel.getSelectedGenomeVersion() != genomeVersion) {
                // This is necessary to make sure new groups get added to the DataLoadView.
                // maybe need a SeqGroupModifiedEvent class instead.
                gmodel.setSelectedGenomeVersion(genomeVersion);
            }

            aseq = genomeVersion.getSeq(seq_name);
            if (aseq == null) {
                System.out.println("adding new seq: id = " + seq_name + ", genomeVersion = " + genomeVersion.getName());
                aseq = genomeVersion.addSeq(seq_name, 0);
            }

            int[] xcoords = new int[entry_count];
            float[] ycoords = new float[entry_count];
            CHPTilingEntry entry;
            for (int dindex = 0; dindex < entry_count; dindex++) {
                entry = tchp.getTilingSequenceEntry(dindex);
                int pos = entry.getPosition();
                float score = entry.getValue();
                xcoords[dindex] = pos;
                ycoords[dindex] = score;
            }
            int last_base_pos = xcoords[xcoords.length - 1];
            if (aseq.getLength() < last_base_pos) {
                aseq.setLength(last_base_pos);
            }
            String graph_id = GraphSymUtils.getGraphNameForFile(tchp.getFileName());
            if (ensure_unique_id) {
                graph_id = GraphSymUtils.getUniqueGraphID(graph_id, aseq);
            }
            GraphSym gsym = new GraphSym(xcoords, ycoords, graph_id, aseq);

            for (Map.Entry<String, String> ent : file_prop_hash.entrySet()) {
                gsym.setProperty(ent.getKey(), ent.getValue());
            }

            List<ParameterNameValue> seq_params = seq.getParameters();
            for (ParameterNameValue param : seq_params) {
                String pname = param.getName();
                String pval = param.getValueText();
                //	System.out.println("   param:  name = " + pname + ", val = " + pval);
                gsym.setProperty(pname, pval);
            }

            // add all seq_prop_hash entries as gsym properties
            results.add(gsym);
            if (annotate_seq) {
                aseq.addAnnotation(gsym);
            }
        }

        gmodel.setSelectedGenomeVersion(genomeVersion);
        gmodel.setSelectedSeq(aseq);
        return results;
    }

    /**
     * Finds all symmetries with the given case-insensitive ID
     * and adds them to the given list.
     *
     * @param id a case-insensitive id.
     * @param results the list to which entries will be appended.
     * It is responsibility of calling code to clear out results list
     * before calling this, if desired.
     * @param try_appended_id whether to also search for ids
     * of the form id + ".1", id + ".2", etc.
     */
    // TODO: does this routine do what is expected?
    // What if id does not exist, but id + ".1" does?
    // What if id + ".1" does not exist, but id + ".2" does?
    // Does this list need to be in order?
    private static void findSyms(GenomeVersion genomeVersion, String id, List<SeqSymmetry> results, boolean try_appended_id) {
        if (id == null) {
            return;
        }

        Set<SeqSymmetry> seqsym_list = genomeVersion.findSyms(id);
        if (!seqsym_list.isEmpty()) {
            results.addAll(seqsym_list);
            return;
        }

        if (!try_appended_id) {
            return;
        }

        // try id appended with ".n" where n is 0, 1, etc. till there is no match
        int postfix = 0;
        for (Set<SeqSymmetry> seq_sym_list = genomeVersion.findSyms(id + "." + postfix++); !seq_sym_list.isEmpty(); seq_sym_list = genomeVersion.findSyms(id + "." + postfix++)) {
            results.addAll(seq_sym_list);
        }
    }

}

/**
 * For sorting single-score probeset entries
 */
abstract class ScoreEntry {

    SeqSymmetry sym;
}

class OneScoreEntry extends ScoreEntry {

    float score;

    public OneScoreEntry(SeqSymmetry sym, float score) {
        this.sym = sym;
        this.score = score;
    }
}

/**
 * For sorting single-score probeset entries
 */
class ScoreEntryComparator implements Comparator<ScoreEntry> {

    public int compare(ScoreEntry seA, ScoreEntry seB) {
        SeqSpan symA = seA.sym.getSpan(0);
        SeqSpan symB = seB.sym.getSpan(0);
        if (symA.getMin() < symB.getMin()) {
            return -1;
        } else if (symA.getMin() > symB.getMin()) {
            return 1;
        } else {  // mins are equal, try maxes
            if (symA.getMax() < symB.getMax()) {
                return -1;
            } else if (symA.getMax() > symB.getMax()) {
                return 1;
            } else {
                return 0;
            }  // mins are equal and maxes are equal, so consider them equal
        }
    }
}
