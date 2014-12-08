package com.affymetrix.igb.update;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.osgi.framework.Version;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author hiralv
 */
public class UpdateParser {

	private static final String UPDATE = "update";
	private static final String VERSION = "version";
	private static final String RELEASE_DATE = "release_date";
	private static final String LINK = "link";

	/*
	 * Parsers given input stream
	 */
	public static List<Update> parse(String updateXml) throws ParserConfigurationException, SAXException, IOException {
		List<Update> updates = new ArrayList<Update>();

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(new ByteArrayInputStream(updateXml.getBytes()));
		doc.getDocumentElement().normalize();

		NodeList nList = doc.getElementsByTagName(UPDATE);
		Node nNode;
		Element eElement;
		Version version;
		String link;
		Date release_date;
		for (int i = 0; i < nList.getLength(); i++) {
			nNode = nList.item(i);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				eElement = (Element) nNode;
				try {
					version = new Version(getText(eElement, VERSION));
					link = getText(eElement, LINK);
					release_date = DateFormat.getDateInstance().parse(getText(eElement, RELEASE_DATE));

					updates.add(new Update(version, release_date, link));
				} catch (ParseException ex) {

				}
			}
		}

		return updates;
	}

	private static String getText(Element element, String tag) {
		return element.getElementsByTagName(tag).item(0).getTextContent();
	}
}
