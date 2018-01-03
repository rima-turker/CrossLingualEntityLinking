package CompareGTAndDic;

public class Request_yovisto {
	private String query;
	private DataFormat dataFormat;
	
	public DataFormat getDataFormat() {
		return dataFormat;
	}
	public void setDataFormat(DataFormat dataFormat) {
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
