package util
{
  import mx.collections.IList;
  import mx.controls.listClasses.IListItemRenderer;
  import mx.core.ClassFactory;
  import mx.core.IFactory;
  
  import spark.components.Label;
  
  /**
   * This class allows the DataGrid to show the results of the ComboCheck selection in a simple
   * label without the dropdown button. It also implements much of the code from DropdownLabel
   * so that they have a consistent look-and-feel.
   */ 
  public class ComboCheckLabel extends Label implements IListItemRenderer
  {
    //-----------------------------------------------------------------------------------------------
    // ATTRIBUTES
    //----------------------------------------------------------------------------------------------- 
    private var _data:Object; // This is the data that was passed into this object.
    
    /**
    * This is the XML child in the "data" that contains the list of selected items.
    */ 
    public var dataField:String;
    
    /**
    * This is the field within each child node in the "data" XML list that contains the value to be
    * compared against the dataProvider value.
    */ 
    public var dataFieldValue:String;
    
    /**
    * This is the list of items that was included in the ComboCheck dropdown.
    */ 
    public var dataProvider:IList;
    
    /**
    * If displayMultipleItems is false, then this is the string that will be used instead of listing
    * all items if multiple items are selected.
    */
    public var displayDefaultMultiple:String;
    
    /**
    * This is the field within the dataProvider that will be used as the display in the list.
    */ 
    public var displayField:String;
    
    /**
     * If this is true, then multiple items will be displayed and separated by commas, rather than
     * some "multiple" text.
     */ 
    public var displayMultipleItems:Boolean;
    
    /**
    * This is the value within the dataProvider that will be used as the value for each item in the 
    * list.
    */ 
    public var valueField:String;
    
    //-----------------------------------------------------------------------------------------------
    // METHODS
    //-----------------------------------------------------------------------------------------------
      
    // Public Methods -------------------------------------------------------------------------------
    /**
     * Class constructor.
     */ 
    public function ComboCheckLabel()
    {
      super();
    } 
    
    /**
     * This method retrieves the ComboCheckFactory associated with this ComboCheckLabel so that it can be used
     * within a DataGrid.
     * 
     * @param dataProvider This is the XML/ArrayList for the ComboBox.
     * @param dataField This is the name of the sub-element in the data that contains checked values. For 
     * example, if you want to preset a list of checked specimen classifications, and your DataGrid dataProvider
     * looks something like this: &lt;Sample&gt;&lt;classifications&gt;&lt;Classification&gt;, you would pass 
     * "classifications". Flex will pass the entire row of sample data to the ComboCheck.data field for
     * processing. By providing "classifications", this class can pick out the proper sub-tag for comparison.
     * @param dataFieldValue This is the item in the dataField sub-tag that contains the value to compare 
     * against the dataProvider's valueField. Using the previous example, this might be something like
     * "idClassification". This method will search through the dataProvider for any valueFields that match
     * the &lt;Sample&gt;&lt;classification&gt;&lt;Classification idClassification&gt;
     * @param displayField This is the item in the dataProvider that will be used as the display.
     * @param valueField This is the item in the dataProvider that will be used as the value.
     * 
     * @param displayMultipleItems If true, all items checked will be displayed in a comma delimited string.
     * @param displayDefaultMultiple If displayMultipleItems is false, this is the string that will be used 
     * if more than one item is selected.
     * @param wordWrap If this is true, the label will wrap across multiple lines, otherwise, the text gets truncated. 
     * (The default is 'false'.)
     */ 
    public static function getFactory(dataProvider:IList, dataField:String, dataFieldValue:String = "@value", 
                                      displayField:String = "@display", valueField:String = "@value", 
                                      displayMultipleItems:Boolean = false, displayDefaultMultiple:String = "<multiple>",
                                      wordWrap:Boolean=false):IFactory 
    {			      
      var factory:ClassFactory = new ClassFactory(ComboCheckLabel);
      factory.properties = {dataProvider:dataProvider, dataField:dataField, dataFieldValue:dataFieldValue, 
        displayField:displayField, valueField:valueField, displayMultipleItems:displayMultipleItems, 
        displayDefaultMultiple:displayDefaultMultiple};
      if (!wordWrap)
        factory.properties.maxDisplayedLines = 1;
      return factory;
    }
    
    // Overridden Methods ---------------------------------------------------------------------------
    override protected function updateDisplayList(unscaledWidth:Number, unscaledHeight:Number):void
    {
      super.updateDisplayList(unscaledWidth, unscaledHeight);
      if (this.isTruncated)
        this.toolTip = this.text;
      else
        this.toolTip = null;
    }
    
    // Getters/Setters ------------------------------------------------------------------------------    
    /**
    * Retrieves the data associated with this label's comboCheck control.
    */ 
    public function get data():Object
    {
      return _data;
    }
    
    /**
    * Changes the data associated with this label's comboCheck control.
    */ 
    public function set data(value:Object):void
    {
      _data = value;
      var comboCheck:ComboCheck = new ComboCheck();
      comboCheck.dataProvider = dataProvider;
      comboCheck.dataField = dataField;
      comboCheck.initialValueField = dataFieldValue;
      comboCheck.displayField = displayField;
      comboCheck.valueField = valueField;
      comboCheck.displayMultipleItems = displayMultipleItems;
      comboCheck.displayDefaultMultiple = displayDefaultMultiple;
      comboCheck.data = value;
      this.text = comboCheck.text;
    }
  }
}