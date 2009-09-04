package com.affymetrix.genometry.servlets.das2manager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.Session;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.AnnotSecurity;


public class Das2ManagerSecurity implements AnnotSecurity {
	
	public static final String    SESSION_KEY = "Das2SecurityManager";
	public static final String    ADMIN_ROLE  = "das2admin";
	public static final String    USER_ROLE   = "das2user";
	public static final String    GUEST_ROLE  = "das2guest";

	public static final String    USER_SCOPE_LEVEL  = "USER";
	public static final String    GROUP_SCOPE_LEVEL = "GROUP";
	public static final String    ALL_SCOPE_LEVEL   = "ALL";
	
	private boolean                scrutinizeAccess = false;
	
	
	private User                    user;
	private boolean                isAdminRole = false;
	private boolean                isGuestRole = true;
	
	
	private HashMap<Integer, SecurityGroup>   groupsMemCollabVisibility = new HashMap<Integer, SecurityGroup>();
	private HashMap<Integer, SecurityGroup>   groupsMemVisibility = new HashMap<Integer, SecurityGroup>();
	
	private HashMap<String, HashMap<Integer, QualifiedAnnotation>> versionToAuthorizedAnnotationMap = new HashMap<String, HashMap<Integer, QualifiedAnnotation>>();
	
	
	@SuppressWarnings("unchecked")
	public Das2ManagerSecurity(Session sess, String userName, boolean scrutinizeAccess, boolean isAdminRole, boolean isGuestRole) throws Exception {
		// Are the annotations loaded from the db?  If so, security
		// logic is driven from info in db, otherwise, access to all resources
		// is granted.
		this.scrutinizeAccess = scrutinizeAccess;
		
		if (this.scrutinizeAccess) {
			// Lookup user
			List<User> users = (List<User>)sess.createQuery("SELECT u from User as u where u.userName = '" + userName + "'").list();
			if (users == null || users.size() == 0) {
				throw new Exception("Cannot find user " + userName);
			}
			user = users.get(0);	
			this.isAdminRole = isAdminRole;
			this.isGuestRole = isGuestRole;
			
			for (SecurityGroup sc : (Set<SecurityGroup>)user.getMemberSecurityGroups()) {
				groupsMemCollabVisibility.put(sc.getIdSecurityGroup(), sc);
				groupsMemVisibility.put(sc.getIdSecurityGroup(), sc);
			}
			for (SecurityGroup sc : (Set<SecurityGroup>)user.getManagingSecurityGroups()) {
				groupsMemCollabVisibility.put(sc.getIdSecurityGroup(), sc);
				groupsMemVisibility.put(sc.getIdSecurityGroup(), sc);
			}
			for (SecurityGroup sc : (Set<SecurityGroup>)user.getCollaboratingSecurityGroups()) {
				groupsMemCollabVisibility.put(sc.getIdSecurityGroup(), sc);
			}
			this.loadAuthorizedResources(sess);	
		}
		
		
		
	}
	
	public Document getXML() {
		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("Das2ManagerSecurity");
		root.addAttribute("userName",        user != null ? user.getUserName() : "");
		root.addAttribute("userDisplayName", user != null ? user.getUserDisplayName() : "");
		root.addAttribute("name",            user != null ? user.getName() : "");
		root.addAttribute("isAdmin",         isAdminRole ? "Y" : "N");
		root.addAttribute("isGuest",         isGuestRole ? "Y" : "N");
		root.addAttribute("canManageUsers",  isAdminRole || (user != null && user.getManagingSecurityGroups().size() > 0) ? "Y" : "N");
		
		
		return doc;		
	}
	
	public boolean belongsToGroup(Integer idSecurityGroup) {
		return isMember(idSecurityGroup) || isCollaborator(idSecurityGroup) || isManager(idSecurityGroup);
	}
	
	public boolean belongsToGroup(SecurityGroup group) {
		return isMember(group) || isCollaborator(group) || isManager(group);
	}
	
	public boolean isMember(SecurityGroup group) {
		return isMember(group.getIdSecurityGroup());
	}
	
	@SuppressWarnings("unchecked")
	public boolean isMember(Integer idSecurityGroup) {
		if (!scrutinizeAccess) {
			return false;
		}
		
		boolean isMember = false;
		for(SecurityGroup g : (Set<SecurityGroup>)user.getMemberSecurityGroups()) {
			if (g.getIdSecurityGroup().equals(idSecurityGroup)) {
				isMember = true;
				break;
			}
		}
		return isMember;
	}
	
	public boolean isCollaborator(SecurityGroup group) {	
		return isCollaborator(group.getIdSecurityGroup());		
	}
	
	@SuppressWarnings("unchecked")
	public boolean isCollaborator(Integer idSecurityGroup) {
		if (!scrutinizeAccess) {
			return false;
		}
		boolean isCollaborator = false;
		for(SecurityGroup g : (Set<SecurityGroup>)user.getCollaboratingSecurityGroups()) {
			if (g.getIdSecurityGroup().equals(idSecurityGroup)) {
				isCollaborator = true;
				break;
			}
		}
		return isCollaborator;
	}
	
	public boolean isManager(SecurityGroup group) {
		return isManager(group.getIdSecurityGroup());
	}
	
	@SuppressWarnings("unchecked")
	public boolean isManager(Integer idSecurityGroup) {
		if (!scrutinizeAccess) {
			return false;
		}
		
		boolean isManager = false;
		if (this.isAdminRole) {
			isManager = true;
		} else {
			for(SecurityGroup g : (Set<SecurityGroup>)user.getManagingSecurityGroups()) {
				if (g.getIdSecurityGroup().equals(idSecurityGroup)) {
					isManager = true;
					break;
				}
			}			
		}
		return isManager;
	}
	
	@SuppressWarnings("unchecked")
	public SecurityGroup getDefaultSecurityGroup() {
		if (!scrutinizeAccess) {
			return null;
		}
		SecurityGroup defaultSecurityGroup = null;
		if (user.getManagingSecurityGroups() != null && user.getManagingSecurityGroups().size() > 0) {
			defaultSecurityGroup = SecurityGroup.class.cast(user.getManagingSecurityGroups().iterator().next());			
		} else if (user.getMemberSecurityGroups() != null && user.getMemberSecurityGroups().size() > 0) {
			defaultSecurityGroup = SecurityGroup.class.cast(user.getMemberSecurityGroups().iterator().next());			
		} else if (user.getCollaboratingSecurityGroups() != null && user.getCollaboratingSecurityGroups().size() > 0) {
			defaultSecurityGroup = SecurityGroup.class.cast(user.getCollaboratingSecurityGroups().iterator().next());			
		} 
		return defaultSecurityGroup;
	}
	
	@SuppressWarnings("unchecked")
	public boolean canRead(Object object) {
		if (!scrutinizeAccess) {
			return true;
		}
		
		boolean canRead = false;
		if (isAdminRole) {
			// Admins can read any annotation
			canRead = true;
		} else if (object instanceof Annotation) {
			Annotation a = Annotation.class.cast(object);
			if (a.getCodeVisibility().equals(Visibility.PUBLIC)) {
				// Public annotations can be read by anyone
				canRead = true;
				
			} else if (a.getCodeVisibility().equals(Visibility.MEMBERS)) {
				// Annotations with Members visibility can be read by members
				// or managers of the annotation's security group.				
				if (this.isMember(a.getIdSecurityGroup()) || this.isManager(a.getIdSecurityGroup())) {
					canRead = true;
				}
			} else if (a.getCodeVisibility().equals(Visibility.MEMBERS_AND_COLLABORATORS)) {
				// Annotations with Members & Collaborators visibility can be read by 
				// members, collaborators, or managers of the annotation's security group.				
				if (this.belongsToGroup(a.getIdSecurityGroup())) {
					canRead = true;
				}
			} else if (a.getIdUser().equals(user.getIdUser())) {
				// Owner of annotation can read it
			}	canRead = true;			
		} else if (object instanceof AnnotationGrouping) {
			AnnotationGrouping ag = AnnotationGrouping.class.cast(object);
			if (ag.hasVisibility(Visibility.PUBLIC)) {
				// Annotation groups for public annotations can be read by anyone
				canRead = true;
				
			} else if (ag.hasVisibility(Visibility.MEMBERS)) {
				// Annotation groups for Annotations with Members visibility can be 
				// read by members or managers of the annotation's security group.	
				for(Annotation a : (Set<Annotation>)ag.getAnnotationGroupings()) {
					if (this.isMember(a.getIdSecurityGroup()) || this.isManager(a.getIdSecurityGroup())) {
						canRead = true;
						break;
					}					
				}
			} else if (ag.hasVisibility(Visibility.MEMBERS_AND_COLLABORATORS)) {
				// Annotation groups for Annotations with Members & Collaborators 
				// visibility can be read by members, collaborators, or managers 
				// of the annotation's security group.				
				for(Annotation a : (Set<Annotation>)ag.getAnnotationGroupings()) {
					if (this.belongsToGroup(a.getIdSecurityGroup())) {
						canRead = true;
						break;
					}
				}
			} else if (ag.getIdUser().equals(user.getIdUser())) {
				// Owner of annotation grouping can read it
			}	canRead = true;
			
		} else {
			canRead = true;
		}
		return canRead;
	}
	
	@SuppressWarnings("unchecked")
	public boolean canWrite(Object object) {
		if (!scrutinizeAccess) {
			return false;
		}
		
		boolean canWrite = false;
		
		// Admins can write any object
		if (this.isAdminRole) {
			canWrite = true;
		} else if (object instanceof Owned) {
			// If object is owned by the user, he can write it 
			Owned o = Owned.class.cast(object);
			if (o.isOwner(user.getIdUser())) {
				canWrite = true;
			}
			
			if (!canWrite) {
				if (object instanceof Annotation) {
					Annotation a = Annotation.class.cast(object);
					if (this.isMember(a.getIdSecurityGroup())) {
						canWrite = true;
					} 
				}
			}
			
		} else if (object instanceof SecurityGroup) {
			SecurityGroup g = (SecurityGroup)object;
			if (this.isManager(g)) {
				canWrite = true;
			}
		} 
		
		return canWrite;
	}
	

	public boolean appendHQLSecurity(String scopeLevel,
			                           StringBuffer queryBuf, 
			                           String annotationAlias, 
			                           String annotationGroupingAlias, 
			                           boolean addWhere)
	  throws Exception {
		if (!scrutinizeAccess) {
			return addWhere;
		}
		
		if (isAdminRole) {
			
			// Admins don't have any restrictions
			return addWhere;
			
		} else if (isGuestRole) {
			
			// Only get public annotations for guests
			addWhere = addWhereOrAnd(addWhere, queryBuf);
			queryBuf.append("(");
			queryBuf.append(annotationAlias + ".codeVisibility = '" + Visibility.PUBLIC + "'");	
			queryBuf.append(")");
			
		} else if (scopeLevel.equals(this.USER_SCOPE_LEVEL)) {
			// Scope to annotations owned by this user
			addWhere = addWhereOrAnd(addWhere, queryBuf);
			appendUserOwnedHQLSecurity(queryBuf, annotationAlias, annotationGroupingAlias, addWhere);
			
		} else if (scopeLevel.equals(this.GROUP_SCOPE_LEVEL) || scopeLevel.equals(this.ALL_SCOPE_LEVEL)) {
			addWhere = addWhereOrAnd(addWhere, queryBuf);

			// If this user isn't part of any group or we aren't searching for public
			// annotations, add a security statement that will ensure no rows are returned.
			if (groupsMemCollabVisibility.isEmpty() && !scopeLevel.equals(this.ALL_SCOPE_LEVEL)) {
				appendUserOwnedHQLSecurity(queryBuf, annotationAlias, annotationGroupingAlias, addWhere);
			} else {
				boolean hasSecurityCriteria = false;
				queryBuf.append("(");
				
				if (!this.groupsMemVisibility.isEmpty()) {
					
					// For annotations with MEMBER visibility, limit to security group
					// in which user is member (or manager).  (Also, include
					// any public annotations belong one of the user's groups.)					
					queryBuf.append("(");
					queryBuf.append(annotationAlias + ".codeVisibility in ('" + Visibility.MEMBERS + "', '" + Visibility.PUBLIC + "')");
					addWhere = addWhereOrAnd(addWhere, queryBuf);
					queryBuf.append(annotationAlias + ".idSecurityGroup ");
					appendMemberInStatement(queryBuf, this.groupsMemVisibility);
					queryBuf.append(")");
					
					hasSecurityCriteria = true;
					
				}

				
				// For annotations with MEMBER & COLLABORATOR visibility, limit to security group
				// in which user is collaborator, member, (or manager)
				if (!this.groupsMemCollabVisibility.isEmpty()) {
					
					addWhere = addWhereOrOr(addWhere, queryBuf);
					
					queryBuf.append("(");
					queryBuf.append(annotationAlias + ".codeVisibility = '" + Visibility.MEMBERS_AND_COLLABORATORS + "'");
					addWhere = addWhereOrAnd(addWhere, queryBuf);
					queryBuf.append(annotationAlias + ".idSecurityGroup ");
					appendMemberInStatement(queryBuf, this.groupsMemCollabVisibility);
					queryBuf.append(")");

					hasSecurityCriteria = true;
				}
				

				if (annotationGroupingAlias != null && !this.groupsMemVisibility.isEmpty()) {

					// Also get annotation groups for those with no annotations attached directly to annotation group
					addWhere = addWhereOrOr(addWhere, queryBuf);
					
					queryBuf.append("(");

					queryBuf.append(annotationAlias + ".idAnnotation is NULL");
					
					addWhere = addWhereOrOr(addWhere, queryBuf);
					
					queryBuf.append(annotationGroupingAlias + ".idUser = " + user.getIdUser());
					
					queryBuf.append(")");

					hasSecurityCriteria = true;
				}
				

				// Get all user owned annotations
				if (hasSecurityCriteria) {
					addWhere = addWhereOrOr(addWhere, queryBuf);
				}
				appendUserOwnedHQLSecurity(queryBuf, annotationAlias, annotationGroupingAlias, addWhere);
				hasSecurityCriteria = true;
				
				
				// Include all public annotations if scope = ALL	
				if (scopeLevel.equals(this.ALL_SCOPE_LEVEL)) {

					if (hasSecurityCriteria) {
						addWhere = addWhereOrOr(addWhere, queryBuf);						
					}
					
					queryBuf.append("(");
					queryBuf.append(annotationAlias + ".codeVisibility = '" + Visibility.PUBLIC + "'");	
					queryBuf.append(")");					
				}
				queryBuf.append(")");					
				
			} 
		} else {
			throw new Exception("invalid scope level " + scopeLevel);
		}
		return addWhere;
	}
	
	@SuppressWarnings("unchecked")
	private void appendUserOwnedHQLSecurity(StringBuffer queryBuf, 
            								 String annotationAlias, 
            								 String annotationGroupingAlias, 
            								 boolean addWhere) throws Exception {
		queryBuf.append("(");
		
		queryBuf.append("(");
		queryBuf.append(annotationAlias + ".idUser = " + user.getIdUser());
		addWhere = addWhereOrOr(addWhere, queryBuf);
		queryBuf.append(annotationGroupingAlias + ".idUser = " + user.getIdUser());				
		queryBuf.append(")");		

		if (annotationGroupingAlias != null) {
			addWhere = addWhereOrOr(addWhere, queryBuf);
			queryBuf.append(annotationAlias + ".idAnnotation is NULL ");				
		}
		
		queryBuf.append(")");		
	}
	
	
	@SuppressWarnings("unchecked")
	private void appendMemberInStatement(StringBuffer queryBuf, HashMap securityGroupMap) {
		queryBuf.append(" in (");
		for(Iterator i = securityGroupMap.keySet().iterator(); i.hasNext();) { 
			Integer idSecurityGroup = (Integer)i.next();
			queryBuf.append(idSecurityGroup);				
			if (i.hasNext()) {
				queryBuf.append(",");
			}
		}
		queryBuf.append(")");
	}
	
	protected boolean addWhereOrAnd(boolean addWhere, StringBuffer queryBuf) {
		if (addWhere) {
			queryBuf.append(" WHERE ");
			addWhere = false;
		} else {
			queryBuf.append(" AND ");
		}
		return addWhere;
	}
	
	protected boolean addWhereOrOr(boolean addWhere, StringBuffer queryBuf) {
		if (addWhere) {
			queryBuf.append(" WHERE ");
			addWhere = false;
		} else {
			queryBuf.append(" OR ");
		}
		return addWhere;
	}

	public boolean isAdminRole() {
    	return isAdminRole;
    }

	public void setAdminRole(boolean isAdminRole) {
    	this.isAdminRole = isAdminRole;
    }	
	
	public String getUserName() {
		if (user != null ) {
			return user.getUserName();
		} else {
			return "";
		}
	}
	
	public Integer getIdUser() {
		if (user != null ) {
			return user.getIdUser();
		} else {
			return null;
		}
	}
	

	
	public void loadAuthorizedResources(Session sess) throws Exception {
		if (!scrutinizeAccess) {
			return;
		}
		
		// Start over if we have already loaded the resources
		if (!versionToAuthorizedAnnotationMap.isEmpty()) {
			versionToAuthorizedAnnotationMap.clear();
		}
		
		// Cache the authorized annotation ids of each genome version for this user
		AnnotationQuery annotationQuery = new AnnotationQuery();
		annotationQuery.runAnnotationQuery(sess, this);
		for (Organism organism : annotationQuery.getOrganisms()) {
			for (String genomeVersionName : annotationQuery.getVersionNames(organism)) {

				HashMap<Integer, QualifiedAnnotation> annotationMap = this.versionToAuthorizedAnnotationMap.get(genomeVersionName);
				if (annotationMap == null) {
					annotationMap = new HashMap<Integer, QualifiedAnnotation>();
					this.versionToAuthorizedAnnotationMap.put(genomeVersionName, annotationMap);
				}
				for (QualifiedAnnotation qa : annotationQuery.getQualifiedAnnotations(organism, genomeVersionName)) {
					annotationMap.put(qa.getAnnotation().getIdAnnotation(), qa);
				}
			}
		}

	}
	
	
	
	//
	//
	// AnnotSecurity methods
	//
	//

	public boolean isAuthorized(String genomeVersionName, Object annotationId) {
		// When annotation is loaded directly from file system, all annotations 
		// are shown.
		if (!scrutinizeAccess) { 
			return true;
		}

		// If the annotation id is not provided, block access
		if (annotationId == null) {
			return false;
		}

		// Get the hash map of annotation ids this user is authorized to view
		Map annotationMap = (Map)versionToAuthorizedAnnotationMap.get(genomeVersionName);

		// Returns true if annotation id is in hash map; otherwise, returns false
		return annotationMap.containsKey(annotationId);

	}
	
	public Map<String, Object> getProperties(String genomeVersionName, Object annotationId) {
		// When annotation is loaded directly from file system, annotations
		// don't have any additional properties
		if (!scrutinizeAccess) { 
			return null;
		}

		// If the annotation access is blocked, don't show
		// properties.
		if (!isAuthorized(genomeVersionName, annotationId)) {
			return null;
		}

		// Get the hash map of annotation ids this user is authorized to view
		Map<Integer, QualifiedAnnotation> annotationMap = versionToAuthorizedAnnotationMap.get(genomeVersionName);
		QualifiedAnnotation qa = annotationMap.get(annotationId);
		return qa.getAnnotation().getProperties();
	}

	


}
 