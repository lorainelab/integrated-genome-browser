/**
*   Copyright (c) 2001-2005 Affymetrix, Inc.
*
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*   Distributions from Affymetrix, Inc., place this in the
*   IGB_LICENSE.html file.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.igb.das2;

import java.io.*;
import java.net.*;
import java.util.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import java.lang.Object.*;
import java.net.URI.*;
import java.util.regex.*;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.util.DasUtils;
import com.affymetrix.igb.util.SynonymLookup;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.das.DasLoader;


public class Das2VersionedSourcePlus extends Das2VersionedSource {
  Map assays = new LinkedHashMap();
  Map materials = new LinkedHashMap();
  Map results = new LinkedHashMap();
  boolean assays_initialized = false;
  boolean materials_initialized = false;
  boolean results_initialized = false;
  boolean platforms_initialized = false;
  String TYPES_QUERY = "types";

  LinkedList platforms = new LinkedList();

 // public Das2VersionedSourcePlus(Das2Source das_source, String version_id, boolean init) {
//    super(das_source, version_id, init);
//  }
    public Das2VersionedSourcePlus(Das2Source das_source, URI vers_uri, boolean init) {
        super(das_source, vers_uri, vers_uri.toString(), null, null, init);
    }

  public void addAssay(Das2Assay assay) {
      assays.put(assay.getID(), assay);
  }

  public void addResult(Das2Result result) {
      results.put(result.getID(), result);
  }

  public void addMaterial(Das2Material material) {
      materials.put(material.getID(), material);
  }

  public void addPlatform(Das2Platform _platform){
      platforms.add(_platform);
  }

  public Map getTypes(String filter) {
     if (! types_initialized || !filter.equals(types_filter)) {
       initTypes(filter, true);
    }
    return types;
  }

  public Map getAssays() {
      if(! assays_initialized) {
          initAssays();
      }
      return(assays);
  }

  public Map getResults() {
      if(! results_initialized) {
          initResults();
      }
      return(results);
  }

  public Map getMaterials() {
      if(! materials_initialized) {
          initMaterials();
      }
      return(materials);
  }

  public LinkedList getPlatforms(){
      if(! platforms_initialized) {
          initPlatforms();
      }
    return platforms;
  }


  public void clearAssays() {
      this.assays = new LinkedHashMap();
  }

  public void clearResults() {
      this.results = new LinkedHashMap();
  }

  public void clearMaterials() {
      this.materials = new LinkedHashMap();
  }

  public void clearPlatforms() {
      this.platforms = new LinkedList();
  }


  protected void initMaterials() {
    this.clearMaterials();
    String materials_request = getSource().getID() +
      "/" + this.getID() + "/material";
    try {
      System.out.println("Das Materials Request: " + materials_request);
      Document doc = DasLoader.getDocument(materials_request);
      Element top_element = doc.getDocumentElement();
      NodeList materialList = doc.getElementsByTagName("BioSource");
      System.out.println("materials: " + materialList.getLength());
      for (int i=0; i< materialList.getLength(); i++)  {
	Element materialnode = (Element)materialList.item(i);
        String materialid = materialnode.getAttribute("identifier");
        String name = materialnode.getAttribute("name");

        // types
	NodeList tlist = materialnode.getElementsByTagName("DatabaseEntry");
	HashMap types = new HashMap();
	for (int k=0; k<tlist.getLength(); k++) {
	  Element inode = (Element)tlist.item(k);
	  String uri = inode.getAttribute("URI");
          types.put(uri.substring(8), uri);
        }

        // contacts
        HashMap contacts = new HashMap();
	NodeList clist = materialnode.getElementsByTagName("Organization_ref");
	for (int k=0; k<clist.getLength(); k++) {
	  Element pnode = (Element)clist.item(k);
	  String id = pnode.getAttribute("identifier");
	  contacts.put(id.substring(11), id);
	}

	this.addMaterial(new Das2Material(this, materialid, name, types, contacts));
      }
    } catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS materials for\n"+materials_request, ex);
    }

    //example get:
    //http://das.biopackages.net/das/assay/mouse/6/material

    //TODO should types_initialized be true after an exception?
    materials_initialized = true;
  }

  protected void initResults() {
    this.clearResults();
    String results_request = getSource().getID() +
      "/" + this.getID() + "/result";
    try {
      System.out.println("Das Results Request: " + results_request);
      Document doc = DasLoader.getDocument(results_request);
      Element top_element = doc.getDocumentElement();
      NodeList resultlist = doc.getElementsByTagName("RESULT");
      System.out.println("results: " + resultlist.getLength());
      for (int i=0; i< resultlist.getLength(); i++)  {
	Element resultnode = (Element)resultlist.item(i);
        String resultid = resultnode.getAttribute("id");
        String assayId = resultnode.getAttribute("assay");
        assayId = assayId.substring(9);
        String imageId = resultnode.getAttribute("image");
        imageId = imageId.substring(9);
        String protocolId = resultnode.getAttribute("protocol");
        protocolId = protocolId.substring(12);

        //example get:
        //http://das.biopackages.net/das/assay/mouse/6/result

	this.addResult(new Das2Result(this, resultid, assayId, imageId, protocolId));
      }
    } catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS types for\n"+results_request, ex);
    }
    //TODO should types_initialized be true after an exception?
    results_initialized = true;
  }

  protected void initAssays() {
    this.clearAssays();
    String assays_request = getSource().getID() +
      "/" + this.getID() + "/assay";
    try {
      System.out.println("Das Assays Request: " + assays_request);
      Document doc = DasLoader.getDocument(assays_request);
      Element top_element = doc.getDocumentElement();
      NodeList assaylist = doc.getElementsByTagName("PhysicalBioAssay");
      System.out.println("assays: " + assaylist.getLength());
      for (int i=0; i< assaylist.getLength(); i++)  {
	Element assaynode = (Element)assaylist.item(i);
        String assayid = assaynode.getAttribute("identifier");

        // images
	NodeList ilist = assaynode.getElementsByTagName("Image");
	HashMap images = new HashMap();
	for (int k=0; k<ilist.getLength(); k++) {
	  Element inode = (Element)ilist.item(k);
	  String uri = inode.getAttribute("URI");
          if(uri.equals("")){/*do nothing if there is no image*/}
          else{images.put(uri.substring(9), uri);}
	}
        // biomaterials
        HashMap biomat = new HashMap();
	NodeList bmlist = assaynode.getElementsByTagName("BioMaterial_ref");
	for (int k=0; k<bmlist.getLength(); k++) {
	  Element pnode = (Element)bmlist.item(k);
	  String id = pnode.getAttribute("identifier");
	  biomat.put(id.substring(12), id);
	}

        // array platform
        HashMap platform = new HashMap();
        NodeList plist = assaynode.getElementsByTagName("Array_ref");
        for (int l=0; l<plist.getLength(); l++) {
           Element pnode = (Element)plist.item(l);
           String id = pnode.getAttribute("identifier");
           platform.put(id.substring(12), id);                                  //This is ok, substring() gets the string from position 2 to the end, so its brittle, but it seems to work ok
        }

	this.addAssay(new Das2Assay(this, assayid, images, biomat, platform));
      }
    } catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS types for\n"+assays_request, ex);
    }
    //TODO should types_initialized be true after an exception?
    assays_initialized = true;
  }

  // get annotation types from das server
  protected void initTypes(String filter, boolean getParents) {
    this.types_filter = filter;
    this.clearTypes();

    // how should xml:base be handled?
    //example of type request:  http://das.biopackages.net/das/assay/mouse/6/type?ontology=MA
    String types_request = this.getID() + "/" + TYPES_QUERY;

    if (filter != null) {
      types_request = types_request+"?ontology="+filter;
    }

    try {
      System.out.println("Das Types Request: " + types_request);
      Document doc = DasLoader.getDocument(types_request);
      Element top_element = doc.getDocumentElement();
      NodeList typelist = doc.getElementsByTagName("TYPE");
      System.out.println("types: " + typelist.getLength());
      int typeCounter = 0;

      //      ontologyStuff1();
      HashMap curOntology = new HashMap();
      HashMap uberTrackingHash = new HashMap();  //this is for tracking whether or not a node has been included in the list of nodes to draw/use later on...
      String ontoRootNodeId = new String() ;

      //I HAVE to get the relevant ontoNode for each typeNode down below
      //OR I can just prefetch the parents into a map up here beforehand...
      if (getParents) {
	String ontologyRequest = "http://das.biopackages.net/das/ontology/obo/1/ontology";
	//next I need a string like:
	//example of onto request:  http://das.biopackages.net/das/ontology/obo/1/ontology/MA
	if (filter != null) {
	  ontologyRequest = ontologyRequest+"/"+filter;
	}
	System.out.println("Das Ontology Request: " + ontologyRequest);
	Document ontoDoc = DasLoader.getDocument(ontologyRequest);
	Element top_OntoElement = ontoDoc.getDocumentElement();
	NodeList ontoTypeList = ontoDoc.getElementsByTagName("TERM");
	System.out.println("Onto Terms: " + ontoTypeList.getLength());
	HashMap parentHoldingHash = new HashMap();

	//now loop through the entire ontology and put the relevant reationships into this map
	for (int m=0; m< ontoTypeList.getLength(); m++)  {
	  Element ontoTypeNode = (Element)ontoTypeList.item(m);
	  String termID = ontoTypeNode.getAttribute("id");                        //I most certainly will have duplicate key issues (many parents per key)
	  //So I will make a map where the vals are ArrayLists
	  // temporary workaround for getting type ending, rather than full URI
	  if (termID.startsWith("./")) { termID = termID.substring(2); }          //do I still need to do this BS?

	  //I need a way to set a variable so that I KNOW which element is the ROOT node.
	  //So I think I will make a HashMap and then anytime a new parent comes along,
	  //I will then add it to the hash (automatically there will be no dups).
	  //THERE is already a list of all the non-parent terms in the curOntology hash.
	  //Therefore at the end, I will grab the thing that was in the parents list and not the other list...

	  NodeList localParentsList = ontoTypeNode.getElementsByTagName("PARENT");
	  for (int l=0; l<localParentsList.getLength(); l++) {

            //the pattern to look for gets compiled in here
            Pattern p = Pattern.compile("/");
            //init the matcher object
            Matcher match = p.matcher("");
            //reset the matcher with the string in question
            match.reset(termID);
            //Replace found characters with an empty string.
            termID = match.replaceAll(":");

            Element pnode = (Element)localParentsList.item(l);
	    //get the newest parent
	    String newParent = pnode.getAttribute("id");
	    //make a new List to hold all parents (old an new)

            //reset the matcher with the string in question
            match.reset(newParent);
            //Replace found characters with an empty string.
            newParent = match.replaceAll(":");

	    ArrayList parentsList = new ArrayList();
	    //add that newest parent to it
	    parentsList.add(newParent);
	    //if we had a parent for this before...

	    //now I need to make sure that ALL parents get added to the following list NO MATTER WHAT.
	    //ALSO, these objects must all contain
	    parentHoldingHash.put(newParent, newParent);    //will just hold a bunch of strings corresponding to the relevant IDs...

	    if(curOntology.containsKey(termID)){
	      //then we need to get the prev parents list out of there
	      ArrayList prevParents = new ArrayList();
	      //get out the existing list
	      prevParents = (ArrayList)curOntology.get(termID);
	      //add all those other parents to the newest list
	      for(int i=0;i<prevParents.size();i++){
		parentsList.add(prevParents.get(i));
	      }
	      //key and value pair
	      curOntology.put(termID, parentsList);
	    }                                            //need this to hold arraylists for each termID
	    //OR if this is the 1st time then just add it in there
	    else{
	      curOntology.put(termID, parentsList);               //key and value pair
	    }
	  }
	}

        //HERE I need to go through the two hashes and annoint a root node...
        //I want the ID of the node that is not shared between parentHoldingHash and the curOntology (not in curOntology)
        Iterator parentIt = parentHoldingHash.keySet().iterator();
        while(parentIt.hasNext()) {
	  String curId = (String)parentIt.next();
	  if(curOntology.containsKey(curId)){
	    //remove that value from the parentHoldingHash
	  }
	  else{
	    ontoRootNodeId = (String)parentHoldingHash.get(curId).toString();
	  }
        }
        System.out.println("the root node is:"+ontoRootNodeId);
      }    // end ontology setup


      for (int i=0; i< typelist.getLength(); i++)  {
	Element typenode = (Element)typelist.item(i);
        String typeid = typenode.getAttribute("id");                            // Gets the ID value
	//        String typeid = typenode.getAttribute("ontology");                            // Gets the ID value
        //FIXME: quick hack to get the type IDs to be kind of right (for now)

        // temporary workaround for getting type ending, rather than full URI
	if (typeid.startsWith("./")) { typeid = typeid.substring(2); }          //if these characters are one the beginning, take off the 1st 2 characters...
	//FIXME: quick hack to get the type IDs to be kind of right (for now)

        String ontid = typenode.getAttribute("ontology");
	String type_source = typenode.getAttribute("source");                   //PROBLEM!  This is missing too (no longer in this doc?)
	String href = typenode.getAttribute("doc_href");                        //ALSO MIA is this attribute  Are these needed?  I doubt they are needed YET
        String type_name = typenode.getAttribute("name");

	NodeList flist = typenode.getElementsByTagName("FORMAT");               //FIXME: I don't even know if these are in the XML yet.
	LinkedHashMap formats = new LinkedHashMap();                            //I don't think that this has ever been used yet.
	HashMap props = new HashMap();
	for (int k=0; k<flist.getLength(); k++) {
	  Element fnode = (Element)flist.item(k);
	  String formatid = fnode.getAttribute("id");
	  String mimetype = fnode.getAttribute("mimetype");
	  if (mimetype == null || mimetype.equals("")) { mimetype = "unknown"; }
          //	  System.out.println("alternative format for annot type " + typeid +
          //": format = " + formatid + ", mimetype = " + mimetype);
          formats.put(formatid, mimetype);
	}

	NodeList plist = typenode.getElementsByTagName("PROP");                 //What IS this?  I am not sure if this is used either.
	for (int k=0; k<plist.getLength(); k++) {
	  Element pnode = (Element)plist.item(k);
	  String key = pnode.getAttribute("key");
	  String val = pnode.getAttribute("value");
	  props.put(key, val);
	}
	//	ontologyStuff2();
        HashMap parents = new HashMap();

        if(getParents == true){
            //get the elements you need from the hash and then assign them as below
            //the value of ontid needs to be unescaped?  And then it needs to be trimmed of whatever base URI information its carrying 1st.
            //So I need to get some java code for dealing with that.

            if (ontid.startsWith("/das/ontology/obo/1/ontology/")) { ontid = ontid.substring(29); }
            //FIXME: this is a hack that deals with out ignorance of the namespace of these URIs
            //FIXME: this hack (and eventually the real code) could also be A LOT less ugly if I used the name attribute instead of the ontology one...
            ontid = URLDecoder.decode(ontid);
            //FIXME: another hack to deal with the fact that the string is escaped (this works pretty well actually)
            //OR I COULD just use/add from the "name" value from this tag (instead of gettting it from the ontology reference)

            //FIXME: if I used the name value in here (as suggested above) then I
            //wouldn't have to swap out the / for a :, but I would have to add some info onto the front of the string ie. (filter+":"+name)
            //the pattern to look for gets compiled in here
            Pattern p = Pattern.compile("/");
            //init the matcher object
            Matcher m = p.matcher("");
            //reset the matcher with the string in question
            m.reset(ontid);
            //Replace found characters with an empty string.
            ontid = m.replaceAll(":");

	    // OLDE way of doing this:
	    //    // temporarily we have to leave this here...  move this into the recursion later on...
	    //	  parents = getParentsMap(ontid, curOntology);
	    //	  Das2Type type = new Das2Type(this, typeid, ontid, type_source, href, formats, props, parents);
	    //	  this.addType(type);
	    //	  typeCounter++;

	    //New way of doing this:
            ArrayList newTypes = new ArrayList();      //the list of this node and its parents who all need to be collected into the cue at this time...
            parents = getParentsMap(ontid, curOntology);
            HashMap orphanTypes = new HashMap(); //map to hold the IDs and the parents of each ID that we later want to recover...
            recurseUntilAllParentsAreFound(this, typeid, ontid, curOntology, uberTrackingHash,
                    newTypes, ontoRootNodeId, parents, orphanTypes, type_source, href, formats, props);

            //the final thing will be to loop through throws Das2Types list and do the this.addType(i); for each one
            for(int t =0; t<newTypes.size();t++ ){
                Das2Type curType = (Das2Type)newTypes.get(t);
                this.addType(curType);
                typeCounter++;
            }
        }
        else if (getParents == false){
            //do not assign real vals to these
            NodeList parentsList = typenode.getElementsByTagName("PARENT");     //ignore this for now
            for (int l=0; l<parentsList.getLength(); l++) {                     //this is the old code just "cased out" for those
               Element pnode = (Element)parentsList.item(l);                    //cases where we don't have parent info coming back
               String key = "";                                                 //used to be = pnode.getAttribute("id");
               String val = "";                                                 //used to be = pnode.getAttribute("id"); as well
               parents.put(key, val);                                           //
            }                                                                   //
            Das2Type type = new Das2Type(this, new URI(typeid), type_name, ontid, type_source, href, formats, props, parents);
            this.addType(type);
        }

        //the Parent information has been GANKED out.  to fix it I will have to make
        //a NEW parent map based on the new information that has been put into the
        //Das 2 requests
        //http://das.biopackages.net/das/assay/mouse/6/type/
        //will return all the types (available to the mouse samples (based on annots that were USED)
        //
        //GET -e "http://das.biopackages.net/das/ontology/obo/1/ontology/MA/brain" | less
        //will return the ontology relationships that are relative to the term brain.
        //
        //GET -e "http://das.biopackages.net/das/ontology/obo/1/ontology" | less
        //returns the ontologys that are available
        //
        //GET -e "http://das.biopackages.net/das/ontology/obo/1/ontology/MA" | less
        //returns all the info from THAT particular ontology...
        //
	//        System.out.println("Here are the number of new types that have been made: "+typeCounter);

      }
    }
    catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS types for\n"+types_request, ex);
    }
    //TODO should types_initialized be true after an exception?
    types_initialized = true;
  }


//FIXME: Marc, what are the next three functions?  I don't know if they belong here
  // perhaps they need to be simplified?
protected void recurseUntilAllParentsAreFound(Das2VersionedSource _type, String _typeid, String _ontid,
        HashMap _curOntology, HashMap _uberTrackingHash, ArrayList _newTypes, String _ontoRootNodeId,
        HashMap _parents, HashMap _orphanTypes, String _type_source, String _href, LinkedHashMap _formats, HashMap _props){

    //the func must return a list of Das2Type's to add. (arraylist of Types), and take in the following:
    //1) _typeid  - this is the current Id
    //2) _ontid  - to ID parents, and also in the future this may diverge from the typeid so lets keep em separate here at least
    //4) _curOntology - for identifying parents
    //5) _uberTrackingHash - to spot whether or not something has been set up before (only set up nodes once)
    //6) _newTypes - is a list of new nodes that wil accumulate for each node
    //7) _parents - the parents associated with a given node, retrieved from the getParentsMap().
    //ALL OTHER Das2Type vals are prolly not important for the case where we worry about parents
    //(meaning nodes used for the gene expression jazz), but I am going to pass them in anyhow, and for
    //the parent nodes, I will just blank them out (ie. = "" since they cannot come from the XML)


    //if the curOntology hash has this then we need to check if it has parents or not
    if(_curOntology.containsKey(_typeid)){   //1st thing is it has to be in our onotology hash otherwise we might as well just return empty handed

        //instructions for adding nodes:
        if(!_uberTrackingHash.containsKey(_typeid)){    //if we don't have this in the _uberTrackingHash already...
                if(!_newTypes.contains(_typeid)){     //make sure we have not seen this element before (in this pass)
                    //1) make a Das2Type...
                    try  {
                      Das2Type type = new Das2Type(_type, new URI(_typeid), _typeid, _ontid,
                                                   _type_source, _href, _formats,
                                                   _props, _parents);
                      //2) add the current element to the ArrayList and use a bool to see if its the 1st time or not...
                      _newTypes.add(type);
                      //3) note that we added the element by adding it to the _uberTrackingHash and the
                      _uberTrackingHash.put(_typeid, type);
                    }
                    catch (Exception ex)  { ex.printStackTrace(); }
                }
        }

        //if it has more parents, then we need to do this some more (recurse down some)
        if(_ontoRootNodeId != _typeid){    //this means we are NOT at the root. (ontoRootNodeId is the root node for any ontology) - deduced above..
            //4) set up the typid ontid and the parents then set the other values to be = "";
            //The slightly tricky bit is that this has to be done for ALL parents:
            //this is tricky, because this will bypass some of the checks that I looked for earlier... (within this loop here)

            Iterator curParentIt = _parents.keySet().iterator();                //iterate over the parents for this node
            while(curParentIt.hasNext()) {                                         //a simple if makes this ignore the ones that come after...
                String curId = (String)curParentIt.next();

                //5) Before we call the function again, we have to set up so that its the right information
                _typeid = (String)_parents.get(curId).toString();    //something is happening HERE
                _ontid = _typeid;

                //reset the parents values for the next layer in.
                HashMap parents = new HashMap();                                //very important to make a new parents object each time (otherwise the other one loses values
                parents = getParentsMap(_ontid, _curOntology);
                setupBasicParentNodeVals( _typeid,  _ontid,  _curOntology,  parents,  _type_source,  _href,  _formats,  _props);

                //6) call this again.  But pass in values for a null node (parents as appropriate)
                recurseUntilAllParentsAreFound( _type,  _typeid,  _ontid, _curOntology, _uberTrackingHash, _newTypes, _ontoRootNodeId, parents, _orphanTypes,  _type_source,  _href,  _formats,  _props);
            }
        }
    }
}

protected void setupBasicParentNodeVals(String _typeid, String _ontid, HashMap _curOntology, HashMap _parents, String _type_source, String _href, LinkedHashMap _formats, HashMap _props){
    //null the rest out (just in case)
    _type_source = "" ;
    _href = "" ;
    LinkedHashMap formats = new LinkedHashMap();
    _formats = formats ;
    HashMap props = new HashMap();
    _props = props ;
}


protected HashMap getParentsMap(String _ontid, HashMap _curOntology){
    HashMap parents = new HashMap();
            //If there is anything that matches
            if(_curOntology.containsKey(_ontid)){
                //so there will be some number of parents retrieved from this hash
                ArrayList totalParents = new ArrayList();
                //so get that out
                totalParents = (ArrayList)_curOntology.get(_ontid);
                for(int j=0;j<totalParents.size();j++){
                    //FIXME: the following two things are the same items (wierd map being used as an ArrayList issue)
                    String key = (String)totalParents.get(j).toString();
                    parents.put(key, key);
                }
            }
            else{
                String key = ""; //the thing that you got from the hash;
                String val = ""; //the same thing from the hash;
                parents.put(key, val);
            }

    return parents;
}


  protected void initPlatforms(){
      this.clearPlatforms();
      String plat_request = getSource().getID() +
        "/" + this.getID() + "/platform";
    try {
      System.out.println("Current DAS platform Request: " + plat_request);
      Document doc = DasLoader.getDocument(plat_request);
      NodeList platlist = doc.getElementsByTagName("ArrayDesign");

      System.out.println("platforms: " + platlist.getLength());
      for (int i=0; i< platlist.getLength(); i++)  {
	Element platnode = (Element)platlist.item(i);
        String platId = platnode.getAttribute("identifier");

        this.addPlatform(new Das2Platform(this, platId));
      }

    } catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS types for\n"+plat_request, ex);
    }

    platforms_initialized = true;
  }


}
