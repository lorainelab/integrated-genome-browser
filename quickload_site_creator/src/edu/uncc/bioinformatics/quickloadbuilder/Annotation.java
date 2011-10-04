/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uncc.bioinformatics.quickloadbuilder;

import java.awt.Color;
import java.io.File;
import java.net.URI;
import java.net.URL;
import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataListener;

/**
 *
 * @author jfvillal
 */
public class Annotation{
    //**Negative one means the tag won't be saved. 
    public static final int NAME_SIZE_DEFAULT_SIZE=-1;
    public static final int MAX_DEPTH_DEFAULT=-1;
    //tag names used by annot.xml standard
    //description options
    static final String FILES_TAG = "files";
    static final String FILE_TAG  = "file";
    static final String NAME_TAG = "name";
    static final String TITLE_TAG = "title";
    static final String DESCRIPTION_TAG = "description";
    static final String URL_TAG = "url";
    //static final String SERVER_URL_TAG = "serverURL";
    static final String LOAD_HINT_TAG = "load_hint";
    static final String LABEL_FIELD_TAG = "label_field";
    
    //appearance options
    static final String BACKGROUND = "background";//color 
    static final String MAX_DEPTH = "max_depth"; //integer TODO add this one
    static final String NAME_SIZE = "name_size";//integer TODO 
    static final String CONNECTED = "connected";//boolean
    static final String COLLAPSED = "collapsed";//boolean
    static final String SHOW_2_TRACKS = "show2tracks";  //boolean 
    static final String DIRECTION_TYPE = "direction_type";//fixed choice
    static final String POSITIVE_STRAND_COLOR = "positive_strand_color";//color
    static final String NEGATIVE_STRAND_COLOR = "negative_strand_color";//color
    static final String VIEW_MOD = "view_mode"; //added 
    
    
    public File FAttribute;
    public String Title;
    public String Description;
    public URI Url;
    public String Label;
    public boolean LoadHint;
    public float NameSize;
    public int MaxDepth;
    
    public String DirectionType;
    public Color BackgroundColor;
    public Color PositiveStrandColor;
    public Color NegativeStrandColor;
    public String ViewMode;
    
    public Boolean Connected;
    public Boolean Collapsed;
    public Boolean ShowTracks;
    
    
    public static boolean isLoadHint(String str){
        return "Whole Sequence".equals(str);
    }
    public Annotation( File file ){
        FAttribute = file;
        Title = file.getName();
        Description = "";
        Url = null;
        Label = "";
        LoadHint = false;
        DirectionType = DirectionTypeValues[0];
        BackgroundColor = null;
        PositiveStrandColor = null;
        NegativeStrandColor = null;
        ViewMode = ViewModeValues[0];
        
        Connected = false;
        Collapsed = false;
        ShowTracks = false;
        NameSize = 12.0f;
        MaxDepth = 10;
                
    }
    public String getName(){
        return FAttribute.getName();
    }
    public static final String[] DirectionTypeValues = { "arrow" };
    /**
     * Returns the index of the Direction Type string or 
     * -1 if the value is not in the standard.
     * @param value
     * @return 
     */
    public static int getIndexofDirectionType( String value ){
        for(int i = 0; i < DirectionTypeValues.length; i++){
            String str = DirectionTypeValues[i];
            if( str.equals( value )){
                return i;
            }
        }
        return -1;
    }
    
    public static final String[] ViewModeValues = { "depth" };
    
    public static int getIndexofViewMode( String value ){
        for(int i = 0; i < ViewModeValues.length; i++){
            String str = ViewModeValues[i];
            if( str.equals( value )){
                return i;
            }
        }
        return -1;
    } 
    
    @Override
    public String toString(){
        return TITLE_TAG + ": " + this.Title;
    }
}
