package util
{
	import flash.events.Event;
	
	public class DictionaryRefreshEvent extends flash.events.Event 
	{
		public static var DICTIONARY_REFRESHED:String          = "dictionaryRefreshed";
		
		public function DictionaryRefreshEvent()
		{
			super(DICTIONARY_REFRESHED);
		}

	}
}