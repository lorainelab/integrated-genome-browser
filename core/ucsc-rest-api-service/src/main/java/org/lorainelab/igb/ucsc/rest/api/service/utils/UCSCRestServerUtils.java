package org.lorainelab.igb.ucsc.rest.api.service.utils;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import org.apache.http.client.utils.URIBuilder;
import org.lorainelab.igb.synonymlookup.services.GenomeVersionSynonymLookup;
import org.lorainelab.igb.ucsc.rest.api.service.model.GenomesData;
import org.lorainelab.igb.ucsc.rest.api.service.model.UCSCRestTracks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class UCSCRestServerUtils {
    private static final Logger logger = LoggerFactory.getLogger(UCSCRestServerUtils.class);
    private static final String LIST = "list";
    private static final String GET_DATA = "getData";
    private static final String UCSC_GENOMES = "ucscGenomes";
    private static final String TRACKS = "tracks";
    private static final String GENOME = "genome";
    private static final String TRACK_PARAM = "trackLeavesOnly";
    public static final String TRACK_PARAM_VALUE = "1";

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
            conn.setReadTimeout(5000);
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
