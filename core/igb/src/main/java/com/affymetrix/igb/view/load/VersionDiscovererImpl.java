/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view.load;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genometryImpl.util.SynonymLookupServiceI;
import com.affymetrix.genometryImpl.util.VersionDiscoverer;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author dcnorris
 */
@Component(name = VersionDiscovererImpl.COMPONENT_NAME, provide = VersionDiscoverer.class)
public class VersionDiscovererImpl implements VersionDiscoverer {

    public static final String COMPONENT_NAME = "VersionDiscovererImpl";
    private Map<String, String> versionName2species;

    private SynonymLookupServiceI synonymLookupService;

    // versions associated with a given genome.
    private SetMultimap<String, GenericVersion> species2generic;

    private GenometryModel gmodel;

    @Activate
    public void activate() {
        synonymLookupService = SynonymLookup.getDefaultLookup(); //could be a service when there is time to do a little refactoring
        gmodel = GenometryModel.getInstance();
        versionName2species = new HashMap<>();
        species2generic
                = Multimaps.synchronizedSetMultimap(LinkedHashMultimap.<String, GenericVersion>create());// the list of versions associated with the species
    }

    @Override
    public synchronized GenericVersion discoverVersion(String versionID, String versionName, GenericServer gServer, Object versionSourceObj, String speciesName) {
        // Make sure we use the preferred synonym for the genome version.
        String preferredVersionName = synonymLookupService.getPreferredName(versionName);
        AnnotatedSeqGroup group = gmodel.addSeqGroup(preferredVersionName); // returns existing group if found, otherwise creates a new group
        GenericVersion gVersion = new GenericVersion(group, versionID, preferredVersionName, gServer, versionSourceObj);
        Set<GenericVersion> gVersionList = getSpecies2Generic(speciesName);
        versionName2species.put(preferredVersionName, speciesName);
        if (!gVersionList.contains(gVersion)) {
            gVersionList.add(gVersion);
        }
        group.addVersion(gVersion);
        return gVersion;
    }

    @Override
    public String versionName2Species(String versionName) {
        return versionName2species.get(versionName);
    }

    @Override
    public SetMultimap<String, GenericVersion> getSpecies2Generic() {
        return species2generic;
    }

    /**
     * Get list of versions for given species. Create it if it doesn't exist.
     *
     * @param speciesName
     * @return list of versions for the given species.
     */
    @Override
    public Set<GenericVersion> getSpecies2Generic(String speciesName) {
        return species2generic.get(speciesName);
    }

}
