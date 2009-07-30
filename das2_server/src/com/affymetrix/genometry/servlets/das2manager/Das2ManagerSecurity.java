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


public class Das2ManagerSecurity {
	
	public static final String    SESSION_KEY = "Das2SecurityManager";
	public static final String    ADMIN_ROLE  = "das2admin";
	public static final String    USER_ROLE   = "das2user";
	public static final String    GUEST_ROLE  = "das2guest";

	public static final String    USER_SCOPE_LEVEL  = "USER";
	public static final String    GROUP_SCOPE_LEVEL = "GROUP";
	public static final String    ALL_SCOPE_LEVEL   = "ALL";
	
	
	private User                    user;
	private boolean                isAdminRole;
	private boolean                isGuestRole;
	
	
	private HashMap<Integer, SecurityGroup>   groupsMemCollabVisibility = new HashMap<Integer, SecurityGroup>();
	private HashMap<Integer, SecurityGroup>   groupsMemVisibility = new HashMap<Integer, SecurityGroup>();
	
	private HashMap<String, HashMap<Integer, String>> versionToAuthorizedResourceMap = new HashMap<String, HashMap<Integer, String>>();
	
	
	@SuppressWarnings("unchecked")
	public Das2ManagerSecurity(Session sess, String userName, boolean isAdminRole, boolean isGuestRole) throws Exception {
		
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
	
	public Document getXML() {
		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("Das2ManagerSecurity");
		root.addAttribute("userName",        user.getUserName());
		root.addAttribute("userDisplayName", user.getUserDisplayName());
		root.addAttribute("name",            user.getName());
		root.addAttribute("isAdmin",         isAdminRole ? "Y" : "N");
		root.addAttribute("isGuest",         isGuestRole ? "Y" : "N");
		root.addAttribute("canManageUsers",  isAdminRole || user.getManagingSecurityGroups().size() > 0 ? "Y" : "N");
		
		
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
	
	public HashMap<String, HashMap<Integer, String>> getAuthorizedResources() {
		return this.versionToAuthorizedResourceMap;
	}
	
	public void loadAuthorizedResources(Session sess) throws Exception {
		// Start over if we have already loaded the resources
		if (!versionToAuthorizedResourceMap.isEmpty()) {
			versionToAuthorizedResourceMap.clear();
		}
		
		// Cache the authorized annotation ids of each genome version for this user
		AnnotationQuery annotationQuery = new AnnotationQuery();
		annotationQuery.runAnnotationQuery(sess, this);
		for (String organismName : annotationQuery.getOrganismNames()) {
			for (String genomeVersionName : annotationQuery.getVersionNames(organismName)) {

				HashMap<Integer, String> authorizedAnnotationIdMap = this.versionToAuthorizedResourceMap.get(genomeVersionName);
				if (authorizedAnnotationIdMap == null) {
					authorizedAnnotationIdMap = new HashMap<Integer, String>();
					this.versionToAuthorizedResourceMap.put(genomeVersionName, authorizedAnnotationIdMap);
				}
				for (QualifiedAnnotation qa : annotationQuery.getQualifiedAnnotations(organismName, genomeVersionName)) {
					String resourceName = null;
					if (qa.getTypePrefix() == null) {						
						resourceName = qa.getAnnotation().getName();
					} else {
						resourceName = qa.getTypePrefix();
					}
					authorizedAnnotationIdMap.put(qa.getAnnotation().getIdAnnotation(), resourceName);
				}
			}
		}

	}

	public boolean isAuthorizedResource(String genomeVersionName, Object annotationId) {
	  
	  // If the annotation id is not provided, block access
	  if (annotationId == null) {
	    return false;
	  }

	  // Get the hash map of annotation ids this user is authorized to view
	  Map authorizedAnnotationIdMap = (Map)versionToAuthorizedResourceMap.get(genomeVersionName);

	  // Returns true if annotation id is in hash map; otherwise, returns false
	  return authorizedAnnotationIdMap.containsKey(annotationId);

	}
	
	public Map<Integer, ?> getAuthorizedAnnotationIds(String genomeVersionName) {
		return versionToAuthorizedResourceMap.get(genomeVersionName);
	}
	


}
 