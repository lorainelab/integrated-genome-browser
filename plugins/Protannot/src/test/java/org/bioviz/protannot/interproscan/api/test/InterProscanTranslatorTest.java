/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.interproscan.api.test;

import org.bioviz.protannot.model.Dnaseq;
import org.bioviz.protannot.model.Dnaseq.Aaseq;
import org.bioviz.protannot.model.Dnaseq.Aaseq.Simsearch;
import org.bioviz.protannot.model.Dnaseq.Aaseq.Simsearch.Simhit;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Tarun
 */
public class InterProscanTranslatorTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(InterProscanTranslatorTest.class);

//    @Test
//    public void translateTest() {
//        try {
//            InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("diff_motifs/InterProtScanResponse.xml");
//            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder newDocumentBuilder = builderFactory.newDocumentBuilder();
//            Document document = newDocumentBuilder.parse(resourceAsStream);
//            InterProscanTranslator interProscanTranslator = new InterProscanTranslator();
//            Dnaseq dnaseq = interProscanTranslator.translateFromResultDocumentToModel(document);
//            dnaseq.getVersion();
//            printDescriptors(dnaseq);
//        } catch (ParserConfigurationException | SAXException | IOException ex) {
//            Logger.getLogger(InterProscanTranslatorTest.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }

    private void printDescriptors(Dnaseq dnaseq) {
        dnaseq.getMRNAAndAaseq().stream().filter(seq -> seq instanceof Aaseq).forEach(aaseq -> {
            ((Aaseq) aaseq).getSimsearch().stream().forEach(simsearch -> {
                ((Simsearch) simsearch).getSimhit().stream().forEach(simhit -> {
                    ((Simhit) simhit).getDescriptor().stream().forEach(desc -> 
                            logger.debug(((Dnaseq.Descriptor) desc).getType() + " " + ((Dnaseq.Descriptor) desc).getValue()));
                });
            });
        });
    }

}
