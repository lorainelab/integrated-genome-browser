/*
 * Das2AssayVersionedSource.java
 *
 * Created on November 3, 2005, 3:42 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.affymetrix.igb.das2.assay;

import com.affymetrix.igb.das2.*;


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

/**
 *
 * @author Marc Carlson
 *
 */
public class Das2AssayVersionedSource extends Das2VersionedSource {

  
    boolean platforms_initialized = false;    
    LinkedList platforms = new LinkedList(); 
          
    // hash to convert ID to name
    HashMap typeIdToName = new HashMap();
  
    /** Creates a new instance of Das2AssayVersionedSource */
    public Das2AssayVersionedSource(Das2AssaySource das_source, String version_id, boolean init) {
        super(das_source, version_id, init);
    }
    
    
    //overridden methods    
    public Map getTypes() {
        if (! this.types_initialized || this.types_filter != null) {
            initTypes(null, false);
        }
        return this.types;
    }

    public Map getTypes(String filter) {
        if (! this.types_initialized || !filter.equals(this.types_filter)) {
            initTypes(filter, true);
        }
        return this.types;
    }
    
    // FIXME: Marc, please remove any hardcoded server strings here
    //        Also, you need to rework the way that "id" and "ontology"
    //        fields are treated.  You're relying on the id and ontology
    //        fields being the same/stable and the id is not.  You can't
    //        count on the id field being in this format, it's just a label.
    //        Finally, the init_types method will be much simplier when the 
    //        new obo format is used since the parent/child relationships are
    //        no longer included.  You can refactor this method up to a higher
    //        level when everything uses the common obo xml.
    // get annotation types from das server
    protected void initTypes(String filter, boolean getParents) {
    this.types_filter = filter;
    this.clearTypes();

    // how should xml:base be handled?
    //example of type request:  http://das.biopackages.net/das/assay/mouse/6/type?ontology=MA
    String types_request = getSource().getServerInfo().getRootUrl() +
        "/" + this.getID() + "/type";
    

    //example of ontology request:  http://das.biopackages.net/das/ontology/obo/1/ontology/
    String ontologyRequest = ((Das2AssayServerInfo)(getSource().getServerInfo() )).getRootOntologyUrl();   
    
    if (filter != null) {
      types_request = types_request+"?ontology="+filter;
      //next I need a string like:
      //example of onto request:  http://das.biopackages.net/das/ontology/obo/1/ontology/MA?format=legacy0
      ontologyRequest = ontologyRequest+filter;
    }
    ontologyRequest = ontologyRequest+"?format=legacy0";

    
    //    String types_request = "file:/C:/data/das2_responses/alan_server/types_short.xml";
    try {   
      System.out.println("Das Ontology Request: " + ontologyRequest);
      System.out.println("Das Types Request: " + types_request);
      Document doc = DasLoader.getDocument(types_request);
      Document ontoDoc = DasLoader.getDocument(ontologyRequest);
      Element top_element = doc.getDocumentElement();
      Element top_OntoElement = ontoDoc.getDocumentElement();
      String typeServerName = top_element.getAttribute("xml:base");
      //Get the actual Server name
      Matcher mat = Pattern.compile("^http://.+?/+").matcher(typeServerName);
      if(mat.find()){ //this always has to be tested for
              typeServerName = typeServerName.substring(mat.start(), (mat.end()-1) );
      }  
      String ontologyBaseURL = top_OntoElement.getAttribute("xml:base");
      NodeList typelist = doc.getElementsByTagName("TYPE");
      NodeList ontoTypeList = ontoDoc.getElementsByTagName("TERM");      
      System.out.println("types: " + typelist.getLength());
      System.out.println("Onto Terms: " + ontoTypeList.getLength());

      HashMap curOntology = new HashMap();
      HashMap parentHoldingHash = new HashMap();

      //this is for tracking whether or not a node has been included in the list of nodes to draw/use later on...
      HashMap uberTrackingHash = new HashMap();     
      
      String ontoRootNodeId = new String();
      int typeCounter = 0;  

      //Loop through the entire ontology and put the relevant relationships into this map
      for (int m=0; m< ontoTypeList.getLength(); m++)  {
        Element ontoTypeNode = (Element)ontoTypeList.item(m);
        String termID = ontoTypeNode.getAttribute("id"); 
        // FIXME: could include relative URL here too
        if (ontoTypeNode.getAttribute("name") != null && ontoTypeNode.getAttribute("name") != "") {
            typeIdToName.put(termID, ontoTypeNode.getAttribute("name")); 
        }

        // loop through all the parents of this node

        NodeList localParentsList = ontoTypeNode.getElementsByTagName("PARENT");    
        for (int l=0; l<localParentsList.getLength(); l++) {                    

            Element pnode = (Element)localParentsList.item(l);                   
           //get the newest parent
           String newParent = pnode.getAttribute("id");
           if (pnode.getAttribute("name") != null  && pnode.getAttribute("name") != "") { 
               typeIdToName.put(newParent, pnode.getAttribute("name")); 
           }
           //newParent = ontologyBaseURL+newParent;  // WTF!?!?!?!
           //make a new List to hold all parents (old an new)

           ArrayList parentsList = new ArrayList(); // FIXME: WRONG?!!!                              
           //add that newest parent to it
           parentsList.add(newParent);
           //if we had a parent for this before...

           //ALSO, these objects must all contain 
           //will just hold a bunch of strings corresponding to the relevant IDs...
           parentHoldingHash.put(newParent, newParent);    

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
           }                                                             
           //OR if this is the 1st time then just add it in there
           else{
             curOntology.put(termID, parentsList);                                                 
           }
        }  
      }
      
      // the loop through all the ontology terms & direct parents should be done
      // next, look at the types for this versioned source

        //Cycle through the two hashes and annoint a root node...
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

      //Finally, go through the types and find (match up from the above hashes) parents for each one
      for (int i=0; i< typelist.getLength(); i++)  {     
        Element typenode = (Element)typelist.item(i);                                   
        String typeid = typenode.getAttribute("ontology");//id");   
        if (typenode.getAttribute("name") != null  && typenode.getAttribute("name") != "") {
            typeIdToName.put(typeid, typenode.getAttribute("name")); 
        }
        // So above everything is coming back with http://<fullurl>/MA/1232
        // I've changed this to be MA/123213
        String[] tokens = typeid.split("/");
        typeid = tokens[tokens.length-2]+"/"+tokens[tokens.length-1];
        //typeid = typeServerName+typeid; // FIXME: WTF!?!?!
              
        String type_name = typenode.getAttribute("name");
        String type_source = ""; // not included anymore
        String href = "";

        NodeList flist = typenode.getElementsByTagName("FORMAT");               
        LinkedHashMap formats = new LinkedHashMap();                            
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

        NodeList plist = typenode.getElementsByTagName("PROP");                 
        for (int k=0; k<plist.getLength(); k++) {
          Element pnode = (Element)plist.item(k);
          String key = pnode.getAttribute("key");
          String val = pnode.getAttribute("value");
          props.put(key, val);
        }

        // HERE!!!
        
        HashMap parents = new HashMap();

        if(getParents == true){

            //this just guarantees that the string is unescaped... (safety feature)           
            typeid = URLDecoder.decode(typeid);


            //The following calls get the relevant type nodes needed to make a tree:            
            ArrayList newTypes = new ArrayList();      //the list of this node and its parents who all need to be collected into the cue at this time...    
            parents = getParentsMap(typeid, curOntology);
            HashMap orphanTypes = new HashMap(); //map to hold the IDs and the parents of each ID that we later want to recover...
            recurseUntilAllParentsAreFound(this, typeid, typeid, curOntology, uberTrackingHash, 
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
            NodeList parentsList = typenode.getElementsByTagName("PARENT");     
            for (int l=0; l<parentsList.getLength(); l++) {                     
               Element pnode = (Element)parentsList.item(l);                    
               String key = "";                                                 
               String val = "";                                                 
               parents.put(key, val);                                          
            }  
            // FIXME: I think the id and ontology fields should be switched here
            Das2Type type = new Das2Type(this, typeid, (String)typeIdToName.get(typeid), type_source, href, formats, props, parents);
            this.addType(type);     
        }

        // relevant Example Das 2 requests:
        //GET -e "http://das.biopackages.net/das/assay/mouse/6/type/" | less 
        //will return all the types (available to the mouse samples of this build)
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
        //GET -e "http://das.biopackages.net/das/ontology/obo/1/ontology/MA?format=obo" | less
        //returns THAT ontology in OBO format...
        //
        //GET -e "http://das.biopackages.net/das/ontology/obo/1/ontology/MA?format=legacy0" | less
        //returns THAT ontology in OBO format...
        //
        
        System.out.println("Here are the number of new types that have been made: "+typeCounter);
      }
    }
    catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS types for\n"+types_request, ex);
    }
    //TODO should types_initialized be true after an exception?
    this.types_initialized = true;
    }


    protected void recurseUntilAllParentsAreFound(Das2AssayVersionedSource _type, String _typeid, String _ontid, 
            HashMap _curOntology, HashMap _uberTrackingHash, ArrayList _newTypes, String _ontoRootNodeId, 
            HashMap _parents, HashMap _orphanTypes, String _type_source, String _href, LinkedHashMap _formats, HashMap _props){

        //the func must return a list of Das2Type's to add. (arraylist of Types), and take in the following:
        //1) _typeid  - this is the current Id
        //2) _ontid  - to ID parents, and also in the future this may diverge from the typeid so lets keep em separate here at least
        //4) _curOntology - for identifying parents 
        //5) _uberTrackingHash - to spot whether or not something has been set up before (only set up nodes once)
        //6) _newTypes - is a list of new nodes that wil accumulate for each node
        //7) _parents - the parents associated with a given node, retrieved from the getParentsMap().

        //if the curOntology hash has this then we need to check if it has parents or not
        if(_curOntology.containsKey(_typeid)){   //1st thing is it has to be in our onotology hash otherwise we might as well just return empty handed

            //instructions for adding nodes:
            if(!_uberTrackingHash.containsKey(_typeid)){    //if we don't have this in the _uberTrackingHash already...        
                    if(!_newTypes.contains(_typeid)){     //make sure we have not seen this element before (in this pass)
                        //1) make a Das2Type...
                        // FIXME: the id and ontology id are probably reversed here
                        Das2Type type = new Das2Type(_type, _typeid, (String)typeIdToName.get(_typeid), _type_source, _href, _formats, _props, _parents);
                        //2) add the current element to the ArrayList and use a bool to see if its the 1st time or not...
                        _newTypes.add(type);
                        //3) note that we added the element by adding it to the _uberTrackingHash and the
                        _uberTrackingHash.put(_typeid, type);
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

    
  
    /**
     *      This is to initialize potential platform values for clients that need them
     **/

    public LinkedList getPlatforms(){
        if(! platforms_initialized) {
            initPlatforms();
        }    
        return platforms;
    }    
  
    public void clearPlatforms() {
        this.platforms = new LinkedList();
    }

    public void addPlatform(Das2Platform _platform){
        platforms.add(_platform);
    }  
    
    protected void initPlatforms(){
        this.clearPlatforms();
        String plat_request = getSource().getServerInfo().getRootUrl() +
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
