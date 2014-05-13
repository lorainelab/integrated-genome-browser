package com.affymetrix.igb.survey;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author hiralv
 */
public class SurveyParser {

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String DESC = "description";
    private static final String LINK = "link";
    private static final String START = "startdate";
    private static final String END = "enddate";

    /*
     * Parsers given input stream
     */
    public static List<Survey> parse(InputStream inputstream) throws ParserConfigurationException, SAXException, IOException {
        List<Survey> surveys = new ArrayList<Survey>();

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputstream);
        doc.getDocumentElement().normalize();

        NodeList nList = doc.getElementsByTagName("survey");
        Node nNode;
        Element eElement;
        String id, name, description, link;
        Date start, end;
        for (int i = 0; i < nList.getLength(); i++) {
            nNode = nList.item(i);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                eElement = (Element) nNode;
                try {
                    id = eElement.getAttribute(ID);
                    name = getText(eElement, NAME);
                    description = getText(eElement, DESC);
                    link = getText(eElement, LINK);
                    start = DateFormat.getDateInstance().parse(getText(eElement, START));
                    end = DateFormat.getDateInstance().parse(getText(eElement, END));
                    surveys.add(new Survey(id, name, description, link, start, end));
                } catch (ParseException ex) {

                }
            }
        }

        return surveys;
    }

    private static String getText(Element element, String tag) {
        return element.getElementsByTagName(tag).item(0).getTextContent();
    }
}
