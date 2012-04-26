package com.affymetrix.genometryImpl.parsers;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdom.input.SAXBuilder;
import org.jdom.*;

/**
 * This class is specifically for parsing the annots.xml file used by IGB and
 * the DAS/2 server.
 * Modified by nick
 */
@SuppressWarnings("rawtypes")
public abstract class AnnotsXmlParser {

	static Element root;
	static final String folder = "folder";
	static final String file = "file";

	/**
	 * @param istr - stream of annots file
	 */
	public static final void parseAnnotsXml(InputStream istr, List<AnnotMapElt> annotList) {
		try {
			SAXBuilder docBuilder = new SAXBuilder();
			Document doc = docBuilder.build(istr);
			root = doc.getRootElement();
			List children = root.getChildren();

			if (root.getChild(folder) != null) {
				iterateAllNodesNew(children, annotList);
			} else {
				iterateAllNodesOld(children, annotList);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void iterateAllNodesNew(List list, List<AnnotMapElt> annotList) {
		String path, title;
		for (int i = 0; i < list.size(); i++) {
			Element e = (Element) list.get(i);
			if (e.getName().equalsIgnoreCase(folder)) {
				List children = e.getChildren();
				if (children != null) {
					iterateAllNodesNew(children, annotList);
				}
			} else if (e.getName().equalsIgnoreCase(file)) {
				path = getPath(e);
				title = path + e.getAttributeValue("title");
				addDataToList(annotList, e, title);
			}
		}
	}

	static void iterateAllNodesOld(List list, List<AnnotMapElt> annotList) {
		Element e;
		String title;
		for (int i = 0; i < list.size(); i++) {
			e = (Element) list.get(i);
			title = e.getAttributeValue("title");
			addDataToList(annotList, e, title);
		}
	}

	static void addDataToList(List<AnnotMapElt> annotList, Element e, String title) {
		String filename = e.getAttributeValue("name");
		String desc = e.getAttributeValue("description");   // not currently used
		String friendlyURL = e.getAttributeValue("url");
		String serverURL = e.getAttributeValue("serverURL");
		String load_hint = e.getAttributeValue("load_hint");
		String label_field = e.getAttributeValue("label_field");
		String foreground = e.getAttributeValue("foreground");
		String background = e.getAttributeValue("background");
		String max_depth = e.getAttributeValue("max_depth");
		String name_size = e.getAttributeValue("name_size");
		String connected = e.getAttributeValue("connected");
		String collapsed = e.getAttributeValue("collapsed");
		String show2tracks = e.getAttributeValue("show2tracks");
		String direction_type = e.getAttributeValue("direction_type");
		String positive_strand_color = e.getAttributeValue("positive_strand_color");
		String negative_strand_color = e.getAttributeValue("negative_strand_color");
		String view_mode = e.getAttributeValue("view_mode");
		if (filename != null) {
			AnnotMapElt annotMapElt = new AnnotMapElt(filename, title, desc,
					friendlyURL, serverURL, load_hint, label_field, foreground,
					background, max_depth, name_size, connected, collapsed,
					show2tracks, direction_type, positive_strand_color,
					negative_strand_color, view_mode);
			annotList.add(annotMapElt);
		}
	}

	static String getPath(Element e) {
		String path = "";
		while (e.getParentElement() != root) {
			e = e.getParentElement();
			path = e.getAttributeValue("name") + "/" + path;
		}

		return path;
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

			if (description != null && description.trim().length() > 0) {
				this.props.put("description", description);
			}

			if (URL != null && URL.trim().length() > 0) {
				this.props.put("url", URL);
			}

			if (load_hint != null && load_hint.trim().length() > 0) {
				this.props.put("load_hint", load_hint);
			}

			if (label_field != null && label_field.trim().length() > 0) {
				this.props.put("label_field", label_field);
			}

			if (foreground != null && foreground.trim().length() > 0) {
				this.props.put("foreground", foreground);
			}

			if (background != null && background.trim().length() > 0) {
				this.props.put("background", background);
			}

			if (max_depth != null && max_depth.trim().length() > 0) {
				this.props.put("max_depth", max_depth);
			}

			if (name_size != null && name_size.trim().length() > 0) {
				this.props.put("name_size", name_size);
			}

			if (connected != null && connected.trim().length() > 0) {
				this.props.put("connected", connected);
			}

			if (collapsed != null && collapsed.trim().length() > 0) {
				this.props.put("collapsed", collapsed);
			}

			if (show2tracks != null && show2tracks.trim().length() > 0) {
				this.props.put("show2tracks", show2tracks);
			}

			if (direction_type != null && direction_type.trim().length() > 0) {
				this.props.put("direction_type", direction_type);
			}

			if (positive_strand_color != null && positive_strand_color.trim().length() > 0) {
				this.props.put("positive_strand_color", positive_strand_color);
			}

			if (negative_strand_color != null && negative_strand_color.trim().length() > 0) {
				this.props.put("negative_strand_color", negative_strand_color);
			}

			if (view_mode != null && view_mode.trim().length() > 0) {
				this.props.put("view_mode", view_mode);
			}

		}

		public static AnnotMapElt findFileNameElt(String fileName, List<AnnotMapElt> annotList) {
			for (AnnotMapElt annotMapElt : new CopyOnWriteArrayList<AnnotMapElt>(annotList)) {
				if (annotMapElt.fileName.equalsIgnoreCase(fileName)) {
					return annotMapElt;
				}
			}
			return null;
		}

		public static AnnotMapElt findTitleElt(String title, List<AnnotMapElt> annotList) {
			for (AnnotMapElt annotMapElt : new CopyOnWriteArrayList<AnnotMapElt>(annotList)) {
				if (annotMapElt.title.equalsIgnoreCase(title)) {
					return annotMapElt;
				}
			}
			return null;
		}
	}
}
