package com.affymetrix.genometry.servlets.das2manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.hibernate.Session;
import org.hibernate.Transaction;


import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.ParamPart;
import com.oreilly.servlet.multipart.Part;

import org.apache.catalina.realm.RealmBase;

public class Das2ManagerServlet extends HttpServlet {

	private static final String DAS2_MANAGER_HTML_WRAPPER = "Das2Manager.html";
	private static final String REALM                     = "Das2";

	private static final String SECURITY_REQUEST                   = "security";
	private static final String DICTIONARIES_REQUEST               = "dictionaries";
	private static final String ANNOTATIONS_REQUEST                = "annotations";
	private static final String ANNOTATION_REQUEST                 = "annotation";
	private static final String ORGANISM_ADD_REQUEST               = "organismAdd";
	private static final String ORGANISM_UPDATE_REQUEST            = "organismUpdate";
	private static final String ORGANISM_DELETE_REQUEST            = "organismDelete";
	private static final String GENOME_VERSION_REQUEST             = "genomeVersion";
	private static final String GENOME_VERSION_ADD_REQUEST         = "genomeVersionAdd";
	private static final String GENOME_VERSION_UPDATE_REQUEST      = "genomeVersionUpdate";
	private static final String GENOME_VERSION_DELETE_REQUEST      = "genomeVersionDelete";
	private static final String GENOME_VERSION_SEGMENT_IMPORT_REQUEST = "genomeVersionSegmentImport";
	private static final String ANNOTATION_GROUPING_ADD_REQUEST    = "annotationGroupingAdd";
	private static final String ANNOTATION_GROUPING_UPDATE_REQUEST = "annotationGroupingUpdate";
	private static final String ANNOTATION_GROUPING_MOVE_REQUEST   = "annotationGroupingMove";
	private static final String ANNOTATION_GROUPING_DELETE_REQUEST = "annotationGroupingDelete";
	private static final String ANNOTATION_ADD_REQUEST             = "annotationAdd";
	private static final String ANNOTATION_UPDATE_REQUEST          = "annotationUpdate";
	private static final String ANNOTATION_DELETE_REQUEST          = "annotationDelete";
	private static final String ANNOTATION_UNLINK_REQUEST          = "annotationUnlink";
	private static final String ANNOTATION_MOVE_REQUEST            = "annotationMove";
	private static final String FORMULATE_UPLOAD_URL_REQUEST       = "uploadURL";
	private static final String UPLOAD_FILES_REQUEST               = "uploadFiles"; 
	private static final String USERS_AND_GROUPS_REQUEST           = "usersAndGroups"; 
	private static final String USER_ADD_REQUEST                   = "userAdd";
	private static final String USER_PASSWORD_REQUEST              = "userPassword"; 
	private static final String USER_UPDATE_REQUEST                = "userUpdate"; 
	private static final String USER_DELETE_REQUEST                = "userDelete"; 
	private static final String GROUP_ADD_REQUEST                  = "groupAdd";
	private static final String GROUP_UPDATE_REQUEST               = "groupUpdate"; 
	private static final String GROUP_DELETE_REQUEST               = "groupDelete"; 
	private static final String DICTIONARY_ADD_REQUEST             = "dictionaryAdd";
	private static final String DICTIONARY_UPDATE_REQUEST          = "dictionaryUpdate"; 
	private static final String DICTIONARY_DELETE_REQUEST          = "dictionaryDelete"; 
	
	private Das2ManagerSecurity das2ManagerSecurity = null;
	
	private String genometry_db_annotation_dir;
	
	public void init() throws ServletException {
		if (getGenometryManagerDataDir() == false) {
			System.out.println("FAILED to init() Das2ManagerServlet, aborting!");
			return;
		}
	}
	
	protected void doPost(HttpServletRequest req, HttpServletResponse res)
    	throws ServletException, IOException {
		handleRequest(req, res);
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse res)
		throws ServletException, IOException {
		handleRequest(req, res);
	}

	private void handleRequest(HttpServletRequest req, HttpServletResponse res)
	        throws ServletException, IOException {
		

		try {
			
			// Get the Das2ManagerSecurity		
			das2ManagerSecurity = Das2ManagerSecurity.class.cast(req.getSession().getAttribute(Das2ManagerSecurity.SESSION_KEY));
			if (das2ManagerSecurity == null) {
				Session sess = HibernateUtil.getSessionFactory().openSession();
				
				das2ManagerSecurity = new Das2ManagerSecurity(sess, 
						                                      req.getUserPrincipal().getName(), 
						                                      true,
						                                      req.isUserInRole(Das2ManagerSecurity.ADMIN_ROLE),
						                                      req.isUserInRole(Das2ManagerSecurity.GUEST_ROLE));
				req.getSession().setAttribute(Das2ManagerSecurity.SESSION_KEY, das2ManagerSecurity);
			}
			

			// Handle the request
			if (req.getPathInfo() == null) {
				this.handleFlexRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.SECURITY_REQUEST)) {
				this.handleSecurityRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.DICTIONARIES_REQUEST)) {
				this.handleDictionaryRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATIONS_REQUEST)) {
				this.handleAnnotationsRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_REQUEST)) {
				this.handleAnnotationRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ORGANISM_ADD_REQUEST)) {
				this.handleOrganismAddRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ORGANISM_UPDATE_REQUEST)) {
				this.handleOrganismUpdateRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ORGANISM_DELETE_REQUEST)) {
				this.handleOrganismDeleteRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.GENOME_VERSION_REQUEST)) {
				this.handleGenomeVersionRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.GENOME_VERSION_ADD_REQUEST)) {
				this.handleGenomeVersionAddRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.GENOME_VERSION_UPDATE_REQUEST)) {
				this.handleGenomeVersionUpdateRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.GENOME_VERSION_DELETE_REQUEST)) {
				this.handleGenomeVersionDeleteRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.GENOME_VERSION_SEGMENT_IMPORT_REQUEST)) {
				this.handleGenomeVersionSegmentImportRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_GROUPING_ADD_REQUEST)) {
				this.handleAnnotationGroupingAddRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_GROUPING_UPDATE_REQUEST)) {
				this.handleAnnotationGroupingUpdateRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_GROUPING_MOVE_REQUEST)) {
				this.handleAnnotationGroupingMoveRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_GROUPING_DELETE_REQUEST)) {
				this.handleAnnotationGroupingDeleteRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_ADD_REQUEST)) {
				this.handleAnnotationAddRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_UPDATE_REQUEST)) {
				this.handleAnnotationUpdateRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_DELETE_REQUEST)) {
				this.handleAnnotationDeleteRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_UNLINK_REQUEST)) {
				this.handleAnnotationUnlinkRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_MOVE_REQUEST)) {
				this.handleAnnotationMoveRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.FORMULATE_UPLOAD_URL_REQUEST)) {
				this.handleFormulateUploadURLRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.UPLOAD_FILES_REQUEST)) {
				this.handleUploadRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.USERS_AND_GROUPS_REQUEST)) {
				this.handleUsersAndGroupsRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.USER_ADD_REQUEST)) {
				this.handleUserAddRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.USER_UPDATE_REQUEST)) {
				this.handleUserUpdateRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.USER_PASSWORD_REQUEST)) {
				this.handleUserPasswordRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.USER_DELETE_REQUEST)) {
				this.handleUserDeleteRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.GROUP_ADD_REQUEST)) {
				this.handleGroupAddRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.GROUP_UPDATE_REQUEST)) {
				this.handleGroupUpdateRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.GROUP_DELETE_REQUEST)) {
				this.handleGroupDeleteRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.DICTIONARY_ADD_REQUEST)) {
				this.handleDictionaryAddRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.DICTIONARY_UPDATE_REQUEST)) {
				this.handleDictionaryUpdateRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.DICTIONARY_DELETE_REQUEST)) {
				this.handleDictionaryDeleteRequest(req, res);
			} 

			res.setHeader("Cache-Control", "max-age=0, must-revalidate");
			
			return;

		} catch (Exception e) {
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
		}
	}

	private void handleFlexRequest(HttpServletRequest request, HttpServletResponse res) throws IOException {
		res.setContentType("text/html");
		res.getOutputStream().println(getFlexHTMLWrapper());
		res.setHeader("Cache-Control", "max-age=0, must-revalidate");
	}
	
	private void handleSecurityRequest(HttpServletRequest request, HttpServletResponse res) throws Exception{
		XMLWriter writer = new XMLWriter(res.getOutputStream(),
			                                 OutputFormat.createCompactFormat());
		writer.write(das2ManagerSecurity.getXML());

	}
	
	private void handleDictionaryRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = HibernateUtil.getSessionFactory().openSession();

		Document doc = DictionaryHelper.reload(sess)
		        .getXML(das2ManagerSecurity);

		XMLWriter writer = new XMLWriter(res.getOutputStream(), OutputFormat
		        .createCompactFormat());
		writer.write(doc);

		
	}
	
	@SuppressWarnings("unchecked")
	private void handleAnnotationsRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
    	Logger logger = Logger.getLogger("Hibernate");

    	Document doc = null;
		Session sess = HibernateUtil.getSessionFactory().openSession();
			
		AnnotationQuery annotationQuery = new AnnotationQuery(request);
		doc = annotationQuery.getAnnotationDocument(sess, das2ManagerSecurity);

		XMLWriter writer = new XMLWriter(res.getOutputStream(), OutputFormat.createCompactFormat());
		writer.write(doc);
	}
	
	
	private void handleAnnotationRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = HibernateUtil.getSessionFactory().openSession();

		if (request.getParameter("idAnnotation") == null || request.getParameter("idAnnotation").equals("")) {
			throw new Exception("idAnnotation request to get Annotation");
		}
		Integer idAnnotation = new Integer(request.getParameter("idAnnotation"));
		
		Annotation annotation = Annotation.class.cast(sess.load(Annotation.class, idAnnotation));
		Document doc = annotation.getXML(this.das2ManagerSecurity, DictionaryHelper.getInstance(sess), genometry_db_annotation_dir);

		XMLWriter writer = new XMLWriter(res.getOutputStream(), OutputFormat
		        .createCompactFormat());
		writer.write(doc);

		
	}
	
	private void handleOrganismAddRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			// Only admins can add organisms
			if (!this.das2ManagerSecurity.isAdminRole()) {
				throw new Exception("Insufficient permission to add organism.");
			}
			
			// Make sure that the required fields are filled in
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new Exception("Please enter an organism DAS2 name.");
			}
			if (request.getParameter("binomialName") == null || request.getParameter("binomialName").equals("")) {
				throw new Exception("Please enter an organism binomial name.");
			}
			if (request.getParameter("commonName") == null || request.getParameter("commonName").equals("")) {
				throw new Exception("Please enter an organism common name.");
			}
			
			// Make sure that the DAS2 name has no spaces or special characters
			if (request.getParameter("name").indexOf(" ") >= 0) {
				throw new Exception("The organism DAS2 name cannot have spaces.");
			}
		    Pattern pattern = Pattern.compile("\\W");
		    Matcher matcher = pattern.matcher(request.getParameter("name"));
			if (matcher.find()) {
				throw new Exception("The organism DAS2 name cannot have special characters.");
			}

			
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
			
			Organism organism = new Organism();
			
			organism.setName(request.getParameter("name"));
			organism.setCommonName(request.getParameter("commonName"));
			organism.setBinomialName(request.getParameter("binomialName"));
			
			sess.save(organism);
			
			tx.commit();
			
			DictionaryHelper.reload(sess);
			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idOrganism", organism.getIdOrganism().toString());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}
	
	
	private void handleOrganismUpdateRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
			
			Organism organism = Organism.class.cast(sess.load(Organism.class, Util.getIntegerParameter(request, "idOrganism")));

			// Check write permissions
			if (!this.das2ManagerSecurity.canWrite(organism)) {
				throw new Exception("Insufficient permission to update organism.");
			}
			
			
			// Make sure that the required fields are filled in
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new Exception("Please enter an organism DAS2 name.");
			}
			if (request.getParameter("binomialName") == null || request.getParameter("binomialName").equals("")) {
				throw new Exception("Please enter an organism binomial name.");
			}
			if (request.getParameter("commonName") == null || request.getParameter("commonName").equals("")) {
				throw new Exception("Please enter an organism common name.");
			}
			
			// Make sure that the DAS2 name has no spaces or special characters
			if (request.getParameter("name").indexOf(" ") >= 0) {
				throw new Exception("The organism DAS2 name cannot have spaces.");
			}
		    Pattern pattern = Pattern.compile("\\W");
		    Matcher matcher = pattern.matcher(request.getParameter("name"));
			if (matcher.find()) {
				throw new Exception("The organism DAS2 name cannot have special characters.");
			}

			
			organism.setName(request.getParameter("name"));
			organism.setCommonName(request.getParameter("commonName"));
			organism.setBinomialName(request.getParameter("binomialName"));
			organism.setNCBITaxID(request.getParameter("NCBITaxID"));
			
			sess.flush();
			
			tx.commit();
			
			DictionaryHelper.reload(sess);
			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idOrganism", organism.getIdOrganism().toString());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}

	
	private void handleOrganismDeleteRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
		
			
			Integer idOrganism = Util.getIntegerParameter(request, "idOrganism");
			Organism organism = Organism.class.cast(sess.load(Organism.class, idOrganism));
			

			// Check write permissions
			if (!this.das2ManagerSecurity.canWrite(organism)) {
				throw new Exception("Insufficient permission to update organism.");
			}

			
			sess.delete(organism);
			
			tx.commit();
			
			DictionaryHelper.reload(sess);
			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);

			if (tx != null) {
				tx.rollback();
			}

		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}

	@SuppressWarnings("unchecked")
	private void handleGenomeVersionRequest(HttpServletRequest request, HttpServletResponse res) {
		Session sess = null;
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();

			if (request.getParameter("idGenomeVersion") == null || request.getParameter("idGenomeVersion").equals("")) {
				throw new Exception("idGenomeVersion request to get Genome Version");
			}
			

			Integer idGenomeVersion = new Integer(request.getParameter("idGenomeVersion"));

			GenomeVersion gv = GenomeVersion.class.cast(sess.load(GenomeVersion.class, idGenomeVersion));
			
			Document doc = DocumentHelper.createDocument();
			Element versionNode = doc.addElement("GenomeVersion");
			
			versionNode.addAttribute("label", gv.getName());				
			versionNode.addAttribute("idGenomeVersion",gv.getIdGenomeVersion().toString());				
			versionNode.addAttribute("name",           gv.getName());				
			versionNode.addAttribute("buildDate",      gv.getBuildDate() != null ? Util.formatDate(gv.getBuildDate()) : "");				
			versionNode.addAttribute("idOrganism",     gv.getIdOrganism().toString());				
			versionNode.addAttribute("coordURI",       gv.getCoordURI() != null ? gv.getCoordURI().toString() : "");	
			versionNode.addAttribute("coordVersion",   gv.getCoordVersion() != null ? gv.getCoordVersion().toString() : "");	
			versionNode.addAttribute("coordSource",    gv.getCoordSource() != null ? gv.getCoordSource().toString() : "");	
			versionNode.addAttribute("coordTestRange", gv.getCoordTestRange() != null ? gv.getCoordTestRange().toString() : "");	
			versionNode.addAttribute("coordAuthority", gv.getCoordAuthority() != null ? gv.getCoordAuthority().toString() : "");
			
			Element segmentsNode = doc.getRootElement().addElement("Segments");
			for (Segment segment : (Set<Segment>)gv.getSegments()) {
				Element sNode = segmentsNode.addElement("Segment");
				sNode.addAttribute("idSegment", segment.getIdSegment().toString());
				sNode.addAttribute("name", segment.getName());
				
				sNode.addAttribute("length", segment.getLength() != null ? NumberFormat.getInstance().format(segment.getLength()) : "");
				sNode.addAttribute("sortOrder", segment.getSortOrder() != null ? segment.getSortOrder().toString() : "");
			}
			
			XMLWriter writer = new XMLWriter(res.getOutputStream(), OutputFormat.createCompactFormat());
			writer.write(doc);
			
		} catch (Exception e) {

			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			try {
				XMLWriter writer = new XMLWriter(res.getOutputStream(), OutputFormat.createCompactFormat());
				writer.write(doc);				
			} catch (Exception e1) {
				
			}

		} 
		finally {

			if (sess != null) {
				sess.close();

			}
		}
	}

	
	private void handleGenomeVersionAddRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			// Only admins can add genome versions
			if (!this.das2ManagerSecurity.isAdminRole()) {
				throw new Exception("Insufficient permissions to add genome version.");
			}
			
			
			// Make sure that the required fields are filled in
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new Exception("Please enter the genome version name.");
			}
			// Make sure that the DAS2 name has no spaces or special characters
			if (request.getParameter("name").indexOf(" ") >= 0) {
				throw new Exception("The genome version DAS2 name cannot have spaces.");
			}
		    Pattern pattern = Pattern.compile("\\W");
		    Matcher matcher = pattern.matcher(request.getParameter("name"));
			if (matcher.find()) {
				throw new Exception("The genome version DAS2 name cannot have special characters.");
			}
			
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
			
			GenomeVersion genomeVersion = new GenomeVersion();
			
			Integer idOrganism = Util.getIntegerParameter(request, "idOrganism");
			Organism organism = Organism.class.cast(sess.load(Organism.class, idOrganism));
			
			genomeVersion.setIdOrganism(idOrganism);
			genomeVersion.setName(request.getParameter("name"));
			genomeVersion.setBuildDate(Util.getDateParameter(request, "buildDate"));
			sess.save(genomeVersion);
			
			// Now add a root annotation grouping
			AnnotationGrouping annotationGrouping = new AnnotationGrouping();
			annotationGrouping.setName(genomeVersion.getName());
			annotationGrouping.setIdGenomeVersion(genomeVersion.getIdGenomeVersion());
			annotationGrouping.setIdParentAnnotationGrouping(null);
			sess.save(annotationGrouping);
			
			Set annotationGroupingsToKeep = new TreeSet<AnnotationGrouping>(new AnnotationGroupingComparator());
			annotationGroupingsToKeep.add(annotationGrouping);
			genomeVersion.setAnnotationGroupings(annotationGroupingsToKeep);

			
			tx.commit();
			
			DictionaryHelper.reload(sess);
			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idGenomeVersion", genomeVersion.getIdGenomeVersion().toString());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private void handleGenomeVersionUpdateRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
			
			GenomeVersion genomeVersion = GenomeVersion.class.cast(sess.load(GenomeVersion.class, Util.getIntegerParameter(request, "idGenomeVersion")));
			
			// Make sure the user can write this genome version
			if (!this.das2ManagerSecurity.canWrite(genomeVersion)) {
				throw new Exception("Insufficient permision to write genome version.");
			}
			
			
			
			// Make sure that the required fields are filled in
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new Exception("Please enter the genome version name.");
			}
			// Make sure that the DAS2 name has no spaces or special characters
			if (request.getParameter("name").indexOf(" ") >= 0) {
				throw new Exception("The genome version DAS2 name cannot have spaces.");
			}
		    Pattern pattern = Pattern.compile("\\W");
		    Matcher matcher = pattern.matcher(request.getParameter("name"));
			if (matcher.find()) {
				throw new Exception("The genome version DAS2 name cannot have special characters.");
			}
			
			
			genomeVersion.setIdOrganism(Util.getIntegerParameter(request, "idOrganism"));
			genomeVersion.setName(request.getParameter("name"));
			genomeVersion.setBuildDate(Util.getDateParameter(request, "buildDate"));
			genomeVersion.setCoordURI(request.getParameter("coordURI"));
			genomeVersion.setCoordVersion(request.getParameter("coordVersion"));
			genomeVersion.setCoordSource(request.getParameter("coordSource"));
			genomeVersion.setCoordTestRange(request.getParameter("coordTestRange"));
			genomeVersion.setCoordAuthority(request.getParameter("coordAuthority"));


			// Delete segments		
			StringReader reader = new StringReader(request.getParameter("segmentsXML"));
			SAXReader sax = new SAXReader();
			Document segmentsDoc = sax.read(reader);
			for (Segment segment : (Set<Segment>)genomeVersion.getSegments()) {
				boolean found = false;
				for(Iterator i = segmentsDoc.getRootElement().elementIterator(); i.hasNext();) {
					Element segmentNode = (Element)i.next();
					String idSegment = segmentNode.attributeValue("idSegment");
					if (idSegment != null && !idSegment.equals("")) {
						if (segment.getIdSegment().equals(new Integer(idSegment))) {
							found = true;
							break;
						}
					}										
				}
				if (!found) {
					sess.delete(segment);
				}
			} 
			sess.flush();
			
			// Add segments
			for(Iterator i = segmentsDoc.getRootElement().elementIterator(); i.hasNext();) {
				Element segmentNode = (Element)i.next();
				
				String idSegment = segmentNode.attributeValue("idSegment");
				String len = segmentNode.attributeValue("length");
				len = len.replace(",", "");
				String sortOrder = segmentNode.attributeValue("sortOrder");
				
				Segment s = null;
				if (idSegment != null && !idSegment.equals("")) {
					s = Segment.class.cast(sess.load(Segment.class, new Integer(idSegment)));
					
					s.setName(segmentNode.attributeValue("name"));
					s.setLength(len != null && !len.equals("") ? new Integer(len) : null);
					s.setSortOrder(sortOrder != null && !sortOrder.equals("") ? new Integer(sortOrder) : null);
					s.setIdGenomeVersion(genomeVersion.getIdGenomeVersion());
					

				} else {
					s = new Segment();		

					s.setName(segmentNode.attributeValue("name"));
					s.setLength(len != null && !len.equals("") ? new Integer(len) : null);
					s.setSortOrder(sortOrder != null && !sortOrder.equals("") ? new Integer(sortOrder) : null);
					s.setIdGenomeVersion(genomeVersion.getIdGenomeVersion());
					
					sess.save(s);
					sess.flush();
				}
				
			}    
			sess.flush();
			

			
			
			tx.commit();
			
			DictionaryHelper.reload(sess);
			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idGenomeVersion", genomeVersion.getIdGenomeVersion().toString());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}

	
	private void handleGenomeVersionDeleteRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			// Find the genome version
			Integer idGenomeVersion = Util.getIntegerParameter(request, "idGenomeVersion");
			GenomeVersion genomeVersion = GenomeVersion.class.cast(sess.load(GenomeVersion.class, idGenomeVersion));
			
			// Make sure the user can write this genome version
			if (!this.das2ManagerSecurity.canWrite(genomeVersion)) {
				throw new Exception("Insufficient permision to delete genome version.");
			}
			
			// Delete the root annotation grouping
			AnnotationGrouping ag = genomeVersion.getRootAnnotationGrouping();
			if (ag != null) {
				// Make sure the root annotation grouping has no children
				if (ag.getAnnotationGroupings().size() > 0 || ag.getAnnotations().size() > 0) {
					throw new Exception("The annotations for" + genomeVersion.getName() + " must be deleted first.");
				}
				sess.delete(ag);
			}
			
			
			// Now delete the genome version
			sess.delete(genomeVersion);
			
			tx.commit();
			
			DictionaryHelper.reload(sess);
			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}

	
	private void handleGenomeVersionSegmentImportRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			Transaction tx = sess.beginTransaction();
			
			
			String chromosomeInfo = request.getParameter("chromosomeInfo");
			String line;
			int count = 1;
			if (chromosomeInfo != null && !chromosomeInfo.equals("")) {
				Integer idGenomeVersion = Util.getIntegerParameter(request, "idGenomeVersion");
				GenomeVersion genomeVersion = GenomeVersion.class.cast(sess.load(GenomeVersion.class, idGenomeVersion));
				
				BufferedReader reader = new BufferedReader(new StringReader(chromosomeInfo));
				while ((line = reader.readLine()) != null) {	
					if ( (line.length() == 0) || line.equals("") || line.startsWith("#"))  { 
						continue; 
					}
					String[] tokens = line.split("\t");
					String name = tokens[0];
					String len = tokens[1];
					
					Segment s = new Segment();		

					s.setName(name);
					s.setLength(len != null && !len.equals("") ? new Integer(len) : null);
					s.setSortOrder(new Integer(count));
					s.setIdGenomeVersion(genomeVersion.getIdGenomeVersion());
					
					sess.save(s);

					count++;
				}
				sess.flush();
			}
			
			
			tx.commit();
			
			DictionaryHelper.reload(sess);
			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idGenomeVersion", request.getParameter("idGenomeVersion"));
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}

	private void handleAnnotationGroupingAddRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		
		try {
			
			
			// Make sure that the required fields are filled in
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new Exception("Please enter the annotation folder name.");
			}
			
			sess = HibernateUtil.getSessionFactory().openSession();
			Transaction tx = sess.beginTransaction();
			
			AnnotationGrouping annotationGrouping = new AnnotationGrouping();
			
			Integer idGenomeVersion = Util.getIntegerParameter(request, "idGenomeVersion");
			Integer idParentAnnotationGrouping = Util.getIntegerParameter(request, "idParentAnnotationGrouping");
			
			// If this is a root annotation grouping, find the default root annotation
	        // grouping for the genome version.
			if (idParentAnnotationGrouping == null) {
				GenomeVersion gv = GenomeVersion.class.cast(sess.load(GenomeVersion.class, idGenomeVersion));
				AnnotationGrouping rootAnnotationGrouping = gv.getRootAnnotationGrouping();
				if (rootAnnotationGrouping == null) {
					throw new Exception("Cannot find root annotation grouping for " + gv.getName());
				}
				idParentAnnotationGrouping = rootAnnotationGrouping.getIdAnnotationGrouping(); 
			}
			
			annotationGrouping.setName(request.getParameter("name"));
			annotationGrouping.setIdGenomeVersion(idGenomeVersion);
			annotationGrouping.setIdParentAnnotationGrouping(idParentAnnotationGrouping);
			annotationGrouping.setIdUserGroup(Util.getIntegerParameter(request, "idUserGroup"));				
			
			
			sess.save(annotationGrouping);
			
			tx.commit();
			
			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idAnnotationGrouping", annotationGrouping.getIdAnnotationGrouping().toString());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}
	
	private void handleAnnotationGroupingUpdateRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			// Make sure that the required fields are filled in
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new Exception("Please enter the annotation folder name.");
			}

			
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
			
			AnnotationGrouping annotationGrouping = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, Util.getIntegerParameter(request, "idAnnotationGrouping")));
			
			
			// Make sure the user can write this annotation grouping
			if (!this.das2ManagerSecurity.canWrite(annotationGrouping)) {
				throw new Exception("Insufficient permision to write annotation folder.");
			}
			
			annotationGrouping.setName(request.getParameter("name"));
			annotationGrouping.setDescription(request.getParameter("description"));
			annotationGrouping.setIdUserGroup(Util.getIntegerParameter(request, "idUserGroup"));
			
			sess.save(annotationGrouping);
			
			tx.commit();
			
		
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idAnnotationGrouping", annotationGrouping.getIdAnnotationGrouping().toString());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();				
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}
	
	private void handleAnnotationGroupingMoveRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
			
			Integer idAnnotationGrouping = Util.getIntegerParameter(request, "idAnnotationGrouping");
			Integer idGenomeVersion = Util.getIntegerParameter(request, "idGenomeVersion");
			Integer idParentAnnotationGrouping = Util.getIntegerParameter(request, "idParentAnnotationGrouping");
			String  isMove = Util.getFlagParameter(request, "isMove");

			AnnotationGrouping annotationGrouping = null;
			GenomeVersion gv = GenomeVersion.class.cast(sess.load(GenomeVersion.class, idGenomeVersion));

			// Get the annotation grouping this annotation grouping should be moved to.
			AnnotationGrouping parentAnnotationGrouping = null;
			if (idParentAnnotationGrouping == null) {
				// If this is a root annotation, find the default root annotation
				// grouping for the genome version.
				parentAnnotationGrouping = gv.getRootAnnotationGrouping();
				if (parentAnnotationGrouping == null) {
					throw new Exception("Cannot find root annotation grouping for " + gv.getName());
				}
			} else {
				// Otherwise, find the annotation grouping passed in as a request parameter.
				parentAnnotationGrouping = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, idParentAnnotationGrouping));
			}
			

			
		
			
			// If this is a copy instead of a move,
			// clone the annotation grouping, leaving the existing one as-is.
			if (isMove.equals("Y")) {
				annotationGrouping = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, idAnnotationGrouping));
				
				// Make sure the user can write this annotation grouping
				if (!this.das2ManagerSecurity.canWrite(annotationGrouping)) {
					throw new Exception("Insufficient permision to move this annotation folder.");
				}
			} else {
				AnnotationGrouping ag = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, idAnnotationGrouping));
				annotationGrouping = new AnnotationGrouping();
				annotationGrouping.setName(ag.getName());
				annotationGrouping.setDescription(ag.getDescription());
				annotationGrouping.setIdGenomeVersion(ag.getIdGenomeVersion());
				annotationGrouping.setIdUserGroup(ag.getIdUserGroup());				
				
				Set annotationsToKeep = new TreeSet<Annotation>(new AnnotationComparator());
				for(Annotation a : (Set<Annotation>)ag.getAnnotations()) {
					annotationsToKeep.add(a);
				}
				annotationGrouping.setAnnotations(annotationsToKeep);
				sess.save(annotationGrouping);
			}

			// The move/copy is disallowed if the parent annotation grouping belongs to a 
			// different genome version
			if (!parentAnnotationGrouping.getIdGenomeVersion().equals(annotationGrouping.getIdGenomeVersion())) {
				throw new Exception("Annotation grouping '" + annotationGrouping.getName() + 
						"' cannot be moved to a different genome version");
			}
	
			
			// Set the parent annotation grouping
			annotationGrouping.setIdParentAnnotationGrouping(parentAnnotationGrouping.getIdAnnotationGrouping());
			
			
			tx.commit();

			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idAnnotationGrouping", annotationGrouping.getIdAnnotationGrouping().toString());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}

	
	
	private void handleAnnotationGroupingDeleteRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
			
			Integer idAnnotationGrouping = Util.getIntegerParameter(request, "idAnnotationGrouping");
			AnnotationGrouping annotationGrouping = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, idAnnotationGrouping));
			
			
			// Make sure the user can write this annotation grouping
			if (!this.das2ManagerSecurity.canWrite(annotationGrouping)) {
				throw new Exception("Insufficient permision to delete this annotation folder.");
			}
			
			sess.delete(annotationGrouping);
			
			tx.commit();
			
			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}

	
	@SuppressWarnings("unchecked")
	private void handleAnnotationAddRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		
		try {

			// Make sure that the required fields are filled in
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new Exception("Please enter an annotation name.");
			}
			if (request.getParameter("codeVisibility") == null || request.getParameter("codeVisibility").equals("")) {
				throw new Exception("Please select the visibility for this annotation.");
			}
			if (!request.getParameter("codeVisibility").equals(Visibility.PUBLIC)) {
				if (Util.getIntegerParameter(request, "idUserGroup") == null) {
					throw new Exception("For private annotations, the group must be specified.");
				}
			}

			sess = HibernateUtil.getSessionFactory().openSession();
			Transaction tx = sess.beginTransaction();
			
			Annotation annotation = new Annotation();
			
			Integer idGenomeVersion = Util.getIntegerParameter(request, "idGenomeVersion");
			Integer idAnnotationGrouping = Util.getIntegerParameter(request, "idAnnotationGrouping");
			
			annotation.setName(request.getParameter("name"));
			annotation.setIdGenomeVersion(idGenomeVersion);
			annotation.setCodeVisibility(request.getParameter("codeVisibility"));
			annotation.setIdUserGroup(Util.getIntegerParameter(request, "idUserGroup"));
			// Only set ownership if this is not an admin
			if (!das2ManagerSecurity.isAdminRole()) {
				annotation.setIdUser(das2ManagerSecurity.getIdUser());				
			}
			sess.save(annotation);
			sess.flush();

			// Get the annotation grouping this annotation is in.
			AnnotationGrouping ag = null;
			if (idAnnotationGrouping == null) {
				// If this is a root annotation, find the default root annotation
				// grouping for the genome version.
				GenomeVersion gv = GenomeVersion.class.cast(sess.load(GenomeVersion.class, idGenomeVersion));
				ag = gv.getRootAnnotationGrouping();
				if (ag == null) {
					throw new Exception("Cannot find root annotation grouping for " + gv.getName());
				}
			} else {
				// Otherwise, find the annotation grouping passed in as a request parameter.
				ag = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, idAnnotationGrouping));
			}

			// Add the annotation to the annotation grouping
			Set newAnnotations = new TreeSet<Annotation>(new AnnotationComparator());
			for(Annotation a : (Set<Annotation>)ag.getAnnotations()) {
				newAnnotations.add(a);
			}
			newAnnotations.add(annotation);
			ag.setAnnotations(newAnnotations);
			
			
			// Assign a file directory name
			annotation.setFileName("A" + annotation.getIdAnnotation());
			
			
			sess.flush();
			
			
			
			tx.commit();

			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idAnnotation", annotation.getIdAnnotation().toString());
			root.addAttribute("idGenomeVersion", idGenomeVersion.toString());
			root.addAttribute("idAnnotationGrouping", idAnnotationGrouping != null ? idAnnotationGrouping.toString() : "");
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private void handleAnnotationUpdateRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
			
			Annotation annotation = Annotation.class.cast(sess.load(Annotation.class, Util.getIntegerParameter(request, "idAnnotation")));
			
			// Make sure the user can write this annotation 
			if (!this.das2ManagerSecurity.canWrite(annotation)) {
				throw new Exception("Insufficient permision to write annotation.");
			}
			
			// Make sure that the required fields are filled in
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new Exception("Please enter an annotation name.");
			}
			if (request.getParameter("codeVisibility") == null || request.getParameter("codeVisibility").equals("")) {
				throw new Exception("Please select the visibility for this annotation.");
			}
			if (!request.getParameter("codeVisibility").equals(Visibility.PUBLIC)) {
				if (Util.getIntegerParameter(request, "idUserGroup") == null) {
					throw new Exception("For private annotations, the group must be specified.");
				}
			}
			
			annotation.setName(request.getParameter("name"));
			annotation.setDescription(request.getParameter("description"));
			annotation.setSummary(request.getParameter("summary"));
			annotation.setIdAnalysisType(Util.getIntegerParameter(request, "idAnalysisType"));
			annotation.setIdExperimentPlatform(Util.getIntegerParameter(request, "idExperimentPlatform"));
			annotation.setIdExperimentMethod(Util.getIntegerParameter(request, "idExperimentMethod"));
			annotation.setCodeVisibility(request.getParameter("codeVisibility"));
			annotation.setIdUserGroup(Util.getIntegerParameter(request, "idUserGroup"));
			annotation.setIdUser(Util.getIntegerParameter(request, "idUser"));
			
			// Remove annotation files
			StringReader reader = new StringReader(request.getParameter("filesToRemoveXML"));
			SAXReader sax = new SAXReader();
			Document filesDoc = sax.read(reader);
			for(Iterator i = filesDoc.getRootElement().elementIterator(); i.hasNext();) {
				Element fileNode = (Element)i.next();
				File file = new File(fileNode.attributeValue("url"));
				file.delete();
			}            
			
			sess.save(annotation);
			
			tx.commit();
			
			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idAnnotation", annotation.getIdAnnotation().toString());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}
	
	
	private void handleAnnotationDeleteRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
			
			Integer idAnnotation = Util.getIntegerParameter(request, "idAnnotation");
			Annotation annotation = Annotation.class.cast(sess.load(Annotation.class, idAnnotation));

			// Make sure the user can write this annotation 
			if (!this.das2ManagerSecurity.canWrite(annotation)) {
				throw new Exception("Insufficient permision to delete annotation.");
			}
			
			
			// remove annotation files
			annotation.removeFiles(genometry_db_annotation_dir);
			
			// delete database object
			sess.delete(annotation);
			
			tx.commit();

			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}

	private void handleAnnotationUnlinkRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
			
			Integer idAnnotation = Util.getIntegerParameter(request, "idAnnotation");
			Integer idGenomeVersion = Util.getIntegerParameter(request, "idGenomeVersion");
			Integer idAnnotationGrouping = Util.getIntegerParameter(request, "idAnnotationGrouping");
			
			Annotation annotation = Annotation.class.cast(sess.load(Annotation.class, idAnnotation));
			GenomeVersion gv = GenomeVersion.class.cast(sess.load(GenomeVersion.class, idGenomeVersion));

			// Make sure the user can write this annotation 
			if (!this.das2ManagerSecurity.canWrite(annotation)) {
				throw new Exception("Insufficient permision to unlink annotation.");
			}
			
			
			// Get the annotation grouping this annotation should be removed from.
			AnnotationGrouping annotationGrouping = null;
			if (idAnnotationGrouping == null) {
				// If this is a root annotation, find the default root annotation
				// grouping for the genome version.
				annotationGrouping = gv.getRootAnnotationGrouping();
				if (annotationGrouping == null) {
					throw new Exception("Cannot find root annotation grouping for " + gv.getName());
				}
			} else {
				// Otherwise, find the annotation grouping passed in as a request parameter.
				annotationGrouping = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, idAnnotationGrouping));
			}

			// Remove the annotation grouping the annotation was in
			// by adding back the annotations to the annotation grouping, 
			// excluding the annotation to be removed
			Set annotationsToKeep = new TreeSet<Annotation>(new AnnotationComparator());
			for(Annotation a : (Set<Annotation>)annotationGrouping.getAnnotations()) {
				if (a.getIdAnnotation().equals(annotation.getIdAnnotation())) {
					continue;
				}
				annotationsToKeep.add(a);
				
			}
			annotationGrouping.setAnnotations(annotationsToKeep);

			
			
			tx.commit();
			
			// Send back XML attributes showing remaining references to annotation groupings
			sess.refresh(annotation);
			StringBuffer remainingAnnotationGroupings = new StringBuffer();
			int agCount = 0;
			for (AnnotationGrouping ag : (Set<AnnotationGrouping>)annotation.getAnnotationGroupings()) {
				if (remainingAnnotationGroupings.length() > 0) {
					remainingAnnotationGroupings.append(",\n");					
				}
				remainingAnnotationGroupings.append("    '" + ag.getName() + "'");
				agCount++;
				
			}

			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idAnnotation", annotation.getIdAnnotation().toString());
			root.addAttribute("name", annotation.getName());
			root.addAttribute("numberRemainingAnnotationGroupings", new Integer(agCount).toString());
			root.addAttribute("remainingAnnotationGroupings", remainingAnnotationGroupings.toString());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}

	private void handleAnnotationMoveRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
			
			Integer idAnnotation = Util.getIntegerParameter(request, "idAnnotation");
			Integer idGenomeVersion = Util.getIntegerParameter(request, "idGenomeVersion");
			Integer idAnnotationGrouping = Util.getIntegerParameter(request, "idAnnotationGrouping");
			Integer idAnnotationGroupingOld = Util.getIntegerParameter(request, "idAnnotationGroupingOld");
			String  isMove = Util.getFlagParameter(request, "isMove");

			Annotation annotation = Annotation.class.cast(sess.load(Annotation.class, idAnnotation));
			GenomeVersion gv = GenomeVersion.class.cast(sess.load(GenomeVersion.class, idGenomeVersion));

			// Make sure the user can write this annotation 
			if (isMove.equals("Y")) {
				if (!this.das2ManagerSecurity.canWrite(annotation)) {
					throw new Exception("Insufficient permision to unlink annotation.");
				}
			}
			
			// Get the annotation grouping this annotation should be moved to.
			AnnotationGrouping annotationGroupingNew = null;
			if (idAnnotationGrouping == null) {
				// If this is a root annotation, find the default root annotation
				// grouping for the genome version.
				annotationGroupingNew = gv.getRootAnnotationGrouping();
				if (annotationGroupingNew == null) {
					throw new Exception("Cannot find root annotation grouping for " + gv.getName());
				}
			} else {
				// Otherwise, find the annotation grouping passed in as a request parameter.
				annotationGroupingNew = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, idAnnotationGrouping));
			}
			
			

			// The move/copy is disallowed if the parent annotation grouping belongs to a 
			// different genome version
			if (!annotationGroupingNew.getIdGenomeVersion().equals(annotation.getIdGenomeVersion())) {
				throw new Exception("Annotation '" + annotation.getName() + 
						"' cannot be moved to a different genome version");
			}
	
			
			//
			// Add the annotation to the annotation grouping
			//
			Set newAnnotations = new TreeSet<Annotation>(new AnnotationComparator());
			for(Annotation a : (Set<Annotation>)annotationGroupingNew.getAnnotations()) {
				newAnnotations.add(a);
			}
			newAnnotations.add(annotation);
			annotationGroupingNew.setAnnotations(newAnnotations);
			
			
		
			// If this is a move instead of a copy,
			// get the annotation grouping this annotation should be removed from.
			if (isMove.equals("Y")) {
				AnnotationGrouping annotationGroupingOld = null;
				if (idAnnotationGroupingOld == null) {
					// If this is a root annotation, find the default root annotation
					// grouping for the genome version.
					annotationGroupingOld = gv.getRootAnnotationGrouping();
					if (annotationGroupingOld == null) {
						throw new Exception("Cannot find root annotation grouping for " + gv.getName());
					}
				} else {
					// Otherwise, find the annotation grouping passed in as a request parameter.
					annotationGroupingOld = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, idAnnotationGroupingOld));
				}

				//
				// Remove the annotation grouping the annotation was in
				// by adding back the annotations to the annotation grouping, 
				// excluding the annotation that has moved
				Set annotationsToKeep = new TreeSet<Annotation>(new AnnotationComparator());
				for(Annotation a : (Set<Annotation>)annotationGroupingOld.getAnnotations()) {
					if (a.getIdAnnotation().equals(annotation.getIdAnnotation())) {
						continue;
					}
					annotationsToKeep.add(a);
				}
				annotationGroupingOld.setAnnotations(annotationsToKeep);

			}

		
			
			tx.commit();

			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idAnnotation", annotation.getIdAnnotation().toString());
			root.addAttribute("idGenomeVersion", idGenomeVersion.toString());
			root.addAttribute("idAnnotationGrouping", idAnnotationGrouping != null ? idAnnotationGrouping.toString() : "");
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}

	
	private void handleFormulateUploadURLRequest(HttpServletRequest req, HttpServletResponse res) {
	    try {
	        
	        //
	        // COMMENTED OUT CODE: 
	        //    String baseURL =  "http"+ (isLocalHost ? "://" : "s://") + req.getServerName() + req.getContextPath();
	        //
	        // To fix upload problem (missing session in upload servlet for FireFox, Safari), encode session in URL
	        // for upload servlet.  Also, use non-secure (http: rather than https:) when making http request; 
	        // otherwise, existing session is not accessible to upload servlet.
	        //
	        //
	        
	        String baseURL =  "http"+  "://"  + req.getServerName() + ":" + req.getLocalPort() + req.getContextPath();
	        String URL = baseURL + "/manager/" +  this.UPLOAD_FILES_REQUEST;
	        // Encode session id in URL so that session maintains for upload servlet when called from
	        // Flex upload component inside FireFox, Safari
	        URL += ";jsessionid=" + req.getRequestedSessionId();
	        
	        
	        res.setContentType("application/xml");
	        res.getOutputStream().println("<UploadURL url='" + URL + "'/>");
	        
	      } catch (Exception e) {
	        System.out.println("An error has occured in UploadURLServlet - " + e.toString());
	      }		
	}

	
	private void handleUploadRequest(HttpServletRequest req, HttpServletResponse res) {
		
		Session sess = null;
		Integer idAnnotation = null;
	    Annotation annotation = null;
	    String fileName = null;
	    Transaction tx = null;
	    
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
			
			PrintWriter out = res.getWriter();
    	    res.setHeader("Cache-Control", "max-age=0, must-revalidate");
    	            
    	    MultipartParser mp = new MultipartParser(req, Integer.MAX_VALUE); 
    	    Part part;
    	    while ((part = mp.readNextPart()) != null) {
    	      String name = part.getName();
    	      if (part.isParam()) {
    	        // it's a parameter part
    	        ParamPart paramPart = (ParamPart) part;
    	        String value = paramPart.getStringValue();
    	        if (name.equals("idAnnotation")) {
    	            idAnnotation = new Integer(String.class.cast(value));
    	            break;
    	          }
    	        } 
    	      }
    	      
    	      if (idAnnotation != null) {
    	        
    	        annotation = (Annotation)sess.get(Annotation.class, idAnnotation);
    	        if (this.das2ManagerSecurity.canWrite(annotation)) {
    	          SimpleDateFormat formatter = new SimpleDateFormat("yyyy");
    	          
    
    	          String annotationFileDir = annotation.getDirectory(genometry_db_annotation_dir);
    	          
    	          // Create annotation directory if it doesn't exist
    	          if (!new File(annotationFileDir).exists()) {
    	              boolean success = (new File(annotationFileDir)).mkdir();
    	              if (!success) {
    	                System.out.println("Unable to create directory " + annotationFileDir);      
    	              }      
    	          }
    	          
    	          while ((part = mp.readNextPart()) != null) {        
    	            if (part.isFile()) {
    	              // it's a file part
    	              FilePart filePart = (FilePart) part;
    	              fileName = filePart.getFileName();
    	              if (fileName != null) {
    	                // the part actually contained a file
    	                long size = filePart.writeTo(new File(annotationFileDir));
    	              }
    	              else { 
    	              }
    	              out.flush();
    	            }
    	          }
    	          sess.flush();
    	        } else {
    	        	System.out.println("Bypassing upload of annotation " + annotation.getName() + " due to insufficient permissions.");
    	        }
    	      }
    	      
    	      tx.commit();
    	      
		} catch (Exception e) {
			if (tx != null) {
				tx.rollback();
			}
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			try {
				XMLWriter writer = new XMLWriter(res.getOutputStream(), OutputFormat.createCompactFormat());
				writer.write(doc);				
			} catch (Exception e1) {
				
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
	}
	

	@SuppressWarnings("unchecked")
	private void handleUsersAndGroupsRequest(HttpServletRequest request, HttpServletResponse res) {
		Session sess = null;

		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("UsersAndGroups");
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			
			// Get group members
			StringBuffer query = new StringBuffer();
			query.append("SELECT      gr, ");
			query.append("            mem   ");
			query.append("FROM        UserGroup as gr   ");
			query.append("LEFT JOIN   gr.members as mem ");
			query.append("ORDER BY    gr.name, mem.lastName, mem.firstName ");
			
			List<Object[]> rows = (List<Object[]>)sess.createQuery(query.toString()).list();

			String groupNamePrev = "";
			Element groupNode = null;
			Element membersNode = null;
			Element collabsNode = null;
			Element managersNode = null;
			Element userNode = null;
			HashMap<Integer, Element> groupNodeMap = new HashMap<Integer, Element>();
		
			for (Object[] row : rows) {
				UserGroup group = (UserGroup)row[0];
				User user = (User)row[1];
				
				// Only show groups this user managers
				if (!this.das2ManagerSecurity.isManager(group)) {
					continue;
				}
				
				if (!group.getName().equals(groupNamePrev)) {
					groupNode = doc.getRootElement().addElement("UserGroup");
					groupNode.addAttribute("label", group.getName());
					groupNode.addAttribute("name", group.getName());					
					groupNode.addAttribute("idUserGroup", group.getIdUserGroup().toString());
					groupNode.addAttribute("canWrite", this.das2ManagerSecurity.canWrite(group) ? "Y" : "N");
					groupNodeMap.put(group.getIdUserGroup(), groupNode);					
					membersNode = null;
				}
				
				if (user != null) {
					if (membersNode == null) {
						membersNode = groupNode.addElement("members");
					}
					userNode = membersNode.addElement("User");
					userNode.addAttribute("label", user.getLastName() + ", " + user.getFirstName());
					userNode.addAttribute("name",  user.getLastName() + ", " + user.getFirstName());
					userNode.addAttribute("idUser", user.getIdUser().toString());
					userNode.addAttribute("type", "Member");					
				}
				
				
				groupNamePrev = group.getName();
			}

			// Get group collaborators
			query = new StringBuffer();
			query.append("SELECT      gr, ");
			query.append("            col   ");
			query.append("FROM        UserGroup as gr   ");
			query.append("JOIN   gr.collaborators as col ");
			query.append("ORDER BY    gr.name, col.lastName, col.firstName ");
			
			rows = (List<Object[]>)sess.createQuery(query.toString()).list();
			for (Object[] row : rows) {
				UserGroup group = (UserGroup)row[0];
				User user = (User)row[1];
				
				// Only show groups this user managers
				if (!this.das2ManagerSecurity.isManager(group)) {
					continue;
				}
				
				groupNode = groupNodeMap.get(group.getIdUserGroup());
				
				collabsNode = groupNode.element("collaborators");				
				if (collabsNode == null) {
					collabsNode = groupNode.addElement("collaborators");
				}
				userNode = collabsNode.addElement("User");
				userNode.addAttribute("label", user.getLastName() + ", " + user.getFirstName());
				userNode.addAttribute("name",  user.getLastName() + ", " + user.getFirstName());
				userNode.addAttribute("idUser", user.getIdUser().toString());
				userNode.addAttribute("type", "Collaborator");					
			}
			
			// Get group managers
			query = new StringBuffer();
			query.append("SELECT      gr, ");
			query.append("            mgr   ");
			query.append("FROM        UserGroup as gr   ");
			query.append("JOIN   gr.managers as mgr ");
			query.append("ORDER BY    gr.name, mgr.lastName, mgr.firstName ");
			
			rows = (List<Object[]>)sess.createQuery(query.toString()).list();
			for (Object[] row : rows) {
				UserGroup group = (UserGroup)row[0];
				User user = (User)row[1];
				groupNode = groupNodeMap.get(group.getIdUserGroup());
				
				// Only show groups this user managers
				if (!this.das2ManagerSecurity.isManager(group)) {
					continue;
				}
				
				managersNode = groupNode.element("managers");				
				if (managersNode == null) {
					managersNode = groupNode.addElement("managers");
				}
				
				userNode = managersNode.addElement("User");
				userNode.addAttribute("label", user.getName());
				userNode.addAttribute("name",  user.getName());
				userNode.addAttribute("idUser", user.getIdUser().toString());
				userNode.addAttribute("type", "Manager");					
			}
			
			// Get All Users
			query = new StringBuffer();
			query.append("SELECT      user ");
			query.append("FROM        User as user   ");
			query.append("ORDER BY    user.lastName, user.firstName ");
			
			List<User> users = (List<User>)sess.createQuery(query.toString()).list();
			for (User user : users) {
				userNode = doc.getRootElement().addElement("User");
				userNode.addAttribute("label", user.getName());
				userNode.addAttribute("name",  user.getName());
				userNode.addAttribute("idUser", user.getIdUser().toString());
				userNode.addAttribute("firstName",  user.getFirstName() != null ? user.getFirstName() : "");
				userNode.addAttribute("lastName",  user.getLastName() != null ? user.getLastName() : "");
				userNode.addAttribute("middleName",  user.getMiddleName() != null ? user.getMiddleName() : "");
				userNode.addAttribute("userName",  user.getUserName() != null ? user.getUserName() : "");
				userNode.addAttribute("canWrite", this.das2ManagerSecurity.canWrite(user) ? "Y" : "N");
				
				if (this.das2ManagerSecurity.canWrite(user)) {
					userNode.addAttribute("passwordDisplay",  user.getPasswordDisplay() != null ? user.getPasswordDisplay() : "");

					for(UserRole role : (Set<UserRole>)user.getRoles()) {
						userNode.addAttribute("role", role.getRoleName());
					}
					
					StringBuffer memberGroups = new StringBuffer();
					for(UserGroup memberGroup : (Set<UserGroup>)user.getMemberUserGroups()) {
						if (memberGroups.length() > 0) {
							memberGroups.append(", ");
						}
						memberGroups.append(memberGroup.getName());
					}
					userNode.addAttribute("memberGroups", memberGroups.length() > 0 ? memberGroups.toString() : "(none)");
					
					StringBuffer collaboratorGroups = new StringBuffer();
					Element collaboratorGroupNode = userNode.addElement("collaborators");
					for(UserGroup colGroup : (Set<UserGroup>)user.getCollaboratingUserGroups()) {
						if (collaboratorGroups.length() > 0) {
							collaboratorGroups.append(", ");
						}
						collaboratorGroups.append(colGroup.getName());
					}
					userNode.addAttribute("collaboratorGroups", collaboratorGroups.length() > 0 ? collaboratorGroups.toString() : "(none)");
					
					StringBuffer managerGroups = new StringBuffer();
					for(UserGroup mgrGroup : (Set<UserGroup>)user.getManagingUserGroups()) {
						if (managerGroups.length() > 0) {
							managerGroups.append(", ");
						}
						managerGroups.append(mgrGroup.getName());
					}
					userNode.addAttribute("managerGroups", managerGroups.length() > 0 ? managerGroups.toString() : "(none)");
					
				}
			}
			
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
		} catch (Exception e) {
			
			e.printStackTrace();
			doc = DocumentHelper.createDocument();
			root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			try {
				XMLWriter writer = new XMLWriter(res.getOutputStream(),
			    OutputFormat.createCompactFormat());
				writer.write(doc);
				
			} catch (Exception e1) {				
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}
	

	
	@SuppressWarnings("unchecked")
	private void handleUserAddRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		// Only admins can add users
		if (!this.das2ManagerSecurity.isAdminRole()) {
			throw new Exception("Insufficient permissions to add users.");
		}
		
		// Make sure that the required fields are filled in
		if ((request.getParameter("firstName") == null || request.getParameter("firstName").equals("")) &&
		    (request.getParameter("lastName") == null || request.getParameter("lastName").equals(""))) {
			throw new Exception("Please enter first or last name.");
		}
		if (request.getParameter("userName") == null || request.getParameter("userName").equals("")) {
			throw new Exception("Please enter the user name.");
		}
		
		
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			// Make sure this user name doesn't exist
			List users = sess.createQuery("SELECT u.userName from User u where u.userName = '" + request.getParameter("userName") + "'").list();
			if (users.size() > 0) {
				throw new Exception("The user name " + request.getParameter("userName") + " is already taken.  Please enter a unique user name.");
			}
	
			User user = new User();
			
			user.setFirstName(request.getParameter("firstName"));
			user.setMiddleName(request.getParameter("middleName"));
			user.setLastName(request.getParameter("lastName"));
			user.setUserName(request.getParameter("userName"));
			
			sess.save(user);
			
			sess.flush();

			// Default user to das2user role
			TreeSet roles = new TreeSet();
			UserRole role = new UserRole();
			role.setRoleName(Das2ManagerSecurity.USER_ROLE);
			role.setUserName(user.getUserName());
			role.setIdUser(user.getIdUser());
			sess.save(role);
			sess.flush();

						
			tx.commit();
			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idUser", user.getIdUser().toString());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}
	

	@SuppressWarnings("unchecked")
	private void handleUserDeleteRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx =  sess.beginTransaction();
			
			User user = User.class.cast(sess.load(User.class, Util.getIntegerParameter(request, "idUser")));

			// Check write permissions
			if (!this.das2ManagerSecurity.canWrite(user)) {
				throw new Exception("Insufficient permissions to delete user.");
			}
			
			for (UserRole role : (Set<UserRole>)user.getRoles()) {
				sess.delete(role);
			}
			sess.flush();
			
			sess.refresh(user);
			
	
			
			sess.delete(user);
			
			sess.flush();
			
			tx.commit();

			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}


	
	@SuppressWarnings("unchecked")
	private void handleUserUpdateRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
			
			User user = User.class.cast(sess.load(User.class, Util.getIntegerParameter(request, "idUser")));
			
			// Check write permissions
			if (!this.das2ManagerSecurity.canWrite(user)) {
				throw new Exception("Insufficient permissions to write user.");
			}

			// Make sure that the required fields are filled in
			if ((request.getParameter("firstName") == null || request.getParameter("firstName").equals("")) &&
			    (request.getParameter("lastName") == null || request.getParameter("lastName").equals(""))) {
				throw new Exception("Please enter first or last name.");
			}
			if (request.getParameter("userName") == null || request.getParameter("userName").equals("")) {
					throw new Exception("Please enter the user name.");
			}
			if (request.getParameter("role") == null || request.getParameter("role").equals("")) {
				throw new Exception("Please select a role (admin, user, guest).");
			}

			
			// Get rid of existing roles if the user name has changed
			boolean userNameChanged = false;
			if (!user.getUserName().equals(request.getParameter("userName"))) {
				userNameChanged = true;
			}
			if (userNameChanged) {
				// Make sure this user name doesn't exist
				List users = sess.createQuery("SELECT u.userName from User u where u.userName = '" + request.getParameter("userName") + "'").list();
				if (users.size() > 0) {
					throw new Exception("The user name " + request.getParameter("userName") + " is already taken.  Please enter a unique user name.");
				}
				
				for (UserRole role : (Set<UserRole>)user.getRoles()) {
					sess.delete(role);						
					sess.flush();
				}
			}
			

			// Set the fields to the values from the screen
			user.setFirstName(request.getParameter("firstName"));
			user.setMiddleName(request.getParameter("middleName"));
			user.setLastName(request.getParameter("lastName"));
			user.setUserName(request.getParameter("userName"));
			
			// Encrypt the password
			if (!request.getParameter("password").equals(User.MASKED_PASSWORD)) {
				String pw = user.getUserName() + ":" + REALM + ":" + request.getParameter("password");
				String digestedPassword = RealmBase.Digest(pw, "MD5", null );
				user.setPassword(digestedPassword);				
			}
			
			// Flush here so that if user name changes, the user row is
			// updated before trying to insert a new role
			sess.flush();
			
			// Set existing user roles
			if (user.getRoles() != null && !userNameChanged) {
				for (UserRole role : (Set<UserRole>)user.getRoles()) {
					role.setRoleName(request.getParameter("role"));
					role.setUserName(user.getUserName());
				}
			} else {
				// New create a new user role
				UserRole role = new UserRole();
				role.setRoleName(Das2ManagerSecurity.USER_ROLE);
				role.setUserName(user.getUserName());
				role.setIdUser(user.getIdUser());
				sess.save(role);
			}
			

			sess.flush();
						
			tx.commit();

			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idUser", user.getIdUser().toString());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}

	@SuppressWarnings("unchecked")
	private void handleUserPasswordRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
			
			User user = User.class.cast(sess.load(User.class, this.das2ManagerSecurity.getIdUser()));
						
			// Encrypt the password
			if (!request.getParameter("password").equals(User.MASKED_PASSWORD) && !request.getParameter("password").equals("")) {
				String pw = user.getUserName() + ":" + REALM + ":" + request.getParameter("password");
				String digestedPassword = RealmBase.Digest(pw, "MD5", null );
				user.setPassword(digestedPassword);				
			}
			
			// Flush here so that if user name changes, the user row is
			// updated before trying to insert a new role
			sess.flush();
	
			tx.commit();

			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idUser", user.getIdUser().toString());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}

	
	@SuppressWarnings("unchecked")
	private void handleGroupAddRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		// Only admins can add groups
		if (!this.das2ManagerSecurity.isAdminRole()) {
			throw new Exception("Insufficient permissions to add groups.");
		}
		// Make sure required fields are filled in.
		if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
			throw new Exception("Please enter the group name.");
		}

		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
			
			UserGroup group = new UserGroup();
			
			group.setName(request.getParameter("name"));

			sess.save(group);
			
			sess.flush();
						
			tx.commit();
			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idUserGroup", group.getIdUserGroup().toString());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}
	

	@SuppressWarnings("unchecked")
	private void handleGroupDeleteRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
			
			UserGroup group = UserGroup.class.cast(sess.load(UserGroup.class, Util.getIntegerParameter(request, "idUserGroup")));
			
			// Check write permissions
			if (!this.das2ManagerSecurity.canWrite(group)) {
				throw new Exception("Insufficient permissions to delete group.");
			}
			
			sess.delete(group);
			
			sess.flush();
			
			
			tx.commit();

			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}


	
	@SuppressWarnings("unchecked")
	private void handleGroupUpdateRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
			
			UserGroup group = UserGroup.class.cast(sess.load(UserGroup.class, Util.getIntegerParameter(request, "idUserGroup")));
			
			// Check write permissions
			if (!this.das2ManagerSecurity.canWrite(group)) {
				throw new Exception("Insufficient permissions to write group.");
			}
			
			// Make sure required fields are filled in.
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new Exception("Please enter the group name.");
			}

			group.setName(request.getParameter("name"));
			
			
			// Add members
			StringReader reader = new StringReader(request.getParameter("membersXML"));
			SAXReader sax = new SAXReader();
			Document membersDoc = sax.read(reader);
			TreeSet members = new TreeSet(new UserComparator());
			for(Iterator i = membersDoc.getRootElement().elementIterator(); i.hasNext();) {
				Element memberNode = (Element)i.next();
				Integer idUser = new Integer(memberNode.attributeValue("idUser"));
				User member = User.class.cast(sess.get(User.class, idUser));
				members.add(member);
			}    
			group.setMembers(members);
			
			// Add collaborators
			reader = new StringReader(request.getParameter("collaboratorsXML"));
			sax = new SAXReader();
			Document collabsDoc = sax.read(reader);
			TreeSet collaborators = new TreeSet(new UserComparator());
			for(Iterator i = collabsDoc.getRootElement().elementIterator(); i.hasNext();) {
				Element collabNode = (Element)i.next();
				Integer idUser = new Integer(collabNode.attributeValue("idUser"));
				User collab = User.class.cast(sess.get(User.class, idUser));
				collaborators.add(collab);
			}    
			group.setCollaborators(collaborators);
			
			// Add managers
			reader = new StringReader(request.getParameter("managersXML"));
			sax = new SAXReader();
			Document managersDoc = sax.read(reader);
			TreeSet managers = new TreeSet(new UserComparator());
			for(Iterator i = managersDoc.getRootElement().elementIterator(); i.hasNext();) {
				Element mgrNode = (Element)i.next();
				Integer idUser = new Integer(mgrNode.attributeValue("idUser"));
				User mgr = User.class.cast(sess.get(User.class, idUser));
				managers.add(mgr);
			}    
			group.setManagers(managers);
			
			
			sess.flush();
						
			tx.commit();

			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idUserGroup", group.getIdUserGroup().toString());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}

	

	
	@SuppressWarnings("unchecked")
	private void handleDictionaryAddRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			Transaction tx = sess.beginTransaction();
			
			String dictionaryName = request.getParameter("dictionaryName");
			Integer id = null;
			
			if (dictionaryName.equals("AnalysisType")) {
				AnalysisType dict = new AnalysisType();
				dict.setName(request.getParameter("name"));
				dict.setIsActive(Util.getFlagParameter(request, "isActive"));
				dict.setIdUser(this.das2ManagerSecurity.isAdminRole() ? null : this.das2ManagerSecurity.getIdUser());
				sess.save(dict);
				id = dict.getIdAnalysisType();
			} else if (dictionaryName.equals("ExperimentMethod")) {
				ExperimentMethod dict = new ExperimentMethod();
				dict.setName(request.getParameter("name"));
				dict.setIsActive(Util.getFlagParameter(request, "isActive"));
				dict.setIdUser(this.das2ManagerSecurity.isAdminRole() ? null : this.das2ManagerSecurity.getIdUser());
				sess.save(dict);
				id = dict.getIdExperimentMethod();
			} else if (dictionaryName.equals("ExperimentPlatform")) {
				ExperimentPlatform dict = new ExperimentPlatform();
				dict.setName(request.getParameter("name"));
				dict.setIsActive(Util.getFlagParameter(request, "isActive"));
				dict.setIdUser(this.das2ManagerSecurity.isAdminRole() ? null : this.das2ManagerSecurity.getIdUser());
				sess.save(dict);
				id = dict.getIdExperimentPlatform();
			} 
			
			sess.flush();

			tx.commit();
			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("id", id.toString());
			root.addAttribute("dictionaryName", dictionaryName);
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}
	

	@SuppressWarnings("unchecked")
	private void handleDictionaryDeleteRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			String dictionaryName = request.getParameter("dictionaryName");
			Integer id = Util.getIntegerParameter(request, "id");
			
			if (dictionaryName.equals("AnalysisType")) {
				AnalysisType dict = AnalysisType.class.cast(sess.load(AnalysisType.class, id));
				// Check write permissions
				if (!this.das2ManagerSecurity.canWrite(dict)) {
					throw new Exception("Insufficient permissions to delete dictionary entry.");
				}
				sess.delete(dict);
				
			} else if (dictionaryName.equals("ExperimentMethod")) {
				ExperimentMethod dict = ExperimentMethod.class.cast(sess.load(ExperimentMethod.class, id));
				// Check write permissions
				if (!this.das2ManagerSecurity.canWrite(dict)) {
					throw new Exception("Insufficient permissions to delete dictionary entry.");
				}
				sess.delete(dict);
				
			} else if (dictionaryName.equals("ExperimentPlatform")) {
				ExperimentPlatform dict = ExperimentPlatform.class.cast(sess.load(ExperimentPlatform.class, id));
				// Check write permissions
				if (!this.das2ManagerSecurity.canWrite(dict)) {
					throw new Exception("Insufficient permissions to delete dictionary entry.");
				}
				sess.delete(dict);
				
			} 
			
			sess.flush();
			
					
			tx.commit();

			
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}


	
	@SuppressWarnings("unchecked")
	private void handleDictionaryUpdateRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();
			

			String dictionaryName = request.getParameter("dictionaryName");
			Integer id = Util.getIntegerParameter(request, "id");
			
			if (dictionaryName.equals("AnalysisType")) {
				AnalysisType dict = AnalysisType.class.cast(sess.load(AnalysisType.class, id));
				// Check write permissions
				if (!this.das2ManagerSecurity.canWrite(dict)) {
					throw new Exception("Insufficient permissions to write dictionary entry.");
				}

				dict.setName(request.getParameter("name"));
				dict.setIsActive(Util.getFlagParameter(request, "isActive"));
				if (this.das2ManagerSecurity.isAdminRole()) {
					dict.setIdUser(Util.getIntegerParameter(request, "idUser"));
				}
				
			} else if (dictionaryName.equals("ExperimentMethod")) {
				ExperimentMethod dict = ExperimentMethod.class.cast(sess.load(ExperimentMethod.class, id));
				// Check write permissions
				if (!this.das2ManagerSecurity.canWrite(dict)) {
					throw new Exception("Insufficient permissions to write dictionary entry.");
				}

				dict.setName(request.getParameter("name"));
				dict.setIsActive(Util.getFlagParameter(request, "isActive"));
				if (this.das2ManagerSecurity.isAdminRole()) {
					dict.setIdUser(Util.getIntegerParameter(request, "idUser"));
				}
				
			} else if (dictionaryName.equals("ExperimentPlatform")) {
				ExperimentPlatform dict = ExperimentPlatform.class.cast(sess.load(ExperimentPlatform.class, id));
				// Check write permissions
				if (!this.das2ManagerSecurity.canWrite(dict)) {
					throw new Exception("Insufficient permissions to write dictionary entry.");
				}

				dict.setName(request.getParameter("name"));
				dict.setIsActive(Util.getFlagParameter(request, "isActive"));				
				if (this.das2ManagerSecurity.isAdminRole()) {
					dict.setIdUser(Util.getIntegerParameter(request, "idUser"));
				}
			} 

			sess.flush();
						
			tx.commit();

			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("id", id.toString());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", e.getMessage());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
            OutputFormat.createCompactFormat());
			writer.write(doc);
			
			if (tx != null) {
				tx.rollback();
			}
			
		} finally {
			
			if (sess != null) {
				sess.close();
			}
		}
		
	}

	
	
	private String getFlexHTMLWrapper() {
		StringBuffer buf = new StringBuffer();
		BufferedReader input = null;
		try {
			String fileName = getServletContext().getRealPath("/");
			fileName += "/" + this.DAS2_MANAGER_HTML_WRAPPER;
			FileReader fileReader = new FileReader(fileName);
			input = new BufferedReader(fileReader);
		} catch (FileNotFoundException ex) {
			System.out.println(ex.toString());
		}
		if (input != null) {
			try {
				String line = null;
				while ((line = input.readLine()) != null) {
					buf.append(line);
					buf.append(System.getProperty("line.separator"));
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
				try {
					input.close();
				} catch (IOException e) {
				}
			}

		}
		return buf.toString();
	}

	private final boolean getGenometryManagerDataDir() {
		// attempt to get properties from servlet context
		ServletContext context = getServletContext();
		genometry_db_annotation_dir = context.getInitParameter("genometry_db_annotation_dir");

		if (genometry_db_annotation_dir != null && !genometry_db_annotation_dir.endsWith("/")) {
		  genometry_db_annotation_dir += "/";			
		}

		//print values
		System.out.println("Das2ManagerServlet: genometry_manager_data_dir\t" + genometry_db_annotation_dir);

		return true;
	}

	/**Loads a file's lines into a hash first column is the key, second the value.
	 * Skips blank lines and those starting with a '#'
	 * @return null if an exception in thrown
	 * */
	private static final HashMap<String, String> loadFileIntoHashMap(File file) {
		BufferedReader in = null;
		HashMap<String, String> names = null;
		try {
			names = new HashMap<String, String>();
			in = new BufferedReader(new FileReader(file));
			String line;
			String[] keyValue;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#")) {
					continue;
				}
				keyValue = line.split("\\s+");
				if (keyValue.length < 2) {
					continue;
				}
				names.put(keyValue[0], keyValue[1]);
			}            
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			GeneralUtils.safeClose(in);
		}
		return names;
	}


}
