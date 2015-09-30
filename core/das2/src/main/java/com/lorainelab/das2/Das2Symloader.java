package com.lorainelab.das2;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.parsers.FileTypeHandler;
import com.affymetrix.genometry.parsers.FileTypeHolder;
import com.affymetrix.genometry.symloader.BAM;
import com.affymetrix.genometry.symloader.SymLoader;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.GeneralUtils;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.collect.Lists;
import com.lorainelab.das2.utils.Das2ServerUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class Das2Symloader extends SymLoader {

    private static final Logger logger = LoggerFactory.getLogger(Das2Symloader.class);
    private static final String DAS2_EXT = "DAS2";
    private final GenometryModel gmodel;
    private final String typeParam;

    public Das2Symloader(URI contextRoot, Optional<URI> indexUri, String dataSetName, String extension, String typeParam, GenomeVersion genomeVersion) {
        super(contextRoot, indexUri, dataSetName, genomeVersion);
        this.extension = DAS2_EXT;
        this.typeParam = typeParam;
        this.extension = extension;
        gmodel = GenometryModel.getInstance();
    }

    @Override
    public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) throws Exception {
        final String segmentParam = getSegmentParam(overlapSpan);
        HttpRequest remoteHttpRequest = HttpRequest.get(uri.toString() + "features", false, "type", typeParam, "segment", segmentParam)
                .acceptGzipEncoding()
                .uncompress(true)
                .trustAllCerts()
                .trustAllHosts()
                .followRedirects(true);
        String typeUriString = typeParam.substring(0, typeParam.indexOf(";format"));
        URI typeUri = new URI(typeUriString);
        return loadFeaturesFromQuery(overlapSpan, remoteHttpRequest, extension, typeUri);
    }

    private List<? extends SeqSymmetry> loadFeaturesFromQuery(SeqSpan overlapSpan, HttpRequest remoteHttpRequest, String extension, URI dataSetUri) {
        List<? extends SeqSymmetry> seqSyms = Lists.newArrayList();
        /**
         * Need to look at content-type of server response
         */
        BufferedInputStream bis = null;
        String content_subtype;

        try {
            BioSeq aseq = overlapSpan.getBioSeq();
            if ((overlapSpan.getMin() == 0) && (overlapSpan.getMax() == aseq.getLength())) {
                bis = remoteHttpRequest.buffer();

                content_subtype = extension;
            } else {
                logger.debug(remoteHttpRequest.toString());

                int response_code = remoteHttpRequest.code();
                String responseMessage = remoteHttpRequest.message();

                if (response_code != 200) {
                    logger.info("WARNING, HTTP response code not 200/OK: " + response_code + ", " + responseMessage);
                }

                if (response_code >= 400 && response_code < 600) {
                    logger.info("Server returned error code, aborting response parsing!");
                    return seqSyms;
                }
                String content_type = remoteHttpRequest.contentType();
                bis = remoteHttpRequest.buffer();

                content_subtype = content_type.substring(content_type.indexOf('/') + 1);
                int sindex = content_subtype.indexOf(';');
                if (sindex >= 0) {
                    content_subtype = content_subtype.substring(0, sindex);
                    content_subtype = content_subtype.trim();
                }
                if (content_subtype == null || content_type.equals("unknown") || content_subtype.equals("unknown") || content_subtype.equals("xml") || content_subtype.equals("plain")) {
                    // if content type is not descriptive enough, go by what was requested
                    content_subtype = extension;
                }
            }

            logger.debug("Parsing {} format for DAS2 feature response", content_subtype.toUpperCase());

            FileTypeHandler fileTypeHandler = FileTypeHolder.getInstance().getFileTypeHandler(content_subtype.toLowerCase());
            if (fileTypeHandler == null) {
                logger.warn("ABORTING FEATURE LOADING, FORMAT NOT RECOGNIZED: {}", content_subtype);
                return seqSyms;
            } else {
                SymLoader symL = fileTypeHandler.createSymLoader(dataSetUri, Optional.ofNullable(indexUri), featureName, aseq.getGenomeVersion());
                symL.setExtension(content_subtype);
                if (symL instanceof BAM) {
                    File bamfile = GeneralUtils.convertStreamToFile(bis, featureName);
                    bamfile.deleteOnExit();
                    BAM bam = new BAM(bamfile.toURI(), Optional.ofNullable(indexUri), featureName, aseq.getGenomeVersion());
                    //for DAS/2 responses, the bam data is already trimmed so should just load it and not build an index, note bam files loaded from a url are not parsed here but elsewhere so the only http inputs are from DAS
                    if (dataSetUri.getScheme().equals("http")) {
                        seqSyms = bam.parseAll(overlapSpan.getBioSeq(), dataSetUri.toString());
                    } else {
                        seqSyms = bam.getRegion(overlapSpan);
                    }
                } else {
                    seqSyms = symL.parse(bis, false);
                }
            }
            return seqSyms;
        } catch (HttpRequest.HttpRequestException ex) {
            logger.info("Server couldn't be accessed with query " + remoteHttpRequest.toString());
            return seqSyms;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return seqSyms;
        } finally {
            GeneralUtils.safeClose(bis);
        }
    }

    @Override
    public List<? extends SeqSymmetry> getChromosome(BioSeq seq) throws Exception {
        return super.getChromosome(seq); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<? extends SeqSymmetry> getGenome() throws Exception {
        return super.getGenome(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<BioSeq> getChromosomeList() throws Exception {
        return genomeVersion.getSeqList();
    }

    @Override
    protected void init() throws Exception {
        super.init(); //To change body of generated methods, choose Tools | Templates.
    }

    private String getSegmentParam(SeqSpan overlapSpan) {
        // example chr1;overlaps=0:2083857
        return Das2ServerUtils.toExternalForm(uri.toString()) + overlapSpan.getBioSeq().getId() + ";overlaps=" + overlapSpan.getMin() + "%3A" + overlapSpan.getMax();
    }

}
