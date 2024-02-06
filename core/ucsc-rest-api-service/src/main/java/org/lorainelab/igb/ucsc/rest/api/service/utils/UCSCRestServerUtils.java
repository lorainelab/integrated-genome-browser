package org.lorainelab.igb.ucsc.rest.api.service.utils;

import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import org.apache.http.client.utils.URIBuilder;
import org.lorainelab.igb.synonymlookup.services.GenomeVersionSynonymLookup;
import org.lorainelab.igb.ucsc.rest.api.service.model.ChromosomeData;
import org.lorainelab.igb.ucsc.rest.api.service.model.DnaInfo;
import org.lorainelab.igb.ucsc.rest.api.service.model.GenomesData;
import org.lorainelab.igb.ucsc.rest.api.service.model.UCSCRestTracks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.lorainelab.igb.ucsc.rest.api.service.RestApiDataProvider.READ_TIMEOUT;

public class UCSCRestServerUtils {
    private static final Logger logger = LoggerFactory.getLogger(UCSCRestServerUtils.class);
    private static final String LIST = "list";
    private static final String GET_DATA = "getData";
    private static final String UCSC_GENOMES = "ucscGenomes";
    private static final String TRACKS = "tracks";
    private static final String GENOME = "genome";
    private static final String TRACK_PARAM = "trackLeavesOnly";
    public static final String TRACK_PARAM_VALUE = "1";
    private static final String CHROMOSOMES = "chromosomes";
    public static final String SEQUENCE = "sequence";
    public static final String CHROM = "chrom";
    public static final String START = "start";
    public static final String END = "end";

    public static Optional<GenomesData> retrieveDsnResponse(String rootUrl) throws IOException {
        String url = toExternalForm(toExternalForm(rootUrl.trim()) + LIST) + UCSC_GENOMES;
        url = checkValidAndSetUrl(url);
        URL genomeUrl = new URL(url);
        String data = Resources.toString(genomeUrl, Charsets.UTF_8);
        GenomesData genomesData = new Gson().fromJson(
                data, GenomesData.class
        );
        return Optional.ofNullable(genomesData);
    }

    public static Optional<UCSCRestTracks> retrieveTracksResponse(String contextRoot, String genomeVersionName) {
        String uri = toExternalForm(toExternalForm(contextRoot.trim()) + LIST) + TRACKS;
        UCSCRestTracks ucscRestTracks = null;
        try {
            URIBuilder uriBuilder = new URIBuilder(uri);
            uriBuilder.addParameter(GENOME, genomeVersionName);
            uriBuilder.addParameter(TRACK_PARAM, TRACK_PARAM_VALUE);
            String validUrl = checkValidAndSetUrl(uriBuilder.toString());
            URL genomeUrl = new URL(validUrl);
            String data = Resources.toString(genomeUrl, Charsets.UTF_8);
            ucscRestTracks = new Gson().fromJson(
                    data, UCSCRestTracks.class
            );
            if(Objects.nonNull(ucscRestTracks))
                ucscRestTracks.setTracks(data, genomeVersionName);
        } catch (URISyntaxException | IOException e) {
            logger.error(e.getMessage(), e);
        }
        return Optional.ofNullable(ucscRestTracks);
    }

    public static Map<String, Integer> getAssemblyInfo(String url, GenomeVersion genomeVersion, Set<String> genomeContextRootMap) {
        final String genomeVersionName = genomeVersion.getName();
        Optional<String> contextRootkey = getContextRootKey(genomeVersionName, genomeContextRootMap, genomeVersion.getGenomeVersionSynonymLookup());
        if (contextRootkey.isPresent()) {
            String contextRoot = contextRootkey.get();
            return retrieveAssemblyInfoByContextRoot(url, contextRoot);
        }
        return Maps.newLinkedHashMap();
    }

    public static Map<String, Integer> retrieveAssemblyInfoByContextRoot(String contextRoot, String genomeVersionName) {
        Optional<ChromosomeData> chromosomeResponse = retrieveChromosomeResponse(contextRoot, genomeVersionName);
        if (chromosomeResponse.isPresent()) {
            ChromosomeData chromosomeData = chromosomeResponse.get();
            return chromosomeData.getChromosomes();
        }
        return Maps.newLinkedHashMap();
    }

    private static Optional<ChromosomeData> retrieveChromosomeResponse(String contextRoot, String genomeVersionName) {
        String uri = toExternalForm(toExternalForm(contextRoot.trim()) + LIST) + CHROMOSOMES;
        ChromosomeData chromosomeDate = null;
        try {
            URIBuilder uriBuilder = new URIBuilder(uri);
            uriBuilder.addParameter(GENOME, genomeVersionName);
            String validUrl = checkValidAndSetUrl(uriBuilder.toString());
            URL chromosomeUrl = new URL(validUrl);
            String data = Resources.toString(chromosomeUrl, Charsets.UTF_8);
            chromosomeDate = new Gson().fromJson(
                    data, ChromosomeData.class
            );
        } catch (URISyntaxException | IOException e) {
            logger.error(e.getMessage(), e);
        }
        return Optional.ofNullable(chromosomeDate);
    }

    public static String retrieveDna(String contextRoot, SeqSpan span, String genomeVersionName) {
        String uri = toExternalForm(toExternalForm(contextRoot.trim()) + GET_DATA) + SEQUENCE;
        try {
            URIBuilder uriBuilder = new URIBuilder(uri);
            uriBuilder.addParameter(GENOME, genomeVersionName);
            uriBuilder.addParameter(CHROM, span.getBioSeq().getId());
            uriBuilder.addParameter(START, String.valueOf(span.getMin()));
            uriBuilder.addParameter(END, String.valueOf(span.getMax()));
            String validUrl = checkValidAndSetUrl(uriBuilder.toString());
            URL dnaUrl = new URL(validUrl);
            String data = Resources.toString(dnaUrl, Charsets.UTF_8);
            DnaInfo dnaInfo = new Gson().fromJson(
                    data, DnaInfo.class
            );
            return dnaInfo.getDna();
        } catch (URISyntaxException | IOException e) {
            logger.error(e.getMessage(), e);
        }
        return "";
    }

    public static Optional<String> getContextRootKey(final String genomeVersionName, Set<String> availableGenomesSet, GenomeVersionSynonymLookup genomeVersionSynonymLookup) {
        if (availableGenomesSet.contains(genomeVersionName)) {
            return Optional.of(genomeVersionName);
        } else {
            Set<String> genomeVersionSynonyms = genomeVersionSynonymLookup.getSynonyms(genomeVersionName);
            Optional<String> matchingSynonym = genomeVersionSynonyms.stream().filter(availableGenomesSet::contains).findFirst();
            if (matchingSynonym.isPresent()) {
                return matchingSynonym;
            }
        }
        return Optional.empty();
    }

    public static String toExternalForm(String urlString) {
        urlString = urlString.trim();
        if (!urlString.endsWith("/")) {
            urlString += "/";
        }
        return urlString;
    }

    public static String checkValidAndSetUrl(String uriString) {
        try {
            URL obj = new URL(uriString);
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setReadTimeout(READ_TIMEOUT);
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_SEE_OTHER) {
                    String newUrl = conn.getHeaderField("Location");
                    return newUrl;
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return uriString;
    }
}
