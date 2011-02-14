package util
{
  import flash.events.Event;
  import flash.events.KeyboardEvent;
  import flash.events.MouseEvent;
  import flash.ui.Keyboard;
  
  import util.ComboCheck;
  import util.ComboCheckEvent;
  
  import mx.binding.utils.BindingUtils;
  import mx.binding.utils.ChangeWatcher;
  import mx.collections.ArrayCollection;
  import mx.collections.IList;
  import mx.core.ClassFactory;
  import mx.events.FlexEvent;
  import mx.events.ItemClickEvent;
  
  import spark.components.CheckBox;
  import spark.components.supportClasses.ItemRenderer;
  
  /**
  * The comboChecked event gets called any time the user checks/unchecks an item in the ComboCheck dropdown.
  */ 
  [Event(name="check", type="lib.events.ComboCheckEvent")]
  
  /**
  * This class creates an CheckBox item renderer that may be used by the ComboCheck control. It handles the
  * click event for the CheckBox and launches the parent's ComboCheckEvent.
  */ 
  public class ComboCheckItemRenderer extends ItemRenderer
  {
    // -----------------------------------------------------------------------------------------------------
    // ATTRIBUTES
    // -----------------------------------------------------------------------------------------------------
    public var item:CheckBox;
    private var _data:Object;
    
    // -----------------------------------------------------------------------------------------------------
    // METHODS
    // -----------------------------------------------------------------------------------------------------
    // Public Methods --------------------------------------------------------------------------------------
    /**
    * Constructor: This method adds event listeners where necessary.
    */ 
    public function ComboCheckItemRenderer()
    {
      super();
      item = new CheckBox();
      item.x = 5;
      addElement(item);
      item.addEventListener(MouseEvent.CLICK, onClick);
      item.addEventListener(KeyboardEvent.KEY_DOWN, onKeyDown);
    }
    
    /**
    * This method gets called any time the user clicks on the checkbox. It launches the ItemClickEvent 
    * associated with its owner so that the list of checked items may be maintained.
    */ 
    public function onClick(event:Event):void
    {
      var e:ItemClickEvent = new ItemClickEvent(ItemClickEvent.ITEM_CLICK, true);
      e.item = data;
      ComboCheck(owner).dispatchEvent(e);
    }
    
    /**
    * This method gets called any time the user presses a key within the checkbox. If the space bar was pressed, 
    * it launches the ItemClickEvent associated with its owner so that the list of checked items may be maintained.
    */ 
    public function onKeyDown(event:KeyboardEvent):void
    {
      // Only process the event if the enter key or space bar were pressed...
      if (!event.keyCode == Keyboard.SPACE)
        return;
      
      var e:ItemClickEvent = new ItemClickEvent(ItemClickEvent.ITEM_CLICK, true);
      e.item = data;
      ComboCheck(owner).dispatchEvent(e);
    }
    
    // Getters/Setters -------------------------------------------------------------------------------------
    /**
     * This method retrieves the data that has been associated with this item renderer.
     */ 
    override public function get data():Object
    {
      return _data;
    }
    
    /**
     * This method changes the data associated with this item renderer.
     */ 
    [Bindable]
    override public function set data (value:Object):void
    {
      if (value != null)
      {
        _data = value;
        var displayField:String = ComboCheck(owner).displayField;
        if (displayField)
          item.label = (value is ArrayCollection ? value[displayField] : value.attribute(displayField.substr(1)));
        item.selected = (ComboCheck(owner).checkedItems && ComboCheck(owner).checkedItems.getItemIndex(data) >= 0);
      }
    }
  }
}