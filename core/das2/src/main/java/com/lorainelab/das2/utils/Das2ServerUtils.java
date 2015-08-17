package com.lorainelab.das2.utils;

import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;
import com.lorainelab.das2.model.segments.Segments;
import com.lorainelab.das2.model.sources.Sources;
import com.lorainelab.das2.model.types.Types;
import com.lorainelab.igb.synonymlookup.services.DefaultSynonymLookup;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 *
 * @author dcnorris
 */
public class Das2ServerUtils {

    private static final Logger logger = LoggerFactory.getLogger(Das2ServerUtils.class);
    public final static String GENOME = "genome";
    public static final String TYPES = "types";
    public static final String SEGMENTS = "segments";
    private static JAXBContext das2SourceContext;
    private static Unmarshaller das2SourceUnmarshaller;
    private static JAXBContext das2SegmentsContext;
    private static Unmarshaller das2SegmentsUnmarshaller;
    private static JAXBContext das2TypesContext;
    private static Unmarshaller das2TypesUnmarshaller;

    public static Optional<Sources> retrieveSourcesResponse(String contextRoot) throws HttpRequestException, Exception {
        HttpRequest remoteHttpRequest = HttpRequest.get(toExternalForm(contextRoot) + GENOME)
                .acceptGzipEncoding()
                .uncompress(true)
                .trustAllCerts()
                .trustAllHosts()
                .followRedirects(true)
                .basic("guest", "guest");
        try (InputStream inputStream = remoteHttpRequest.buffer()) {
            if (das2SourceContext == null) {
                das2SourceContext = JAXBContext.newInstance(Sources.class);
                das2SourceUnmarshaller = das2SourceContext.createUnmarshaller();
            }
            SAXSource saxSource = createSaxSource(inputStream);
            Optional<Sources> sources = Optional.ofNullable((Sources) das2SourceUnmarshaller.unmarshal(saxSource));
            return sources;
        }
    }

    public static Optional<Segments> retrieveSegmentsResponse(String contextRoot, String segmentQueryUri) throws HttpRequestException, Exception {
        HttpRequest remoteHttpRequest = HttpRequest.get(toExternalForm(contextRoot) + GENOME + "/" + segmentQueryUri)
                .acceptGzipEncoding()
                .uncompress(true)
                .trustAllCerts()
                .trustAllHosts()
                .followRedirects(true)
                .basic("guest", "guest");
        try (InputStream inputStream = remoteHttpRequest.buffer()) {
            if (das2SegmentsContext == null) {
                das2SegmentsContext = JAXBContext.newInstance(Segments.class);
                das2SegmentsUnmarshaller = das2SegmentsContext.createUnmarshaller();
            }
            SAXSource saxSource = createSaxSource(inputStream);
            Optional<Segments> segments = Optional.ofNullable((Segments) das2SegmentsUnmarshaller.unmarshal(saxSource));
            return segments;
        }
    }

    public static Optional<Types> retrieveTypesResponse(String contextRoot, String genomeVersionName) throws HttpRequestException, Exception {
        HttpRequest remoteHttpRequest = HttpRequest.get(getGenomeVersionContextUrl(contextRoot, genomeVersionName) + TYPES)
                .acceptGzipEncoding()
                .uncompress(true)
                .trustAllCerts()
                .trustAllHosts()
                .followRedirects(true)
                .basic("guest", "guest");
        try (InputStream inputStream = remoteHttpRequest.buffer()) {
            if (das2TypesContext == null) {
                das2TypesContext = JAXBContext.newInstance(Types.class);
                das2TypesUnmarshaller = das2TypesContext.createUnmarshaller();
            }
            SAXSource saxSource = createSaxSource(inputStream);
            Optional<Types> types = Optional.ofNullable((Types) das2TypesUnmarshaller.unmarshal(saxSource));
            return types;
        }
    }

    public static String getGenomeVersionContextUrl(String contextRoot, String genomeVersionName) {
        return toExternalForm(contextRoot) + GENOME + "/" + genomeVersionName + "/";
    }

    public static String toExternalForm(String urlString) {
        urlString = urlString.trim();
        if (!urlString.endsWith("/")) {
            urlString += "/";
        }
        return urlString;
    }

    private static SAXSource createSaxSource(final InputStream inputSteam) throws SAXException, ParserConfigurationException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        spf.setFeature("http://xml.org/sax/features/validation", false);
        XMLReader xr = (XMLReader) spf.newSAXParser().getXMLReader();
        SAXSource source = new SAXSource(xr, new InputSource(inputSteam));
        return source;
    }

    public static Optional<String> getMatchingGenomeVersionName(final String genomeVersionName, Set<String> availableGenomeVersionNames, DefaultSynonymLookup defSynonymLookup) {
        if (availableGenomeVersionNames.contains(genomeVersionName)) {
            return Optional.of(genomeVersionName);
        } else {
            Set<String> genomeVersionSynonyms = defSynonymLookup.getSynonyms(genomeVersionName);
            Optional<String> matchingSynonym = genomeVersionSynonyms.stream().filter(syn -> availableGenomeVersionNames.contains(syn)).findFirst();
            if (matchingSynonym.isPresent()) {
                return Optional.of(matchingSynonym.get());
            }
        }
        return Optional.empty();
    }
}
