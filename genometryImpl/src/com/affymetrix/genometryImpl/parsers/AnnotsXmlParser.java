package com.affymetrix.genometryImpl.parsers;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

/**
 * This class is specifically for parsing the annots.xml file used by IGB and the DAS/2 server.
 */
public abstract class AnnotsXmlParser {

	/**
	 * @param istr - stream of annots file
	 */
	public static final void parseAnnotsXml(InputStream istr, List<AnnotMapElt> annotList) throws SAXParseException {
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(istr);
			doc.getDocumentElement().normalize();

			NodeList listOfFiles = doc.getElementsByTagName("file");

			int length = listOfFiles.getLength();
			for (int s = 0; s < length && (!Thread.currentThread().isInterrupted()); s++) {
				Node fileNode = listOfFiles.item(s);
				if (fileNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				Element fileElement = (Element) fileNode;
				String filename = fileElement.getAttribute("name");
				String title = fileElement.getAttribute("title");
				String desc = fileElement.getAttribute("description");   // not currently used
				String friendlyURL = fileElement.getAttribute("url");
				String serverURL = fileElement.getAttribute("serverURL");
				String load_hint = fileElement.getAttribute("load_hint");
				String label_field = fileElement.getAttribute("label_field");
				String foreground = fileElement.getAttribute("foreground");
				String background = fileElement.getAttribute("background");
				String max_depth = fileElement.getAttribute("max_depth");
				String name_size = fileElement.getAttribute("name_size");
				String connected = fileElement.getAttribute("connected");
				String collapsed = fileElement.getAttribute("collapsed");
				String show2tracks = fileElement.getAttribute("show2tracks");
				String direction_type = fileElement.getAttribute("direction_type");
				String positive_strand_color = fileElement.getAttribute("positive_strand_color");
				String negative_strand_color = fileElement.getAttribute("negative_strand_color");
				String view_mode =  fileElement.getAttribute("view_mode");

				if (filename != null) {
					AnnotMapElt annotMapElt = new AnnotMapElt(filename, title, desc, 
							friendlyURL, serverURL, load_hint, label_field, foreground,
							background, max_depth, name_size, connected, collapsed,
							show2tracks, direction_type, positive_strand_color,
									negative_strand_color, view_mode);
					annotList.add(annotMapElt);
				}
			}
		} catch (SAXParseException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class AnnotMapElt {

		public String fileName;
		public String title;
		public String serverURL;
		public Map<String, String> props = new HashMap<String, String>();

		public AnnotMapElt(String fileName, String title) {
			this(fileName, title, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
		}

		public AnnotMapElt(String fileName, String title, String description, 
				String URL, String serverURL, String load_hint, String label_field, 
				String foreground, String background, String max_depth, String name_size, 
				String connected, String collapsed, String show2tracks, String direction_type, 
				String positive_strand_color, String negative_strand_color, String view_mode) {
			// filename's case is important, since we may be loading this file locally (in QuickLoad).
			this.fileName = fileName;
			this.title = (title == null ? "" : title);
			this.serverURL = (serverURL == null ? "" : serverURL);
			
			if(description != null && description.trim().length() > 0){
				this.props.put("description", description);
			}
			
			if(URL != null && URL.trim().length() > 0){
				this.props.put("url", URL);
			}
			
			if(load_hint != null && load_hint.trim().length() > 0){
				this.props.put("load_hint", load_hint);
			}
			
			if(label_field != null && label_field.trim().length() > 0){
				this.props.put("label_field", label_field);
			}
			
			if(foreground != null && foreground.trim().length() > 0){
				this.props.put("foreground", foreground);
			}
			
			if(background != null && background.trim().length() > 0){
				this.props.put("background", background);
			}
			
			if(max_depth != null && max_depth.trim().length() > 0){
				this.props.put("max_depth", max_depth);
			}
			
			if(name_size != null && name_size.trim().length() > 0){
				this.props.put("name_size", name_size);
			}
			
			if(connected != null && connected.trim().length() > 0){
				this.props.put("connected", connected);
			}
			
			if(collapsed != null && collapsed.trim().length() > 0){
				this.props.put("collapsed", collapsed);
			}
			
			if(show2tracks != null && show2tracks.trim().length() > 0){
				this.props.put("show2tracks", show2tracks);
			}
			
			if(direction_type != null && direction_type.trim().length() > 0){
				this.props.put("direction_type", direction_type);
			}
			
			if(positive_strand_color != null && positive_strand_color.trim().length() > 0){
				this.props.put("positive_strand_color", positive_strand_color);
			}
			
			if(negative_strand_color != null && negative_strand_color.trim().length() > 0){
				this.props.put("negative_strand_color", negative_strand_color);
			}
			
			if(view_mode != null && view_mode.trim().length() > 0){
				this.props.put("view_mode", view_mode);
			}
						
		}

		public static AnnotMapElt findFileNameElt(String fileName, List<AnnotMapElt> annotList) {
			for (AnnotMapElt annotMapElt : annotList) {
				if (annotMapElt.fileName.equalsIgnoreCase(fileName)) {
					return annotMapElt;
				}
			}
			return null;
		}

		public static AnnotMapElt findTitleElt(String title, List<AnnotMapElt> annotList) {
			for (AnnotMapElt annotMapElt : annotList) {
				if (annotMapElt.title.equalsIgnoreCase(title)) {
					return annotMapElt;
				}
			}
			return null;
		}
	}
}
