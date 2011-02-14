package util
{
  import flash.events.Event;
  import flash.events.KeyboardEvent;
  import flash.events.MouseEvent;
  import flash.ui.Keyboard;
  
  import util.ComboBox;
  import util.ComboCheckItemRenderer;
  
  import mx.collections.ArrayCollection;
  import mx.collections.ArrayList;
  import mx.collections.IList;
  import mx.collections.XMLListCollection;
  import mx.core.ClassFactory;
  import mx.core.IFactory;
  import mx.core.INavigatorContent;
  import mx.events.DropdownEvent;
  import mx.events.FlexEvent;
  import mx.events.ItemClickEvent;
  
  import spark.components.CheckBox;
  import spark.events.DropDownEvent;
  import spark.events.IndexChangeEvent;
  
  /**
  * This event is launched any time the user checks an item in the list.
  */ 
  [Event("addItem", type="flash.events.Event")]
  
  /**
  * This event is launched any time the user unchecks an item in the list.
  */ 
  [Event("removeItem", type="flash.events.Event")]
  
  /**
  * This event is launched any time the user chooses to check all items in the list.
  */ 
  [Event("selectAll", type="flash.events.Event")]
  
  /**
  * This event is launched any time the user chooses to uncheck all items in the list.
  */ 
  [Event("deselectAll", type="flash.events.Event")]
  
  /**
  * This class extends the existing ComboBox to use HCI Dictionaries, and allows the user to make multiple 
  * selections using a CheckBox.
  * <p> 
  * In addition to the regular ComboBox, this control also includes the following features:
  * <ul>
  * <li>Instead of modifying the list itself to show what is checked/unchecked, the ComboCheck maintains a 
  * list of checked items in checkedItems. This list is either an ArrayCollection or XMLListCollection 
  * (depending on what the dataProvider type is).</li>
  * <li>The user can check/uncheck items by either selecting them with the mouse, or by pressing the Space 
  * Bar.</li>
  * <li>A list of pre-checked items may be included by passing in initialValues. By default, the valueField 
  * of this list is the valueField of the dataProvider; however, this may be changed by setting the 
  * initialValueField instead.</li>
  * <li>You may change the default "&lt;multiple&gt;" text string to whatever text you want (by changing 
  * displayDefaultMultiple). Or, you may choose to have it display all selected items in a comma-delimited 
  * list (by setting displayMultipleItems to true). (Note: If you are displaying a comma-delimited list, it 
  * will change the toolTip based on the number of items selected.)</li>
  * <li>If you add a "Select All" node to the dataProvider list, set the valueSelectAll so the control knows
  * which item to use as the select-all item. (Note: If you are using a dictionary, this is typically the 
  * value in the "value" field. However, you may also define this to be another attribute by changing 
  * valueField.)</li>
  * </ul>
  */ 
  public class ComboCheck extends ComboBox
  {
    // -----------------------------------------------------------------------------------------------------
    // ATTRIBUTES
    // -----------------------------------------------------------------------------------------------------
    private var _checkedItems:IList;                          // The list of items that are actually selected.
    private var _data:Object;                                 // The data passed in from a factory.
    private var _display:String;                              // The display text for the ComboBox.
    private var _displayDefaultMultiple:String;               // Default text used when multiple items are selected.
    private var _displayMultipleItems:Boolean;                // Determines whether the default multiple text will be used, or a comma-delimited string..
    private var _initialValues:IList;                         // Initial values pre-selected by the user.
    private var _initialValueField:String;                    // The valueField to use for the initial values list.
    private var _proposedSelectedIndex:Number = NO_SELECTION; // Proposed selected index based on what the user currently has selected in the dropdown. 
    private var _valueSelectAll:String;                       // Value of the item in the list that is used as the select/deselect all
    
    // -----------------------------------------------------------------------------------------------------
    // METHODS
    // -----------------------------------------------------------------------------------------------------
    
    // Public Methods --------------------------------------------------------------------------------------
    /**
    * Constructor: This method adds event listeners, item-renderers, etc. necessary in order to use the 
    * ComboCheck component.
    */ 
    public function ComboCheck()
    {
      super();
      itemRenderer = new ClassFactory(ComboCheckItemRenderer);
      clearInvalid = false;
      addEventListener(IndexChangeEvent.CARET_CHANGE, onIndexChange);
    }
    
    /**
     * This method retrieves the ComboCheckFactory associated with this ComboCheck so that it can be used
     * within a DataGrid.
     * 
     * @param dataProvider This is the XML/ArrayList for the ComboBox.
     * @param dataField This is the name of the sub-element in the data that contains initial values. For 
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
     * @param displayMultipleItems If true, all items checked will be displayed in a comma delimited string.
     * @param displayDefaultMultiple If displayMultipleItems is false, this is the string that will be used 
     * if more than one item is selected.
     */ 
    public static function getFactory(dataProvider:Object, dataField:String, dataFieldValue:String = "@value",
                                      displayField:String = "@display", valueField:String = "@value", 
                                      displayMultipleItems:Boolean = false, displayDefaultMultiple:String = "<multiple>"):IFactory 
    {			
      return new ComboCheckFactory({dataProvider: dataProvider, displayField: displayField, valueField: valueField, 
        dataField: dataField, initialValueField: dataFieldValue, displayMultipleItems: displayMultipleItems, 
        displayDefaultMultiple: displayDefaultMultiple});
    }
    
    // Overridden Methods ----------------------------------------------------------------------------------    
    override protected function commitProperties():void
    {
      super.commitProperties();
      itemRenderer = new ClassFactory(ComboCheckItemRenderer);
      textInput.editable = false; // Don't let the user enter their own text, it will be controlled by their selection(s).
      updateDisplay();
    }
    
    override public function set data(value:Object):void
    {
      _data = value;
      updateInitialValues(value);
    }
    
    override public function get data():Object
    {
      return _data;
    }
    
    override public function set dataProvider(value:IList):void
    {
      super.dataProvider = value;
	  // Reset the checked items.  Otherwise, when the data source for the dataprovider changes,
	  // the checked items get duplicated.
	  _checkedItems = null;
      initCheckedItems();
    }
    
    override public function get display():String
    {
      return _display;
    }
    
    override protected function dropDownController_closeHandler(event:DropDownEvent):void
    {
      super.dropDownController_closeHandler(event);
      selectedIndex = NO_SELECTION;
      callLater(updateDisplay); // Override the current ComboBox change of this field.
    }
    
    override protected function item_mouseDownHandler(event:MouseEvent):void
    {
      if (event.currentTarget is ComboCheckItemRenderer)
      {
        var render:ComboCheckItemRenderer = event.currentTarget as ComboCheckItemRenderer;
        onCheck(render.data);
      }
      else
        updateDisplay();
    }
    
    override protected function keyDownHandler(event:KeyboardEvent):void
    {
      if (dropDownController.isOpen && _proposedSelectedIndex >= 0 && dataProvider && 
          event.keyCode == Keyboard.SPACE)
      {
        onCheck(dataProvider.getItemAt(_proposedSelectedIndex));
      }  
      else
        updateDisplay();
    }
    
    /**
    * Retrieves the value of the first checked item. This is a custom field that uses the valueField to
    * determine the appropriate string from the ComboBox's dataProvider to return.
    */ 
    override public function get value():String
    {
      if (checkedItems.length > 0)
        return (checkedItems is ArrayCollection ? checkedItems.getItemAt(0)[valueField] : checkedItems.getItemAt(0).attribute(valueField.substr(1)));
      return null;
    }
    
    /**
    * Checks the first item with the provided value. This custom field uses the valueField to determine the
    * appropriate item from the ComboBox's dataProvider to check.
    */ 
    override public function set value(value:String):void
    {
      checkItemValue(value);
    }
    
    // Private Methods -------------------------------------------------------------------------------------    
    /**
     * This method checks the item with the dataProvider index provided. If the index passed in or dataProvider is 
     * null, then this method does nothing. Also if the index is >= dataProvider.length, this method does nothing.
     * 
     * @param index This is the index in the dataProvider to be checked.
     */ 
    private function checkItemIndex(index:int):void
    {
     if (!index || !dataProvider || index >= dataProvider.length) {
        return;
	 }
      
      if (!checkedItems)
        checkedItems = (dataProvider is ArrayCollection ? new ArrayCollection() : new XMLListCollection());
      
      if (checkedItems.getItemIndex(dataProvider.getItemAt(index)) >= 0)
        return;
      
      checkedItems.addItem(dataProvider.getItemAt(index));
      updateDisplay();
      dataProvider.itemUpdated(dataProvider.getItemAt(index));
      //dispatchEvent(new Event("change"));
	  dispatchEvent(new spark.events.IndexChangeEvent("change"));
    }
    
    /**
    * This method checks the object provided. If the obj passed in or dataProvider is null, then this method
    * does nothing. Also, if dataProvider[obj] doesn't exist, this method does nothing.
    * 
    * @param obj This is the Object in the dataProvider to be checked.
    */ 
    private function checkItemObj(obj:Object):void
    {
      if (!obj || !dataProvider || dataProvider.getItemIndex(obj) < 0)
        return;
      
      if (!checkedItems)
        checkedItems = (dataProvider is ArrayCollection ? new ArrayCollection() : new XMLListCollection());
      
      if (checkedItems.getItemIndex(obj) >= 0)
        return;
      
      checkedItems.addItem(obj);
      updateDisplay();
      dataProvider.itemUpdated(obj);
      //dispatchEvent(new Event("change"));
	  dispatchEvent(new spark.events.IndexChangeEvent("change"));
    }
    
    /**
     * This method checks the item with the value provided. If the value passed in, dataProvider, or valueField is 
     * null, then this method does nothing.
     * 
     * @param value This is the value in the list to be checked.
     * @param checkOne If this is true then it checks the first item with this value. Otherwise, it will check all
     * items with this value.
     */ 
    private function checkItemValue(value:String, checkOne:Boolean = true):void
    {
      if (!value || !dataProvider || !valueField)
        return;
      
      if (!checkedItems)
        checkedItems = (dataProvider is ArrayCollection ? new ArrayCollection() : new XMLListCollection());
      
      for (var i:int = 0; i < dataProvider.length; i++)
      {
        if ((dataProvider is ArrayCollection && dataProvider.getItemAt(i)[valueField] == value) ||
          (!(dataProvider is ArrayCollection) && dataProvider.getItemAt(i).attribute(valueField.substr(1)) == value))
        {
          if (checkedItems.getItemIndex(dataProvider.getItemAt(i)) > 0) // If we already have the item checked, skip it...
            continue;
          
          checkedItems.addItem(dataProvider.getItemAt(i));
          updateDisplay();
          dataProvider.itemUpdated(dataProvider.getItemAt(i));
          if (checkOne)
            break;
        }
      }
      dispatchEvent(new Event("change"));
    }
    
    /**
     * If the dataProvider and initialValues have been loaded, this checks all the items in the dataProvider
     * that are initially selected. If there is an item in the list with a value of itemAllValue, then it will select
     * all items in the list.
     * <p>
     * Assumption: The initialValues and dataProvider are the same data type (e.g., ArrayCollection, XMLListCollection).
     * The initialValues is simply a sub-set of the dataProvider list.
     */ 
    public function initCheckedItems():void
    {
      if (!dataProvider || !initialValues) {
		  // Need to clear out display if there aren't any items in data provider or any initital checked items
		  updateDisplay();
		  return;
	  }
      
      // If the selectAll item in the list of initial values is checked, then check all items in the list.
      // Otherwise, only check those in the initialValues.
      var i:int;
      var initValueField:String = (this.initialValueField ? this.initialValueField : valueField);
      if (selectAllInList(initialValues))
      {
        for (i = 0; i < dataProvider.length; i++)
          checkItemObj(dataProvider.getItemAt(i));
      }
      else
      {
        for (i = 0; i < dataProvider.length; i++)
        {
		  var wasChecked:Boolean = false;
          for (var j:int = 0; j < initialValues.length; j++)
          {
            if ((dataProvider is ArrayCollection && dataProvider.getItemAt(i)[valueField] == initialValues.getItemAt(j)[initValueField]) ||
                (!(dataProvider is ArrayCollection) && dataProvider.getItemAt(i).attribute(valueField.substr(1)) == initialValues.getItemAt(j).attribute(initValueField.substr(1))))
            {
			  wasChecked = true;
              checkItemObj(dataProvider.getItemAt(i));
              break;
            }
          }
		  // Keep track of what was checked.  We need to explicitly uncheck thos items that
		  // where not checked; otherwise, we will maintain the checks from the
		  // last data provider (the old values).
		  if (!wasChecked) {
			  this.uncheckItemObj(dataProvider.getItemAt(i));
		  }
        }
      }
    }
    
    /**
    * This method adds/removes the item in checkedItems based on whether the user checked/unchecked the
    * item in the dropdown. If the Select All was selected, then it checks all the items in the list. If
    * the Select All was unselected, then it unchecks all the items in the list.
    * <p>
    * This method also dispatches the following events when appropriate: selectAll, deselectAll, addItem, 
    * removeItem, and valueCommit.
    */ 
    private function onCheck(obj:Object):void
    {
      if (!obj)
        return;
      
      var selectAllChecked:Boolean = selectAllInList(checkedItems);
      var i:int;
      
      // If nothing has been checked, or if the item checked isn't in the checkedItem's list, then check the item.
      if (checkedItems == null || checkedItems.getItemIndex(obj) == NO_SELECTION)
      {        
        // Check the Item
        checkItemObj(obj);
        
        // If select all was checked, then loop through the list and check all the items.
        if (selectAllChecked != selectAllInList(checkedItems))
        {
          for (i = 0; i < dataProvider.length; i++)
            checkItemObj(dataProvider.getItemAt(i)); // Check All the Items
          dispatchEvent(new Event("selectAll"));
        }
        dispatchEvent(new Event("addItem"));
      }
      
      // Otherwise, we are unchecking an item in the list.
      else
      {
        // Uncheck the Item
        uncheckItemObj(obj);
        
        // If the item removed was the select all, then uncheck all items in the list.
        if (selectAllChecked != selectAllInList(checkedItems))
        {
          for (i = 0; i < dataProvider.length; i++)
            uncheckItemObj(dataProvider.getItemAt(i)); // Uncheck All the Items
          dispatchEvent(new Event("deselectAll"));
        }
        
        // Otherwise, if select all is checked, uncheck it..
        else if (selectAllChecked)
          uncheckItemValue(valueSelectAll); // Uncheck the "Select All" Item
        
        dispatchEvent(new Event("removeItem"));
      }
      dispatchEvent(new Event("valueCommit"));
    }
    
    /**
     * This method gets thrown any time the user changes the selected item in the list either by using arrow
     * keys in the dropDown, or by clicking on it with the mouse.
     */ 
    private function onIndexChange(event:IndexChangeEvent):void
    {
      _proposedSelectedIndex = event.newIndex;
    }
    
    /**
     * This method searches through the list to see if there is any value equal to the defined valueSelectAll. If
     * valueSelectAll or itemList is null or undefined, this method will return false.
     */ 
    private function selectAllInList(itemList:IList):Boolean
    {
      if (!itemList || !valueSelectAll)
        return false;
      
      // Search through the list to see if there are any items with a value equal to the defined valueSelectAll.
      for (var i:int = 0; i < itemList.length; i++)
      {
        if ((itemList is ArrayCollection && itemList.getItemAt(i)[valueField] == valueSelectAll) ||
          (!(itemList is ArrayCollection) && itemList.getItemAt(i).attribute(valueField.substr(1)) == valueSelectAll))
        {
          return true;
        }
      }
      
      return false;
    }
    
    /**
     * This method unchecks the item with the dataProvider index provided. If the index passed in, checkedItems, or
     * dataProvider is null, then this method does nothing. Also if the index is >= dataProvider.length, this method
     * does nothing.
     * 
     * @param index This is the index in the dataProvider to be unchecked.
     */ 
    private function uncheckItemIndex(index:int):void
    {
      if (!index || !checkedItems || !dataProvider || index >= dataProvider.length)
        return;
      
      var checkedIndex:int = checkedItems.getItemIndex(dataProvider.getItemAt(index));
      if (checkedIndex < 0)
        return;
      
      checkedItems.removeItemAt(checkedIndex);
      updateDisplay();
      dataProvider.itemUpdated(dataProvider.getItemAt(index));
      //dispatchEvent(new Event("change"));
	  dispatchEvent(new spark.events.IndexChangeEvent("change"));
    }
    
    /**
     * This method unchecks the object provided. If the obj passed in, checkedItems, or dataProvider is null, 
     * then this method does nothing. Also, if dataProvider[obj] doesn't exist, this method does nothing.
     * 
     * @param obj This is the Object in the dataProvider to be unchecked.
     */ 
    private function uncheckItemObj(obj:Object):void
    {
      if (!obj || !checkedItems || !dataProvider || dataProvider.getItemIndex(obj) < 0)
        return;
      
      var checkedIndex:int = checkedItems.getItemIndex(obj);
      if (checkedIndex < 0)
        return;
      
      checkedItems.removeItemAt(checkedIndex);
      updateDisplay();
      dataProvider.itemUpdated(obj);
      //dispatchEvent(new Event("change"));
	  dispatchEvent(new spark.events.IndexChangeEvent("change"));
    }
    
    /**
     * This method unchecks the item with the value provided. If the value passed in, checkedItems, dataProvider, or
     * valueField is null, then this method does nothing.
     * 
     * @param value This is the value in the list to be unchecked.
     * @param checkOne If this is true then it checks the first item with this value. Otherwise, it will check all
     * items with this value.
     */ 
    private function uncheckItemValue(value:String, checkOne:Boolean = true):void
    {
      if (!value || !checkedItems || !dataProvider || !valueField)
        return;
      
      for (var i:int = 0; i < checkedItems.length; i++)
      {
        if ((checkedItems is ArrayCollection && checkedItems.getItemAt(i)[valueField] == value) ||
          (!(checkedItems is ArrayCollection) && checkedItems.getItemAt(i).attribute(valueField.substr(1)) == value))
        {
          var index:int = dataProvider.getItemIndex(checkedItems.getItemAt(i));
          checkedItems.removeItemAt(i);
          updateDisplay();
          dataProvider.itemUpdated(dataProvider.getItemAt(i));
          if (checkOne)
            break;
        }
      }
      //dispatchEvent(new Event("change"));
	  dispatchEvent(new spark.events.IndexChangeEvent("change"));

    }
    
    /**
     * This method gets called any time any of the properties associated with the checkedItems list gets 
     * modified, including: checkedItems, listMultipleItems, and defaultMultipleString. It sets the display
     * and dispatches proper events.
     */ 
    private function updateDisplay():void
    {
      var multipleItemsInList:Boolean = false;
      
      // Determine appropriate display text based on the selection.
      if (checkedItems)
      {
        if (checkedItems.length > 1)
        {
          if (displayMultipleItems)
          {
            _display = '';
            for (var i:int = 0; i < checkedItems.length; i++)
            { 
			  if (i != 0)
              {
                _display += ', ';
                multipleItemsInList = true;
              }
              _display += (checkedItems is ArrayCollection ? checkedItems.getItemAt(i)[displayField] : checkedItems.getItemAt(i).attribute(displayField.substr(1)));
            }
          }
          else
            _display = (displayDefaultMultiple == null ? "<multiple>" : displayDefaultMultiple);
        }
        else if (checkedItems.length == 1)
          _display = (checkedItems is ArrayCollection ? checkedItems.getItemAt(0)[displayField] : checkedItems.getItemAt(i).attribute(displayField.substr(1)));
        else
          _display = '';
      }
      else
        _display = '';
      
      // Show the display...
      if (textInput)
      {
        textInput.text = _display;
        textInput.validateNow();
      }
      if (displayMultipleItems) // Only replace the toolTip when displayMultipleItems selected.
      {
        if (multipleItemsInList)
          toolTip = _display;
        else
          toolTip = '';
      }
    }
    
    // Getters/Setters -------------------------------------------------------------------------------------
    /**
    * Retrieves the list of selected items included in the dropdown.
    */ 
    public function get checkedItems():IList 
    {
      return _checkedItems;
      //trace(_checkedItems);
    }
    
    /**
    * Changes the list of selected items included in the dropdown.
    */ 
    [Bindable] public function set checkedItems(value:IList):void 
    {
      _checkedItems = value;
    }
    
    /**
    * Returns the default string that will be used when multiple items are selected. (Note: This will
    * only appear if displayMultipleItems is false.)
    */ 
    public function get displayDefaultMultiple():String
    {
      return _displayDefaultMultiple;
    }
    
    /**
    * Changes the default string that will be used when muleiple items are selected. (Note: This will
    * only appear if displayMultipleItems is false.)
    */ 
    public function set displayDefaultMultiple(value:String):void
    {
      _displayDefaultMultiple = value;
      updateDisplay();
    }
    
    /**
     * If this is false, then the default multiple string will be listed in the text input. Otherwise, it 
     * will list all selected items in a comma delimited string. (Default is false.) If multiple items are
     * in the list, then the toolTip gets replaced with the values from the list.
     */ 
    public function get displayMultipleItems():Boolean
    {
      return _displayMultipleItems;
    }
    
    /**
     * If this is false, then the default multiple string will be listed in the text input. Otherwise, it 
     * will list all selected items in a comma delimited string. (Default is false.) If multiple items are
     * in the list, then the toolTip gets replaced with the values from the list.
     */ 
    public function set displayMultipleItems(value:Boolean):void
    {
      _displayMultipleItems = value;
      updateDisplay();
    }
    
    /**
    * Changes the valueField to be used for the initialValues list. (If this is not provided, then the 
    * valueField for the dataProvider will be used instead.)
    */ 
    public function set initialValueField(value:String):void
    {
      _initialValueField = value;
      initCheckedItems();
    }
    
    /**
    * Retrieves the valueField that was used for the initialValues list. (If this was not provided, then
    * the valueField for the dataProvider was used instead.)
    */ 
    public function get initialValueField():String
    {
      return _initialValueField;
    }
    
    /**
    * Retrieves the set of initial values that were used to pre-load the list of checked items.
    */ 
    public function get initialValues():IList
    {
      return _initialValues;
    }
    
    /**
    * Changes the set of initial values that were used to pre-load the list of checked items.
    */ 
    public function set initialValues(value:IList):void
    {
      _initialValues = value;
      initCheckedItems();
    }
    
    /**
     * Updates the initialValues that are checked in the list based on the value provided.
     * The following fields are required in order for the initialValues can be updated:
     * <ul>
     * <li>dataProvider</li>
     * <li>dataField</li>
     * <li>initialValueField</li>
     * <li>valueField</li>
     * <li>value has to be an XMLList of some sort</li>
     * </ul>
     */ 
    private function updateInitialValues(value:Object):void
    {
      if (!dataProvider || !dataField || !initialValueField || (value is ArrayCollection) || !valueField)
        return;
      
      var sourceList:XMLListCollection = new XMLListCollection(value.child(dataField).children());
      _initialValues = (dataProvider is ArrayCollection ? new ArrayCollection() : new XMLListCollection());
      for (var i:int = 0; i < sourceList.length; i++)
      {
        for (var j:int = 0; j < dataProvider.length; j++)
        {
          if ((dataProvider is ArrayCollection && 
            dataProvider.getItemAt(j)[valueField] == sourceList.getItemAt(i).attribute(initialValueField.substr(1)) &&
            _initialValues.getItemIndex(dataProvider.getItemAt(j)) <= 0) || 
            
            (!(dataProvider is ArrayCollection) && 
              dataProvider.getItemAt(j).attribute(valueField.substr(1)) == sourceList.getItemAt(i).attribute(initialValueField.substr(1)) &&
              _initialValues.getItemIndex(dataProvider.getItemAt(j)) <= 0))
          {
            _initialValues.addItem(dataProvider.getItemAt(j));
          }
        }
      }
      initialValues = _initialValues;
    }
    
    /**
    * Returns the value of the item in the list to be used as select/deselect all. If this is null, then
    * this ComboCheck assumes that there is no select all item in the list.
    */ 
    public function get valueSelectAll():String
    {
      return _valueSelectAll;
    }
    
    /**
    * Changes the value of the item in the list to be used as select/deselect all. If this is null, then
    * this ComboCheck assumes that there is no select all item in the list.
    */ 
    public function set valueSelectAll(value:String):void
    {
      _valueSelectAll = value;
    }
    
    /**
     * Retrieves the checkedItems wrapped in the defined dataField.
     */ 
    public function get wrappedValue():Object
    {
      /*if (!dataField)
        return null;
      
      var _value:Object;
      if (checkedItems is ArrayList)
      {
        _value = new ArrayList();
        _value.addItem(dataField);
        ArrayCollection(_value.getItemAt(0)).addAll(checkedItems);
      }
      else
      {
        _value = new XMLListCollection();
        _value.addItem(new XML("<" + dataField + "/>"));
        if (checkedItems)
        {
          for (var i:int = 0; i < checkedItems.length; i++)
            _value.getItemAt(0).appendChild(checkedItems.getItemAt(i));
        }
      }
      return _value;*/
      return checkedItems;
    }
  }
}

/**
 * This is the ComboCheckFactory that allows the ComboCheck to be used from a DataGrid.
 */ 
class ComboCheckFactory implements mx.core.IFactory 
{
  private var properties:Object;
  
  public function ComboCheckFactory(properties:Object) 
  {
    this.properties = properties;
  }
  
  public function newInstance():* 
  {
    var comboCheck:util.ComboCheck = new util.ComboCheck();
    
    comboCheck.valueField = properties.valueField;
    comboCheck.displayField = properties.displayField;
    comboCheck.dataProvider = properties.dataProvider;
    comboCheck.dataField = properties.dataField;
    comboCheck.initialValueField = properties.initialValueField;
    comboCheck.displayMultipleItems = properties.displayMultipleItems;
    comboCheck.displayDefaultMultiple = properties.displayDefaultMultiple;
    
    return comboCheck;	
  }
}