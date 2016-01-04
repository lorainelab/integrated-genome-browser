package org.lorainelab.igb.das;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import org.lorainelab.igb.das.model.dsn.DasDsn;
import org.lorainelab.igb.das.model.ep.DasEp;
import org.lorainelab.igb.das.model.gff.DasGff;
import org.lorainelab.igb.das.model.gff.Segment;
import org.lorainelab.igb.das.model.types.DasTypes;
import org.lorainelab.igb.das.utils.DasServerUtils;
import static org.lorainelab.igb.das.utils.DasServerUtils.retrieveDsnResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Optional;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class DasServerInfoTest {

    private static final Logger logger = LoggerFactory.getLogger(DasServerInfoTest.class);
    private final String UCSC_DAS_URL = "http://genome.cse.ucsc.edu/cgi-bin/das/dsn/";

    @Ignore
    @Test
    public void retrieveDsnResponseTest() throws MalformedURLException, IOException {
        Optional<DasDsn> dasSources = retrieveDsnResponse(UCSC_DAS_URL);
        dasSources.ifPresent(ds -> {
            ds.getDSN().stream().forEach(dsn -> {
                String mapMaster = dsn.getMapMaster();
                String sourceId = dsn.getSOURCE().getId();
                logger.info(sourceId + ":" + mapMaster);
            });
        });
    }

    @Test
    public void retrieveDnaTest() {
        String contextRoot = "http://genome.cse.ucsc.edu:80/cgi-bin/das/hg38";
        SeqSpan seqSpan = getTestSeqSpan();
        logger.info(DasServerUtils.retrieveDna(contextRoot, seqSpan));
    }

    @Ignore
    @Test
    public void retrieveDasEpResponseTest() {
        String contextRoot = "http://genome.cse.ucsc.edu:80/cgi-bin/das/hg38";
        Optional<DasEp> retrieveDasEpResponse = DasServerUtils.retrieveDasEpResponse(contextRoot);
        if (retrieveDasEpResponse.isPresent()) {
            DasEp entryPointInfo = retrieveDasEpResponse.get();
            entryPointInfo.getENTRYPOINTS().getSEGMENT().stream().forEach(segment -> {
                logger.info(segment.getId() + ":" + segment.getStop());
            });
        }
    }

    @Ignore
    @Test
    public void retrieveDasTypesResponseTest() {
        String contextRoot = "http://genome.cse.ucsc.edu:80/cgi-bin/das/hg38";
        Optional<DasTypes> retrieveDasTypesResponse = DasServerUtils.retrieveDasTypesResponse(contextRoot);
        if (retrieveDasTypesResponse.isPresent()) {
            DasTypes entryPointInfo = retrieveDasTypesResponse.get();
            entryPointInfo.getGFF().getSEGMENT().getTYPE().forEach(type -> {
                logger.info(type.getId());
            });
        }
    }

    @Ignore
    @Test
    public void retrieveDasFeatureResponseTest() {
        String contextRoot = "http://genome.cse.ucsc.edu:80/cgi-bin/das/hg38";
        String type = "knownGene";
        SeqSpan seqSpan = getTestSeqSpan();
        Optional<DasGff> retrieveDasFeatureResponse = DasServerUtils.retrieveDasGffResponse(contextRoot, type, seqSpan);
        if (retrieveDasFeatureResponse.isPresent()) {
            DasGff dasGff = retrieveDasFeatureResponse.get();
            Optional<Segment> segment = dasGff.getGFF().getSEGMENT().stream().findFirst();
            if (segment.isPresent()) {
                segment.get().getFEATURE().stream().forEach(feature -> logger.info(feature.getId()));
            }
        }
    }

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
