/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.interproscan;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.bioviz.protannot.model.Dnaseq;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 *
 * @author jeckstei
 */
public class InterProscanTranslator {
    
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(InterProscanTranslator.class);

    public Dnaseq translateFromResultDocumentToModel(Document document) {
        Dnaseq dnaseq = new Dnaseq();
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            NodeList nodes = (NodeList) xPath.evaluate("/protein-matches/protein/sequence/matches",
                    document.getDocumentElement(), XPathConstants.NODESET);
            
        } catch (XPathExpressionException ex) {
            LOG.error(ex.getMessage(), ex);
        }

        return dnaseq;
    }
}
