package util;

public class StringUtil {
	
	public static String removePuntionation(String str)
	{
		return str.replaceAll("[^\\w\\s]", "").replaceAll("[\\d]", "");
	}
	
	
}
