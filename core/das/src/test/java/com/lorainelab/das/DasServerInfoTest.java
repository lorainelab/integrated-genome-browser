package com.lorainelab.das;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.github.kevinsawicki.http.HttpRequest;
import org.junit.jupiter.api.Test;
import org.lorainelab.igb.das.model.dsn.DasDsn;
import org.lorainelab.igb.das.model.ep.DasEp;
import org.lorainelab.igb.das.model.gff.DasGff;
import org.lorainelab.igb.das.model.gff.Segment;
import org.lorainelab.igb.das.model.types.DasTypes;
import org.lorainelab.igb.das.utils.DasServerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author dcnorris
 */
public class DasServerInfoTest {

    private static final Logger logger = LoggerFactory.getLogger(DasServerInfoTest.class);
    private final String UCSC_DAS_URL = "https://genome.cse.ucsc.edu/cgi-bin/das/dsn/";

    @Test
    public void retrieveDsnResponseTest() {
        try{
            Optional<DasDsn> dasSources = DasServerUtils.retrieveDsnResponse(UCSC_DAS_URL);
            dasSources.ifPresent(ds -> {
                if(ds.getDSN().stream().anyMatch(dsn -> dsn != null && dsn.getMapMaster() != null)){
                    logger.info("Successful connection while getting Dsn Response");
                }
            });
        } catch (HttpRequest.HttpRequestException e) {
            logger.warn("Connection failed to DAS Server while getting Dsn Response");
        }
    }

    @Test
    public void retrieveDnaTest() {
        try{
            String contextRoot = "http://genome.cse.ucsc.edu:80/cgi-bin/das/hg38";
            SeqSpan seqSpan = getTestSeqSpan();
            String result = DasServerUtils.retrieveDna(contextRoot, seqSpan);
            if(!result.isEmpty())
                assertEquals(result,testSeqSpanDnaString);
        } catch (HttpRequest.HttpRequestException e) {
            logger.warn("Connection failed to DAS Server while getting Dna Response");
        }
    }

    @Test
    public void retrieveDasEpResponseTest() {
        try{
            String contextRoot = "http://genome.cse.ucsc.edu:80/cgi-bin/das/hg38";
            Optional<DasEp> retrieveDasEpResponse = DasServerUtils.retrieveDasEpResponse(contextRoot);
            DasEp entryPointInfo = retrieveDasEpResponse.get();
            if(entryPointInfo.getENTRYPOINTS().getSEGMENT().stream().anyMatch(segment -> segment != null && segment.getId() != null)){
                logger.info("Successful connection while getting Entry points Response");
            }
        } catch (HttpRequest.HttpRequestException e) {
            logger.warn("Connection failed to DAS Server while getting Entry points Response");
        }
    }

    @Test
    public void retrieveDasTypesResponseTest() {
        try{
            String contextRoot = "http://genome.cse.ucsc.edu:80/cgi-bin/das/hg38";
            Optional<DasTypes> retrieveDasTypesResponse = DasServerUtils.retrieveDasTypesResponse(contextRoot);
            DasTypes entryPointInfo = retrieveDasTypesResponse.get();
            if(entryPointInfo.getGFF().getSEGMENT().getTYPE().stream().anyMatch(type -> type != null && type.getId() != null)) {
                logger.info("Successful connection while getting Types Response");
            }
        } catch (HttpRequest.HttpRequestException e){
            logger.warn("Connection failed to DAS Server while getting Types Response");
        }
    }

    @Test
    public void retrieveDasFeatureResponseTest() {
        try{
            String contextRoot = "http://genome.cse.ucsc.edu:80/cgi-bin/das/hg38";
            String type = "knownGene";
            SeqSpan seqSpan = getTestSeqSpan();
            Optional<DasGff> retrieveDasFeatureResponse = DasServerUtils.retrieveDasGffResponse(contextRoot, type, seqSpan);
            DasGff dasGff = retrieveDasFeatureResponse.get();
            Optional<Segment> segment = dasGff.getGFF().getSEGMENT().stream().findFirst();
            segment.ifPresent(value -> {
                if(value.getFEATURE().stream().anyMatch(feature -> feature != null && feature.getId()!=null)){
                    logger.info("Successful connection while getting Feature Response");
                }
            });
        } catch (HttpRequest.HttpRequestException e){
            logger.warn("Connection failed to DAS Server while getting Feature Response");
        }
    }

    private final String testSeqSpanDnaString = "cggagcgctgtcctgtcgggccgagtcgcgggcctgggcacggaactcacgctcactccgagctcccgacgtgcacacggctcccatgcgttgtcttccgagcgtcaggccgcccctacccgtgctttctgctctgcagaccctcttcccagacctccgtcctttgtcccatcgctgccttcccctcaagctcagggccaagctgtccgccagcctcggctcctccgggcagcccttgcccggggtgcgccccggggcaggacccccagcccaggcccagggcccgcccctgccctccagccctacgccttgacccgctttcctgcgtctctcagcctacctgaccttgtctttacctctgtgggcagctcccttgtgatctgcttagttcccacccccctttaagaattaaatagagaagccagacgcaaaactacagatatcgtatgagtccagttttgtgaagtgcctagaatagtcaaaattcacagagacagaagc";
    private SeqSpan getTestSeqSpan() {
        SeqSpan seqSpan = new SeqSpan() {

            @Override
            public int getStart() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public int getEnd() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public int getMin() {
                return 200000;
            }

            @Override
            public int getMax() {
                return 200500;
            }

            @Override
            public int getLength() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public boolean isForward() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public BioSeq getBioSeq() {
                return new BioSeq("1", 100000);
            }

            @Override
            public double getStartDouble() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public double getEndDouble() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public double getMaxDouble() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public double getMinDouble() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public double getLengthDouble() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public boolean isIntegral() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
        return seqSpan;
    }

}
