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
	public static String removeUmlaut(String str)
	{
//		String entity = str.replace("\\u0028","(").
//				replace("\\u0029",")").replace("\\u0027","'").replace("\\u00fc","ü").replace("\\u002c",",").
//				replace("\\u0163","ţ").replace("\\u00e1s","á").replace("\\u0159","ř").replace("\\u00e9","é").
//				replace("\\u00ed","í").replace("\\u00e1","á").replace("\\u2013","-").replace("\\u0107","ć").
//				replace("\\u002e",".").replace("\\u00f3","ó").replace("\\u002d","-").replace("\\u00e1","Ž").
//				replace("\\u0160","Š").replace("\\u0105","ą").replace("\\u00eb","ë").replace("\\u017d","Ž").
//				replace("\\u00e7","ç").replace("\\u00f8","ø").replace("\\u0161","š").replace("\\u0107","ć").
//				replace("\\u00f6","ö").replace("\\u010c","Č").replace("\\u00fd","ý").replace("\\u00d6","Ö").
//				replace("\\u00c0","À").replace("\\u0026","&").replace("\\u00df","ß").replace("\\u00ea","ê").
//				replace("\\u017","ž").replace("\\u011b","ě").replace("\\u00f6","ö").replace("\\u00e3","ã").
//				replace("\\u0103","ă").replace("\\u00c1","Á").replace("\\u002f","/").replace("\\u00e4","ä").
//				replace("\\u00c5","Å").replace("\\u0142","ł").replace("\\u0117","ė").replace("\\u00ff","ÿ").
//				replace("\\u00f1","ñ").replace("\\u015f","ş").replace("\\u015e","Ş").replace("\\u0131","ı").
//				replace("\\u0131k","Ç").replace("\\u0144","ń").replace("\\u0119","ę").replace("\\u00c9","É").
//				replace("\\u0111","đ").replace("\\u00e2","â").replace("\\u010d","č").replace("\\u015a","Ś").
//				replace("\\u0141","Ł").replace("\\u00e8","è").replace("\\u00c9","É").replace("\\u00e5","å").
//				replace("\\u014d","ō").replace("\\u00e6","æ").replace("\\u00d3","Ó").replace("\\u00da","Ú").
//				replace("\\u0151","ő").replace("\\u0148","ň").replace("\\u00fa","ú").replace("\\u00ee","î").
//				replace("\\u015b","ś").replace("\\u00c7","Ç").replace("\\u00f4","ô").replace("\\u013d","Ľ").
//				replace("\\u013e","ľ").replace("\\u011f","ğ").replace("\\u00e0","à").replace("\\u00dc","Ü").
//				replace("\\u0021","!").replace("_"," ");
		
		String entity = str.replace("á","a").replace("ř","r").replace("é","e").
				replace("í","i").replace("ć","c").replace("ó","o").replace("Ž","Z").replace("Š","S").replace("ą","a").replace("ë","e").replace("Ž","Z").
				replace("š","s").replace("Č","C").replace("ý","y").replace("À","A").replace("ê","e").
				replace("ž","z").replace("ě","e").replace("ã","a").
				replace("ă","a").replace("Á","A").replace("Å","A").replace("ė","e").replace("ÿ","y").
				replace("ñ","n").replace("ı","i").replace("ń","n").replace("ę","e").replace("É","E").
				replace("đ","d").replace("â","a").replace("\\u010d","č").replace("Ś","S").
				replace("è","e").replace("É","E").replace("å","a").
				replace("ō","ö").replace("Ó","O").replace("Ú","U").
				replace("ő","ö").replace("ň","n").replace("\\u00fa","ú").replace("î","i").
				replace("ś","s").replace("ô","o").replace("Ľ","L").
				replace("ľ","l").replace("à","a");
				return entity;
	
	}
}
