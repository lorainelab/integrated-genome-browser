package com.affymetrix.genometry.servlets.das2manager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.mortbay.http.HttpRequest;

public class AnnotationQuery {

  
	// Criteria
	private String             scopeLevel;
	private Integer            idSecurityGroup;
	private Integer            idOrganism;
	private Integer            idGenomeVersion;
	private String             text;
	
    
	private StringBuffer        queryBuf;
	private boolean            addWhere = true;
	
	private static String      KEY_DELIM = "!!!!";
	
	private static final int ANNOTATION_GROUPING_LEVEL = 1;
	private static final int ANNOTATION_LEVEL = 2;
	
	
	private TreeMap<String, TreeMap<String, ?>> organismToVersion;
	private HashMap<String, TreeMap<String, ?>> versionToRootGroupings;
	private HashMap<String, TreeMap<String, ?>> groupingToGroupings;
	private HashMap<String, TreeMap<String, ?>> groupingToAnnotations;
	
	private HashMap<String, List<Segment>>           versionToSegments;  
	
	private HashMap<String,  Organism>               organismMap;
	private HashMap<String,  GenomeVersion>          genomeVersionMap;
	private HashMap<Integer, Annotation>             annotationMap;
	private HashMap<Integer, AnnotationGrouping>     annotationGroupingMap;
	
	public AnnotationQuery() {
		if (scopeLevel == null || scopeLevel.equals("")) {
			scopeLevel = Das2ManagerSecurity.ALL_SCOPE_LEVEL;
		}		
	}
	
	public AnnotationQuery(HttpServletRequest req) {
		scopeLevel         = req.getParameter("scopeLevel");
		idSecurityGroup    = Util.getIntegerParameter(req, "idSecurityGroup");
		idOrganism         = Util.getIntegerParameter(req, "idOrganism");
		idGenomeVersion    = Util.getIntegerParameter(req, "idGenomeVersion");
		text               = req.getParameter("text");
		
		if (scopeLevel == null || scopeLevel.equals("")) {
			scopeLevel = Das2ManagerSecurity.USER_SCOPE_LEVEL;
		}		
	}

	@SuppressWarnings("unchecked")
	public Document getAnnotationDocument(Session sess, Das2ManagerSecurity das2ManagerSecurity) throws Exception {
		
		// Run query to get annotation grouping and annotations, organized under
		// organism and genome version
		StringBuffer queryBuf = this.getAnnotationQuery(das2ManagerSecurity);
    	Query query = sess.createQuery(queryBuf.toString());
		List<Object[]> annotationRows = (List<Object[]>)query.list();
		
	
		// Create an XML document
		Document doc = this.getAnnotationDocument(annotationRows, DictionaryHelper.getInstance(sess), das2ManagerSecurity);
		return doc;
		
	}
	
	@SuppressWarnings("unchecked")
	public void runAnnotationQuery(Session sess, Das2ManagerSecurity das2ManagerSecurity) throws Exception {
		
		// Run query to get annotation grouping and annotations, organized under
		// organism and genome version
		StringBuffer queryBuf = this.getAnnotationQuery(das2ManagerSecurity);    	
		System.out.println("Annotation query: " + queryBuf.toString());
    	Query query = sess.createQuery(queryBuf.toString());
		List<Object[]> annotationRows = (List<Object[]>)query.list();
		
		// Now run query to get the genome version segments
		queryBuf = this.getSegmentQuery();			
    	query = sess.createQuery(queryBuf.toString());
		List<Segment> segmentRows = (List<Segment>)query.list();
			
		this.hashAnnotations(annotationRows, segmentRows, DictionaryHelper.getInstance(sess));
		
	}

	private StringBuffer getAnnotationQuery(Das2ManagerSecurity das2Security) throws Exception {
		
		addWhere = true;
		queryBuf = new StringBuffer();

		queryBuf.append(" SELECT     org, ");
		queryBuf.append("            ver, ");
		queryBuf.append("            ag, ");
		queryBuf.append("            pag, ");
		queryBuf.append("            a  ");
		queryBuf.append(" FROM       Organism as org ");
		queryBuf.append(" JOIN       org.genomeVersions as ver ");
		queryBuf.append(" JOIN       ver.annotationGroupings as ag ");
		queryBuf.append(" LEFT JOIN  ag.parentAnnotationGrouping as pag ");
		queryBuf.append(" LEFT JOIN  ag.annotations as a ");

		addWhere = true;

		addCriteria(ANNOTATION_GROUPING_LEVEL);
		
		if (das2Security != null) {
			das2Security.appendHQLSecurity(scopeLevel, queryBuf, "a", "ag", addWhere);			
		}
		
		queryBuf.append(" ORDER BY org.name asc, ver.buildDate desc, ag.name asc, a.name asc ");

		return queryBuf;

	}


	
	private StringBuffer getSegmentQuery() throws Exception {
		addWhere = true;
		queryBuf = new StringBuffer();
		queryBuf.append(" SELECT     seg  ");
		queryBuf.append(" FROM       Segment as seg  ");
		queryBuf.append(" ORDER BY   seg.sortOrder");
		
		return queryBuf;

	}

	private Document getAnnotationDocument(List<Object[]> annotationRows, DictionaryHelper dictionaryHelper, Das2ManagerSecurity das2ManagerSecurity) {
		
		// Organize results rows into hash tables
		hashAnnotations(annotationRows, null, dictionaryHelper);		
		

		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("Annotations");
		
		Element organismNode = null;
		Element versionNode  = null;
		String[] tokens;
				
		// Use hash to create XML Document
		for (String organismName : organismToVersion.keySet()) {
			TreeMap<String, ?> versionNameMap = organismToVersion.get(organismName);
			Organism organism = organismMap.get(organismName);

			organismNode = root.addElement("Organism");
			organismNode.addAttribute("label", organismName);
			organismNode.addAttribute("idOrganism", organism.getIdOrganism().toString());
			organismNode.addAttribute("name",         organism.getName() != null ? organism.getName() : "");				
			organismNode.addAttribute("commonName",   organism.getCommonName() != null ? organism.getCommonName() : "");				
			organismNode.addAttribute("binomialName", organism.getBinomialName() != null ? organism.getBinomialName() : "");				
			organismNode.addAttribute("NCBITaxID",    organism.getNCBITaxID() != null ? organism.getNCBITaxID() : "");		
			organismNode.addAttribute("canWrite",     das2ManagerSecurity.canWrite(organism) ? "Y" : "N");
			
			// For each version, build up hierarchy
			if (versionNameMap != null) {
				for (String versionName : versionNameMap.keySet()) {
					
					GenomeVersion genomeVersion = genomeVersionMap.get(versionName);
					
					versionNode = organismNode.addElement("GenomeVersion");
					versionNode.addAttribute("label", versionName);				
					versionNode.addAttribute("idGenomeVersion",genomeVersion.getIdGenomeVersion().toString());				
					versionNode.addAttribute("name",           genomeVersion.getName());				
					versionNode.addAttribute("buildDate",      genomeVersion.getBuildDate() != null ? Util.formatDate(genomeVersion.getBuildDate()) : "");				
					versionNode.addAttribute("idOrganism",     genomeVersion.getIdOrganism().toString());				
					versionNode.addAttribute("coordURI",       genomeVersion.getCoordURI() != null ? genomeVersion.getCoordURI().toString() : "");	
					versionNode.addAttribute("coordVersion",   genomeVersion.getCoordVersion() != null ? genomeVersion.getCoordVersion().toString() : "");	
					versionNode.addAttribute("coordSource",    genomeVersion.getCoordSource() != null ? genomeVersion.getCoordSource().toString() : "");	
					versionNode.addAttribute("coordTestRange", genomeVersion.getCoordTestRange() != null ? genomeVersion.getCoordTestRange().toString() : "");	
					versionNode.addAttribute("coordAuthority", genomeVersion.getCoordAuthority() != null ? genomeVersion.getCoordAuthority().toString() : "");	
					versionNode.addAttribute("canWrite",     das2ManagerSecurity.canWrite(genomeVersion) ? "Y" : "N");
					
					// For each root annotation grouping, recurse to create annotations
					// and groupings.
					TreeMap<String, ?> rootGroupings = versionToRootGroupings.get(versionName);
					fillGroupingNode(genomeVersion, versionNode, rootGroupings, das2ManagerSecurity, dictionaryHelper, false);
				}
				
			}
			
		}

		return doc;
		
	}
	
	private void hashAnnotations(List<Object[]> annotationRows, List<Segment> segmentRows, DictionaryHelper dictionaryHelper) {

		organismToVersion        = new TreeMap<String, TreeMap<String, ?>>();
		versionToRootGroupings   = new HashMap<String, TreeMap<String, ?>>();
		groupingToGroupings      = new HashMap<String, TreeMap<String, ?>>();
		groupingToAnnotations    = new HashMap<String, TreeMap<String, ?>>();
		versionToSegments        = new HashMap<String, List<Segment>>();
		
		organismMap              = new HashMap<String, Organism>();
		genomeVersionMap         = new HashMap<String, GenomeVersion>();
		annotationGroupingMap    = new HashMap<Integer, AnnotationGrouping>();
		annotationMap            = new HashMap<Integer, Annotation>();
		
		// Prefill organism, genome version, and root annotation grouping
		// hash map with known entries
		// since those without annotations would otherwise not show up.
		for (Organism o : dictionaryHelper.getOrganisms()) {
			organismMap.put(o.getName(), o);
			
			// If we are filtering by organism, only include that one
			if (this.idOrganism != null) {
				if (!this.idOrganism.equals(o.getIdOrganism())) {
					continue;
				}
			}
			TreeMap<String, ?> versionNameMap = new TreeMap<String, String>();
			organismToVersion.put(o.getName(), versionNameMap);
			if (dictionaryHelper.getGenomeVersions(o.getIdOrganism()) != null) {
				for(GenomeVersion v : dictionaryHelper.getGenomeVersions(o.getIdOrganism())) {
					
					// If we are filtering by genome version, only include that one
					if (this.idGenomeVersion != null) {
						if (!this.idGenomeVersion.equals(v.getIdGenomeVersion())) {
							continue;
						}
					}

					versionNameMap.put(v.getName(), null);
					
					genomeVersionMap.put(v.getName(), v);
					AnnotationGrouping rootGrouping = v.getRootAnnotationGrouping();
					
					if (rootGrouping != null) {
						String groupingKey       = rootGrouping.getName()  + KEY_DELIM + rootGrouping.getIdAnnotationGrouping();
						TreeMap<String, String> groupings = new TreeMap<String, String>();
						groupings.put(groupingKey, null);
						versionToRootGroupings.put(v.getName(), groupings);
					}
				
				}
				
			}
		}
		
		// Hash segments for each genome version
		if (segmentRows != null) {
			for (Segment segment : segmentRows) {
				if (segment == null) {
					continue;
				}
				GenomeVersion genomeVersion = dictionaryHelper.getGenomeVersion(segment.getIdGenomeVersion());
				if (genomeVersion == null) {
					System.out.println("Warning - Segment " + segment.getIdSegment() + " does not belong to a valid Genome Version");
					continue;
				}
				List<Segment> segments = versionToSegments.get(genomeVersion.getName());
				if (segments == null) {
					segments = new ArrayList<Segment>();
					versionToSegments.put(genomeVersion.getName(), segments);
				}
				segments.add(segment);
			}			
		}

		
		// Hash to create hierarchy:
		//   Organism
		//     Genome Version
		//       Annotation
		//       Annotation Grouping
		//          Annotation

		// Hash the annotation grouping->annotation.  We get
		// a row for each annotation grouping.  
		// 1. Hash the genome versions under the organism.
		// 2. Hash the root annotation groupings under the organism.
		//    (root annotations are under the root annotation grouping for
		//     the genome version.  We just hide this annotation grouping
		//     and show the annotations under the genome version node instead.
		// 3. Hash the annotation groupings under parent annotation grouping
		//    and the annotations under the parent annotation grouping.
		for (Object[] row : annotationRows) {
			Organism organism                      = (Organism) row[0];
			GenomeVersion genomeVersion            = (GenomeVersion)  row[1];
			AnnotationGrouping annotGrouping       = (AnnotationGrouping) row[2];
			AnnotationGrouping parentAnnotGrouping = (AnnotationGrouping) row[3];
			Annotation annot                       = (Annotation) row[4];
			
			// Load properties for anotations
			if (annot != null) {
				annot.loadProps(dictionaryHelper);				
			}
			
			// Hash genome versions for an organism
			TreeMap<String, ?> versionNameMap = organismToVersion.get(organism.getName());
			if (versionNameMap == null) {
				versionNameMap = new TreeMap<String, String>();
				organismToVersion.put(organism.getName(), versionNameMap);
			}
			if (genomeVersion != null) {
				versionNameMap.put(genomeVersion.getName(), null);
				genomeVersionMap.put(genomeVersion.getName(), genomeVersion);
			}
			
			if (annotGrouping != null) {
				String groupingKey       = annotGrouping.getName()  + KEY_DELIM + annotGrouping.getIdAnnotationGrouping();
				// Hash root annotation groupings for a genome version
				if (parentAnnotGrouping == null) {

					TreeMap<String, ?> groupingNameMap = versionToRootGroupings.get(genomeVersion.getName());
					if (groupingNameMap == null) {
						groupingNameMap = new TreeMap<String, String>();
						versionToRootGroupings.put(genomeVersion.getName(), groupingNameMap);
					}
					groupingNameMap.put(groupingKey, null);				
				} else {
					String parentGroupingKey = parentAnnotGrouping.getName() + KEY_DELIM + parentAnnotGrouping.getIdAnnotationGrouping();
					
					// Hash annotation grouping for a parent annotation grouping
					TreeMap<String, ?> childGroupingNameMap = groupingToGroupings.get(parentGroupingKey);
					if (childGroupingNameMap == null) {
						childGroupingNameMap = new TreeMap<String, String>();
						groupingToGroupings.put(parentGroupingKey, childGroupingNameMap);
					}
					childGroupingNameMap.put(groupingKey, null);				
				}
				annotationGroupingMap.put(annotGrouping.getIdAnnotationGrouping(), annotGrouping);				

				// Hash annotations for an annotation grouping
				if (annot != null) {
					TreeMap<String, ?> annotNameMap = groupingToAnnotations.get(groupingKey);
					if (annotNameMap == null) {
						annotNameMap = new TreeMap<String, String>();
						groupingToAnnotations.put(groupingKey, annotNameMap);
					}
					String annotKey       = annot.getName()  + KEY_DELIM + annot.getIdAnnotation();
					annotNameMap.put(annotKey, null);
					annotationMap.put(annot.getIdAnnotation(), annot);
				}			
			}
			
			
			
		}
		
	}
	

	public Set<String> getOrganismNames() {
		return organismToVersion.keySet();
	}
	
	public Set<String> getVersionNames(String organismName) {
		Set<String> versionNames = new TreeSet<String>();
		
		TreeMap<String, ?> versionNameMap = organismToVersion.get(organismName);
		if (versionNameMap != null) {
			versionNames = versionNameMap.keySet();
		}
		
		return versionNames;
	}
	
	public List<Segment> getSegments(String organismName, String genomeVersionName) {
		List<Segment> segments = null;
		TreeMap<String, ?> versionNameMap = organismToVersion.get(organismName);
			
		// For each version...
		if (versionNameMap != null) {
			for (String versionName : versionNameMap.keySet()) {
				
				if (versionName.equals(genomeVersionName)) {
					segments = versionToSegments.get(versionName);
					break;
				}
			}
		}
		return segments;		
	}
	
	public List<QualifiedAnnotation> getQualifiedAnnotations(String organismName, String genomeVersionName) {
		List<QualifiedAnnotation> qualifiedAnnotations = new ArrayList<QualifiedAnnotation>();
		
				
		TreeMap<String, ?> versionNameMap = organismToVersion.get(organismName);
			
		// For each version...
		if (versionNameMap != null) {
			for (String versionName : versionNameMap.keySet()) {
				
				if (versionName.equals(genomeVersionName)) {
					// For each root annotation grouping, recurse annotation grouping
					// hierarchy to get leaf annotations.
					TreeMap<String, ?> rootGroupingNameMap = versionToRootGroupings.get(versionName);
					String qualifiedName = new String();
					getQualifiedAnnotation(rootGroupingNameMap, qualifiedAnnotations, qualifiedName, false);
					
				}
				
			}
			
		}
		return qualifiedAnnotations;		
	}
	
	private void getQualifiedAnnotation(TreeMap<String, ?> theGroupings, List<QualifiedAnnotation> qualifiedAnnotations, String typePrefix, boolean showGroupingLevel) {
		if (theGroupings != null) {
			for (String groupingKey : theGroupings.keySet()) {
				String[] tokens     = groupingKey.split(KEY_DELIM);
				String groupingName          = tokens[0];
				Integer idAnnotationGrouping = new Integer(tokens[1]);
				
				
				// For each annotation....
				TreeMap<String, ?> annotNameMap = groupingToAnnotations.get(groupingKey);
				if (annotNameMap != null) {
					// For each annotation...
					for (String annotNameKey : annotNameMap.keySet()) { 
						String[] tokens1    = annotNameKey.split(KEY_DELIM);
						Integer idAnnotation = new Integer(tokens1[1]);
													
						Annotation annot = annotationMap.get(idAnnotation);
						
						String fullTypePrefix = concatenateTypePrefix(typePrefix, groupingName, showGroupingLevel);
						if (fullTypePrefix != null && fullTypePrefix.length() > 0) {
							fullTypePrefix += "/";
						}
						
						qualifiedAnnotations.add(new QualifiedAnnotation(annot, 
								fullTypePrefix + annot.getName(), 
								fullTypePrefix + annot.getName() ));
					}						
				}
											
				// Recurse for each annotation grouping (under a grouping)
				TreeMap<String, ?> childGroupings = groupingToGroupings.get(groupingKey);
				if (childGroupings != null) {
					getQualifiedAnnotation(childGroupings, qualifiedAnnotations, concatenateTypePrefix(typePrefix, groupingName, showGroupingLevel), true);					
				}
			}					
		}
	}
	
	private String concatenateTypePrefix(String typePrefix, String groupingName, boolean showGroupingLevel) {
		if (showGroupingLevel) {
			if (typePrefix == null || typePrefix.equals("")) {
				return groupingName;
			} else {
				return typePrefix + "/" + groupingName;
			}
		} else {
			return typePrefix != null ? typePrefix : "";
		}
	}


	
	private void fillGroupingNode(GenomeVersion genomeVersion, Element parentNode, TreeMap<String, ?> theGroupings, Das2ManagerSecurity das2ManagerSecurity, DictionaryHelper dictionaryHelper, boolean showGroupingLevel) {
		if (theGroupings != null) {
			for (String groupingKey : theGroupings.keySet()) {
				String[] tokens     = groupingKey.split(KEY_DELIM);
				String groupingName          = tokens[0];
				Integer idAnnotationGrouping = new Integer(tokens[1]);
				
				Element groupingNode = null;
				AnnotationGrouping annotGrouping = null;
				if (showGroupingLevel) {
					annotGrouping = annotationGroupingMap.get(idAnnotationGrouping);
					
				    groupingNode = parentNode.addElement("AnnotationGrouping");
					groupingNode.addAttribute("label", groupingName);	
					groupingNode.addAttribute("idAnnotationGrouping", annotGrouping.getIdAnnotationGrouping().toString());	
					groupingNode.addAttribute("idGenomeVersion", genomeVersion.getIdGenomeVersion().toString());	
					groupingNode.addAttribute("name", annotGrouping.getName().toString());	
					groupingNode.addAttribute("description", annotGrouping.getDescription() != null ? annotGrouping.getDescription() : "");	
					groupingNode.addAttribute("canWrite",    das2ManagerSecurity.canWrite(annotGrouping) ? "Y" : "N");
					groupingNode.addAttribute("owner", dictionaryHelper.getUserFullName(annotGrouping.getIdUser()));
					groupingNode.addAttribute("idUser",annotGrouping.getIdUser() != null ? annotGrouping.getIdUser().toString() : "");
					
				} else {
					groupingNode = parentNode;					
				}
				
				// For each annotation
				TreeMap<String, ?> annotNameMap = groupingToAnnotations.get(groupingKey);
				if (annotNameMap != null) {
					// For each annotation...
					for (String annotNameKey : annotNameMap.keySet()) { 
						String[] tokens1    = annotNameKey.split(KEY_DELIM);
						Integer idAnnotation = new Integer(tokens1[1]);
						
						Annotation annot = annotationMap.get(idAnnotation);

						Element annotNode = groupingNode.addElement("Annotation");
						annotNode.addAttribute("label", annot.getName());
						annotNode.addAttribute("idAnnotation", annot.getIdAnnotation().toString());
						annotNode.addAttribute("idGenomeVersion", genomeVersion.getIdGenomeVersion().toString());	
						annotNode.addAttribute("idAnnotationGrouping", annotGrouping != null ? annotGrouping.getIdAnnotationGrouping().toString() : "");	
						annotNode.addAttribute("codeVisibility", annot.getCodeVisibility());
						annotNode.addAttribute("canWrite",       das2ManagerSecurity.canWrite(annot) ? "Y" : "N");
					}						
				}
											
				// Recurse for each annotation grouping (under a grouping)
				TreeMap<String, ?> childGroupings = groupingToGroupings.get(groupingKey);
				fillGroupingNode(genomeVersion, groupingNode, childGroupings, das2ManagerSecurity, dictionaryHelper, true);
			}					
		}

	}

   
	

  
  

	private void addCriteria(int joinLevel) {

		// Search by organism
		if (idOrganism != null) {
			this.addWhereOrAnd();
			queryBuf.append(" org.idOrganism = ");
			queryBuf.append(idOrganism);
		}
		// Search by genome version
		if (idGenomeVersion != null) {
			this.addWhereOrAnd();
			queryBuf.append(" ver.idGenomeVersion = ");
			queryBuf.append(idGenomeVersion);
		}
		// Search for annotations and annotation groups for a particular security group
		if (idSecurityGroup != null) {
			this.addWhereOrAnd();
			queryBuf.append("(");
			queryBuf.append(" a.idSecurityGroup = " + this.idSecurityGroup);
			if (joinLevel == ANNOTATION_GROUPING_LEVEL) {
				queryBuf.append("  OR ");
				queryBuf.append(" a.idSecurityGroup is null");				
			}
			queryBuf.append(")");
		}

	}

  

  
	protected boolean addWhereOrAnd() {
		if (addWhere) {
			queryBuf.append(" WHERE ");
			addWhere = false;
		} else {
			queryBuf.append(" AND ");
		}
		return addWhere;
	}



  
}
