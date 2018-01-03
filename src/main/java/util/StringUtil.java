package util;

public class StringUtil {
	
	public static String removePuntionation(String str)
	{
		return str.replaceAll("[^\\w\\s]", "").replaceAll("[\\d]", "");
	}
	
	public static String convertUmlaut(String text) {
        final String[][] UMLAUT_REPLACEMENTS = { { new String("Ä"), "Ae" }, { new String("Ü"), "Ue" },
                { new String("Ö"), "Oe" }, { new String("ä"), "ae" }, { new String("ü"), "ue" },
                { new String("ö"), "oe" }, { new String("ß"), "ss" } };
        String result = text;
        for (int i = 0; i < UMLAUT_REPLACEMENTS.length; i++) {
            result = result.replace(UMLAUT_REPLACEMENTS[i][0], UMLAUT_REPLACEMENTS[i][1]);
        }
        return result;
    }
}
