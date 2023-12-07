package org.lorainelab.igb.ucsc.rest.api.service.utils;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import org.lorainelab.igb.synonymlookup.services.GenomeVersionSynonymLookup;
import org.lorainelab.igb.ucsc.rest.api.service.model.GenomesData;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.Set;

public class UCSCRestServerUtils {
    private static final String LIST = "list";
    private static final String UCSC_GENOMES = "ucscGenomes";

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
