package com.affymetrix.genometryImpl.parsers;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class is specifically for parsing the annots.xml file used by IGB and the DAS/2 server.
 */
public abstract class AnnotsParser {
	/**
	 * add elements of annots file to annots_map
	 * @param istr - stream of annots file
	 * @param annots_map
	 */
	public static final void parseAnnotsXml(InputStream istr, Map<String,String> annots_map) {
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(istr);
			doc.getDocumentElement().normalize();

			NodeList listOfFiles = doc.getElementsByTagName("file");

			int length = listOfFiles.getLength();
			for (int s = 0; s < length; s++) {
				Node fileNode = listOfFiles.item(s);
				if (fileNode.getNodeType() == Node.ELEMENT_NODE) {
					Element fileElement = (Element) fileNode;
					String filename = fileElement.getAttribute("name");
					String title = fileElement.getAttribute("title");
					String desc = fileElement.getAttribute("description");   // not currently used

					if (filename != null) {
						// We use lower-case here, since filename's case is unimportant.
						annots_map.put(filename.toLowerCase(), title);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
