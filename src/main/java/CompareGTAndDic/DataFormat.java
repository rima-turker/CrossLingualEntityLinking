package CompareGTAndDic;

public enum DataFormat {
	JSON("application/json"),
	XML("application/sparql-results+xml"),
	TSV("text/tab-separated-values"),
	CSV("text/csv"),
	HTML("application/html");
	
	String text;
	
	private DataFormat(String text) {
		this.text = text;
	}
	
	String getText(){
		return text;
	}
}
