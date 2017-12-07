package CandidateGeneration;

public class Request_yovisto {
	private String query;
	private DataForomat dataFormat;
	
	public DataForomat getDataFormat() {
		return dataFormat;
	}
	public void setDataFormat(DataForomat dataFormat) {
		this.dataFormat = dataFormat;
	}

	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
	@Override
	public String toString() {
		return "Request [query=" + query + ", dataFormat=" + dataFormat + "]";
	}
}
