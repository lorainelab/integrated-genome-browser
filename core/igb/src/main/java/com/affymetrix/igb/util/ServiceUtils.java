/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.util;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * @version $Id: ServiceUtils.java 7505 2011-02-10 20:27:35Z hiralv $
 */
public final class ServiceUtils {

    private static final ServiceUtils instance = new ServiceUtils();
    private static final String UNKNOWN_GENOME_VERSION = "unknown";

    private ServiceUtils() {
        super();
    }

    public static final ServiceUtils getInstance() {
        return instance;
    }

    public Optional<GenomeVersion> determineAndSetGroup(final String version) {
        final GenomeVersion genomeVersion;
        GenometryModel gmodel = GenometryModel.getInstance();
        if (StringUtils.isBlank(version) || UNKNOWN_GENOME_VERSION.equals(version)) {
            genomeVersion = gmodel.getSelectedGenomeVersion();
        } else {
            genomeVersion = gmodel.getSeqGroup(version);
        }
        if (genomeVersion != null && !genomeVersion.equals(gmodel.getSelectedGenomeVersion())) {
            GeneralLoadView.getLoadView().initVersion(version);
            gmodel.setSelectedGenomeVersion(genomeVersion);
        }
        return Optional.ofNullable(genomeVersion);
    }

    /**
     * This handles the "select" API parameter. The "select" parameter can be
     * followed by one or more comma separated IDs in the form:
     * &select=<id_1>,<id_2>,...,<id_n> Example: "&select=EPN1,U2AF2,ZNF524"
     * Each ID that exists in IGB's ID to symmetry hash will be selected, even
     * if the symmetries lie on different sequences.
     *
     * @param selectParam The select parameter passed in through the API
     */
    public void performSelection(String selectParam) {

        if (StringUtils.isBlank(selectParam)) {
            return;
        }

        // split the parameter by commas
        String[] ids = selectParam.split(",");

        if (ids.length == 0) {
            return;
        }

        GenomeVersion genomeVersion = GenometryModel.getInstance().getSelectedGenomeVersion();
        List<SeqSymmetry> sym_list = new ArrayList<>(ids.length);
        for (String id : ids) {
            sym_list.addAll(genomeVersion.findSyms(id));
        }

        GenometryModel.getInstance().setSelectedSymmetriesAndSeq(sym_list, ServiceUtils.class);
    }

    public void selectFeatureAndCenterZoomStripe(String selectParam) {

        if (StringUtils.isBlank(selectParam)) {
            return;
        }

        // split the parameter by commas
        String[] ids = selectParam.split(",");

        if (ids.length == 0) {
            return;
        }

        GenomeVersion genomeVersion = GenometryModel.getInstance().getSelectedGenomeVersion();
        List<SeqSymmetry> sym_list = new ArrayList<>(ids.length);
        SeqSpan span;
        double midpoint = -1;

        for (String id : ids) {
            for (SeqSymmetry sym : genomeVersion.findSyms(id)) {
                span = sym.getSpan(0);
                if (midpoint == -1) {
                    midpoint = span.getMin() + (span.getLengthDouble() / 2);
                }
                sym_list.add(sym);
            }

        }

        GenometryModel.getInstance().setSelectedSymmetriesAndSeq(sym_list, IGB.getInstance().getMapView().getSeqMap());
        IGB.getInstance().getMapView().setZoomSpotX(midpoint);
        IGB.getInstance().getMapView().setZoomSpotY(0);
    }
}
