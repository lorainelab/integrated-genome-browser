package com.lorainelab.das2;

import com.google.common.collect.Maps;
import com.lorainelab.das2.model.segments.Segments;
import com.lorainelab.das2.model.sources.Sources;
import com.lorainelab.das2.model.types.Types;
import com.lorainelab.das2.utils.Das2ServerUtils;
import java.util.Map;
import java.util.Optional;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class Das2ServerUtilsTest {

    private static final Logger logger = LoggerFactory.getLogger(Das2ServerUtilsTest.class);
    public static String DAS2_URL = "http://bioserver.hci.utah.edu:8080/DAS2DB";

    @Ignore
    @Test
    public void retrieveSourcesTest() throws Exception {
        Optional<Sources> retrieveSourcesResponse = Das2ServerUtils.retrieveSourcesResponse(DAS2_URL);
        retrieveSourcesResponse.ifPresent(sources -> {
            sources.getSource().stream().forEach(source -> {
                logger.info(source.getUri());
                System.out.println();
                source.getVersion().stream().forEach(version -> {
                    logger.info(version.getTitle());
                });
                System.out.println();
            });
        });
    }

    @Ignore
    @Test
    public void retrieveSegmentsTest() throws Exception {
        Map<String, Integer> assemblyInfo = Maps.newLinkedHashMap();
        final String queryUri = "D_rerio_Jul_2007/segments";
        Optional<Segments> retrieveSegmentsResponse = Das2ServerUtils.retrieveSegmentsResponse(DAS2_URL, queryUri);
        retrieveSegmentsResponse.ifPresent(segmentResponse -> {
            segmentResponse.getSEGMENT().stream().forEach(segment -> {
                assemblyInfo.put(segment.getTitle(), segment.getLength());
            });
        });
        assemblyInfo.entrySet().forEach(entry -> logger.info(entry.getKey() + ":" + entry.getValue()));
    }
    
    @Ignore
    @Test
    public void retrieveTypesTest() throws Exception {
        final String queryUri = "H_sapiens_Feb_2009";
        Optional<Types> retrieveSegmentsResponse = Das2ServerUtils.retrieveTypesResponse(DAS2_URL, queryUri);
        retrieveSegmentsResponse.ifPresent(types -> {
            types.getTYPE().forEach(type -> {
                logger.info(type.getTitle());
                logger.info(type.getUri());
                logger.info(type.getFORMAT().getName());
                logger.info(type.getPROP().getKey() + ":" + type.getPROP().getValueAttribute());
            });

        });
    }
}
