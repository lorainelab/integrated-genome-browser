package util
{
  import flash.events.Event;
  import flash.events.FocusEvent;
  import flash.events.KeyboardEvent;
  
  import mx.collections.ArrayCollection;
  import mx.collections.IList;
  import mx.collections.Sort;
  import mx.collections.SortField;
  import mx.collections.XMLListCollection;
  import mx.collections.errors.SortError;
  import mx.controls.listClasses.IListItemRenderer;
  import mx.core.IFactory;
  
  import spark.components.ComboBox;
  import spark.events.TextOperationEvent;
  
  //-------------------------------------------------------------------------------------------------
  // METADATA TAGS
  //-------------------------------------------------------------------------------------------------
  
  /**
   * If the sort is "ascending", then those items that are displayed in the ComboBox will be sorted in
   * ascending order. If the sort is "descending", then those items that are displayed in the ComboBox
   * will be sorted in descending order. If it is "none", then none of the items in the list will be
   * sorted. The default is "ascending".
   */ 
  [Style(name="sort", type="String", enumeration="ascending,descending,none")]
  
  /**
   * This class extends the Spark ComboBox and allows the dropdown to use HCI dictionaries and XML.
   * <p> 
   * It includes the following features:
   * <ul>
   * <li>The dataProvider may either be an ArrayCollection or XMLListCollection. When using HCI 
   * dictionaries, the valueField is typically "value" and displayField is typically "display". However, 
   * these may be changed where appropriate.</li>
   * <li>You may choose to show only active items + the first value provided to this control by setting 
   * isActiveEnabled to true.</li>
   * <li>You may define a "sort" style for the dropdown. By default it sorts the data in "ascending" 
   * order.</li>
   * <li>You may choose to filter out certain fields by defining a filter and filterField.</li>
   * </ul>
   */ 
  public class ComboBox extends spark.components.ComboBox implements mx.controls.listClasses.IListItemRenderer
  {
    //-----------------------------------------------------------------------------------------------
    // ATTRIBUTES
    //-----------------------------------------------------------------------------------------------
    /**
    * If this is true, when the user enters invalid data on commit, it clears out the data. Otherwise,
    * it leaves the data in the ComboBox, even though it is invalid. (Default = true)
    */ 
    public var clearInvalid:Boolean = true;         
    
    private var _displayField:String;               // The value in the dataProvider to be used for display.
    private var _editable:Boolean = true;           // Enables/Disables the ComboBox
    private var _filterField:String;                // A single attribute to filter out of the list.
    private var _filter:String;                     // Items that have this value will be filtered out of the list.
    private var initValue:String;                   // The very first value that was set for this ComboBox.
    private var _isActiveEnabled:Boolean = false;   // If this is true, then all fields (except the original value) whose isActive != 'Y' will be scrubbed.
    private var _unfilteredDataProvider:IList;      // The unfiltered dataProvider that was given to this ComboBox.
    private var _valueField:String;                 // The value in the dataProvider to be used for the set/return value.
    private var dataProviderLoaded:Boolean = false; // This boolean is used to load the dataProvider and refresh filters the first time it is loaded.
    private var _data:Object;                       // This is the value of the row that relates back to this object.
	  private var _dataField:String;                  // The name of the attribute that possesses the data.  Needed only for itemRenderers
    
    //-----------------------------------------------------------------------------------------------
    // METHODS
    //-----------------------------------------------------------------------------------------------
    
    // Main Methods ---------------------------------------------------------------------------------
    /**
     * Class constructor.
     */ 
    public function ComboBox()
    {
      super();
    }
    
    /**
    * This method filters the ComboBox's dataProvider based on the filter. (Note: It will only
    * filter if the dataProvider was of type XMLListCollection.)
    */ 
    private function filterDataProvider():void 
    { 
      // Don't do anything if a data provider doesn't exist...
      if (!_unfilteredDataProvider)
        return;
      
      // Don't scrub anything by default...
      super.dataProvider = _unfilteredDataProvider;
      
      // Only filter if we successfully scrubbed an XMLListCollection...
      if (_unfilteredDataProvider is XMLListCollection && _unfilteredDataProvider.length > 0) 
      {
        var filteredDataProvider:XMLListCollection = new XMLListCollection((XMLListCollection(_unfilteredDataProvider).copy()));
        for (var i:int = 0; i < filteredDataProvider.length; i++)
        {
          var item:Object = filteredDataProvider[i];
          
          //Optional filtering on final XML node of DataProvider
          if (_filterField && item[_valueField] != "" && item[_filterField] != _filter && 
              (!initValue || item[_valueField] != initValue)) 
          {
            filteredDataProvider.removeItemAt(i);
            i--;
          }
          
          //isActive filtering on final XML node of DataProvider
          if(_isActiveEnabled && item.@isActive && item.@isActive != "Y" && item[_valueField] != initValue) 
          {
              filteredDataProvider.removeItemAt(i);
              i--;		
          }
        }   			
        
        super.dataProvider = filteredDataProvider;
      }    		
    }
    
    /**
    * This method retrieves the ComboBoxFactory associated with this ComboBox so that it can be used
    * within a DataGrid.
    * 
    * @param dataProvider This is the XML/ArrayList for the ComboBox.
    * @param dataField This is the item in the DataGrid that this ComboBox relates to.
    * @param displayField This is the item in the dataProvider that will be used as the display.
    * @param valueField This is the item in the dataProvider that will be used as the value. 
    */ 
    public static function getFactory(dataProvider:Object, dataField:String, displayField:String = "@display", 
                                      valueField:String = "@value"):IFactory 
    {			
      return new ComboBoxFactory({dataProvider: dataProvider, displayField: displayField, valueField: valueField, dataField: dataField});	
    }
    
    /**
     * This method updates the sort on the ComboBox based on the defined sortType. If there is no
     * comboBox.dataProvider, or if sortType == "none", this method does nothing.
     * 
     * @param sortType One of the sort Style defined types (e.g., ascending, descending, none).
     * (The default is "ascending".)
     */ 
    private function updateSort(sortType:String = "ascending"):void
    {
      // Refresh the ComboBox.dataProvider Sort...
      if (sortType != null && sortType != "none" && dataProvider != null)
      {
        var descending:Boolean = (sortType == "descending");
        var sort:Sort = new Sort();
        sort.fields = [new SortField(_displayField, true, descending)];
        
        if (dataProvider is XMLListCollection || dataProvider is XMLList)
        {
          XMLListCollection(dataProvider).sort = sort;
          XMLListCollection(dataProvider).refresh();
        }
        else if (dataProvider is ArrayCollection)
        {
          ArrayCollection(dataProvider).sort = sort;
          ArrayCollection(dataProvider).refresh();
        }
      }
    }
    
    // Overridden Methods --------------------------------------------------------------------------- 
    
    override public function styleChanged(styleProp:String):void
    {
      super.styleChanged(styleProp);
      
      // Updates the sort only if the style property changed...
      if (styleProp && styleProp == "sort")
        updateSort(getStyle(styleProp));
    }
    
    override public function stylesInitialized():void
    {
      super.stylesInitialized();
      
      // The first time the properties are initialized, this updates the sort appropriately...
      if (getStyle("sort"))
        updateSort(getStyle("sort"));
    }
     
    override public function set dataProvider(value:IList):void
    {            
      // If no value was defined, don't do anything...
      if (!value)
        return;
	  
  	  if (value is ArrayCollection)
  	  {
  		  if (!_displayField)
        {
  		  	_displayField = "label";
          labelField = _displayField;
        }
  		  if (!_valueField)
  		  	_valueField = "value";
  	  }
  	  else
  	  {
  		  if (!_displayField)
        {
  			  _displayField = "@display";
          labelField = _displayField;
        }
  		  if (!_valueField)
  			  _valueField = "@value";
  	  }
      
      // Save a copy of the list
      this.labelField = _displayField;
      _unfilteredDataProvider = value;
      filterDataProvider();
      
      // Re-sort based on the new data provider
      if (getStyle("sort"))
        updateSort(getStyle("sort"));
      else
        updateSort();
      
      // Update the initially selected value
      this.value = initValue;
    }
    
    override protected function commitProperties():void
    {
      try
      {
        super.commitProperties();
      }
      catch (error:SortError)
      {
        if (clearInvalid)
        {
          // The user has entered a bad input, so clear it out.
          if (this.selectedIndex < 0 && this.textInput.text)
            this.textInput.text = "";
		  	this.selectedItem = null;
			this.selectedIndex = null;
        }
        // Otherwise, do nothing.
      }
      
      updateTextInput();
    }
    
    override protected function dataProvider_collectionChangeHandler(event:Event):void
    {
      super.dataProvider_collectionChangeHandler(event);
      
      // Once it is finally loaded, refresh the data provider only once.
      if (this.dataProvider.length > 0 && !dataProviderLoaded)
      {
        dataProviderLoaded = true;
        this.dataProvider = _unfilteredDataProvider;
      }
    }
    
    override public function get enabled():Boolean
    {
      // When setting the ComboBox.enabled to false, even the textInput becomes un-selectable.
      // So, if we are going to set this component as not editable, then we need to ignore 
      // the actual enabled setting and just use the editable. This successfully disables the
      // component, but allows us to make the textInput editable.
      if (!editable)
        return editable;
      return super.enabled;
    }
    
    override public function set errorString(value:String):void
    {
      super.errorString = value;
      commitProperties();   // This method is sometimes getting called before the errorString gets set. It updates the error string data.
    }
    
    // Getters/Setters ------------------------------------------------------------------------------
    /**
     * Changes the object in the DataGrid that this ComboBox relates to. It is only used when
     * using a Factory.
     */ 
    public function set data(value:Object):void 
    {
      this._data = value;
      this.value = value[_dataField];
    }
    
    /**
     * Retrieves the object in the DataGrid that this ComboBox relates to. It is only used when using
     * a Factory.
     */ 
    public function get data():Object 
    {
      return _data;
    }
    
    /**
     * Changes the ComboBox's dataField. This field is only used when using a Factory in a DataGrid. It
     * is the field in the DataGrid that corresponds to this particular ComboBox.
     */ 
    public function get dataField():String
    {
      return _dataField;
    }
    
    /**
     * Retrieves the ComboBox's dataField. This field is only used when using a Factory in a DataGrid. It
     * is the field in the DataGrid that corresponds to this particular ComboBox.
     */ 
    public function set dataField(value:String):void
    {
      _dataField = value;
    }
    
    /**
     * Returns the display of the ComboBox. This is a custom field that uses the displayField to determine
     * the appropriate string from the ComboBox's dataProvider to return.
     */ 
    public function get display():String
    {
      if (this.selectedItem)
        return String(this.selectedItem[_displayField]);
      return null;
    }
    
    /**
     * Retrieves the ComboBox's displayField. This is a custom field that defaults to using "@display"
     * in the ComboBox's dataProvider as the display item in the dropdown.
     */ 
    public function get displayField():String
    {
      return _displayField;
    }
    
    /**
     * Changes the ComboBox's displayField. This is a custom field that defaults to using "@display"
     * in the ComboBox's dataProvider as the display item in the dropdown.
     */ 
    public function set displayField(value:String):void
    {
      _displayField = value;
      this.labelField = _displayField;
      
      // Re-sort based on the new selection...
      if (getStyle("sort"))
        updateSort(getStyle("sort"));
      else
        updateSort();
    }
    
    /**
    * Disables the text & dropdown on the ComboBox but still allows the text to be selected.
    */ 
    [Bindable]
    public function get editable():Boolean
    {
      return _editable;
    }
    
    /**
    * Disables the text & dropdown on the ComboBox but still allows the text to be selected.
    */ 
    public function set editable(value:Boolean):void
    {
      // Only make a change if the value actually changed.
      if (value != _editable)
      {
        _editable = value;
        // If we've actually set this to be editable again, then we need to make sure that the
        // ComboBox.enabled flag gets reset. This will make sure that the component gets redrawn
        // with the appropriate enabled setting.
        if (value)
          enabled = super.enabled;
        updateTextInput();
      }
    }
    
    /**
    * Retrieves the filter that was used to filter out items from the ComboBox. Items that have a
    * matching filterField + filter will be scrubbed from the result set. (e.g., If you don't want
    * items that aren't from @idSite = 1, then pass 1 as the filter and @idSite as the filterField.)
    */ 
    public function get filter():String
    {
      return _filter;
    }
    
    /**
    * Changes the filter that will be used to filter out items from the ComboBox. Items that have a
    * matching filterField + filter will be scrubbed from the result set. (e.g., If you don't want
    * items that aren't from @idSite = 1, then pass 1 as the filter and @idSite as the filterField.)
    */ 
    public function set filter(value:String):void
    {
      _filter = value;
      filterDataProvider();
    }
    
    /**
     * Retrieves the filterField that was used to filter out items from the ComboBox. Items that have 
     * a matching filterField + filter will be scrubbed from the result set. (e.g., If you don't want
     * items that aren't from @idSite = 1, then pass 1 as the filter and @idSite as the filterField.)
     */ 
    public function get filterField():String
    {
      return _filterField;
    }
    
    /**
     * Changes the filterField that will be used to filter out items from the ComboBox. Items that have 
     * a matching filterField + filter will be scrubbed from the result set. (e.g., If you don't want
     * items that aren't from @idSite = 1, then pass 1 as the filter and @idSite as the filterField.)
     */ 
    public function set filterField(value:String):void
    {
      _filterField = value;
      filterDataProvider();
    }
    
    /**
    * Returns true if all fields whose isActive flag is not 'Y' have been scrubbed from the dataProvider.
    * Otherwise, it returns false. (Default is false.)
    */ 
    public function get isActiveEnabled():Boolean
    {
      return _isActiveEnabled;
    }
    
    /**
    * If this is set to true, then it will filter the dropdown based on whether the isActive flag is 'Y'
    * or not. If this is false, then it won't scrub. (Default is false.)
    */ 
    public function set isActiveEnabled(value:Boolean):void
    {
      if (_isActiveEnabled != value)
      {
        _isActiveEnabled = value;
        filterDataProvider();
      }
      else
        _isActiveEnabled = value;
    }
    
    /**
    * Retrieves the display value for the ComboBox.
    */ 
    public function get text():String
    {
      return display;
    }
    
    /**
    * Update the textInput associated with this ComboBox.
    */ 
    private function updateTextInput():void
    {
      // If the textInput hasn't been initialized yet, then don't do anything.
      if (!textInput)
        return;
      
      if (!editable) // This allows the ComboBox to have selectable text, but be completely disabled.
      {
        textInput.enabled = true;
        textInput.editable = editable;
      }
      else // This resets the ComboBox's textInput back to normal.
      {
        textInput.editable = enabled;
        textInput.enabled = enabled;
      }
      invalidateDisplayList();
    }
    
    /**
     * Retrieves the value of the ComboBox. This is a custom field that uses the valueField to determine
     * the appropriate string from the ComboBox's dataProvider to return.
     */ 
    public function get value():String
    {
      if (this.selectedItem)
        return String(this.selectedItem[valueField]);
      return null;
    }
    
    /**
     * Changes the ComboBox's selectedItem. This custom field uses the valueField to determine the 
     * appropriate item from the ComboBox's dataProvider to set as the currently selected item. 
     */ 
    public function set value(value:String):void
    {
      // This only gets set the first time...
      if (!initValue && value)
      {
        initValue = value;
        this.dataProvider = _unfilteredDataProvider; // Likely, the dataProvider wasn't fully loaded until this point...
      }
      
      // Select the value in the list...
      if (this.dataProvider && value)
      {
        this.selectedIndex = -1; // Deselected...
		    var i:int;
        
    		if (dataProvider is ArrayCollection)
    		{
    			for (i = 0; i < this.dataProvider.length && this.selectedIndex < 0; i++)
    			{
    				if (this.dataProvider.getItemAt(i)[valueField] == value)
    					this.selectedIndex = i;
    			}
    		}
    		else
    		{
	        for (i = 0; i < this.dataProvider.length && this.selectedIndex < 0; i++)
	        {
	          if (this.dataProvider.getItemAt(i).attribute(valueField.substr(1)) == value)
	            this.selectedIndex = i;
	        }
    		}
      }
      else
      {
        this.selectedIndex = -1; // Deselected...
      }
    }
    
    /**
     * Retrieves the ComboBox's valueField. This is a custom field that defaults to using "@value"
     * in the ComboBox's dataProvider as the value of the item in the dropdown.
     */ 
    public function get valueField():String
    {
      return _valueField;
    }
    
    /**
    * Changes the ComboBox's valueField. This is a custom field that defaults to using "@value"
    * in the ComboBox's dataProvider as the vale of the item in the dropdown.
    */ 
    public function set valueField(value:String):void
    {
      _valueField = value;
    }
  }
}

/**
 * This is the ComboBoxFactory that allows the ComboBox to be used from a DataGrid.
 */ 
class ComboBoxFactory implements mx.core.IFactory 
{
  private var properties:Object;
  
  public function ComboBoxFactory(properties:Object) 
  {
    this.properties = properties;
  }
  
  public function newInstance():* 
  {
    var cb:util.ComboBox = new util.ComboBox();
    
    cb.valueField = properties.valueField;
    cb.displayField = properties.displayField;
    cb.dataProvider = properties.dataProvider;
    cb.dataField = properties.dataField;
    
    return cb;	
  }
}
