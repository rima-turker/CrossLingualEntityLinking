package CandidateGeneration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.ResultSet;

import org.apache.log4j.Logger;

import edu.stanford.nlp.io.EncodingPrintWriter.out;




//https://dbpedia.org/sparql?default-graph-uri=http%3A%2F%2Fdbpedia.org&query=
public class Caller_yovisto {
	
	private static final String REFER_ENDPOINT = "http://apps.yovisto.com/refer-rest/services/suggest/x?input=";
	private static final String DBPEDIA_ENDPOINT = "https://dbpedia.org/sparql?default-graph-uri=http%3A%2F%2Fdbpedia.org&query=";
	//http://10.10.4.10:8890/sparql
	private static final String DBPEDIA_ENDPOINT_local ="http://10.10.4.10:8890/sparql?default-graph-uri=&query=";
	
	//select+distinct+%3FConcept+where+%7B%5B%5D+a+%3FConcept%7D+LIMIT+100&format=text%2Fhtml&timeout=0&debug=on
	
	//https://dbpedia.org/sparql?default-graph-uri=http%3A%2F%2Fdbpedia.org&query=select+distinct+%3FConcept+where+%7B%5B%5D+a+%3FConcept%7D+LIMIT+100&format=text%2Fhtml&CXML_redir_for_subjs=121&CXML_redir_for_hrefs=&timeout=30000&debug=on&run=+Run+Query+
	static {
		System.setProperty("java.net.useSystemProxies", "true");
	}

	public static String runPopularity(String entity) {

		String queryEnd="&format=text%2Fhtml&CXML_redir_for_subjs=121&CXML_redir_for_hrefs=&timeout=30000&debug=on&run=+Run+Query+";
		//String queryEnd_local = "&format=text%2Fhtml&timeout=0&debug=on";
		//http://dbpedia.org/resource/
		String query="select count(?predicate) where {?subj ?predicate <"+entity+">}";;
		try {
			query = URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		Request_yovisto request= new Request_yovisto();
		request.setQuery(query+queryEnd);
		
	//	request.setQuery("select+distinct+%3FConcept+where+%7B%5B%5D+a+%3FConcept%7D+LIMIT+100&format=text%2Fhtml&timeout=0&debug=on");
		request.setDataFormat(DataForomat.HTML);
		
		try {
			//final URL url = new URL(DBPEDIA_ENDPOINT + request.getQuery());
			
			
			System.out.println(DBPEDIA_ENDPOINT+request.getQuery());
			URL url = new URL(DBPEDIA_ENDPOINT+request.getQuery());
			
			final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			//conn.setRequestProperty("Accept", request.getDataFormat().text);

			//System.err.println("Accessing REST API...");
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}
			//System.err.println("Received result from REST API.");
			final BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			final StringBuilder result = new StringBuilder("");
			String output;
			while ((output = br.readLine()) != null) {
				result.append(output);
			}
			
			//String r = result.toString().replaceAll(".*<td><pre>","");
			//r = r.replaceAll("</pre></td>.*", "");
			
			String r = result.toString().replaceAll(".*<td>","").replaceAll("</td>.*","");
			//<td>21</td>
			
			//System.err.println(r);
			conn.disconnect();
			//return result.toString();
			return r;
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		
		System.out.println(request.toString());
		return null;
	}
	
	public static String runYovisto(final Request_yovisto request) {

		try {
			
			String encodeQuery = URLEncoder.encode(request.getQuery(), "UTF-8").replace("+", "%20");
			
			//System.out.println(encodeQuery);
			
			final URL url = new URL(REFER_ENDPOINT + encodeQuery);
			//System.out.println(REFER_ENDPOINT + encodeQuery);
			final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", request.getDataFormat().text);

			//System.err.println("Accessing REST API...");
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}
			//System.err.println("Received result from REST API.");
			final BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			final StringBuilder result = new StringBuilder("");
			String output;
			while ((output = br.readLine()) != null) {
				result.append(output);
				//System.out.println(output);
			}
			conn.disconnect();
			return result.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String runDBpedia(final Request_yovisto request) {

		try {
			final URL url = new URL(DBPEDIA_ENDPOINT + request.getQuery());
			final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", request.getDataFormat().text);

			System.err.println("Accessing REST API...");
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}
			System.err.println("Received result from REST API.");
			final BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			final StringBuilder result = new StringBuilder("");
			String output;
			while ((output = br.readLine()) != null) {
				result.append(output);
			}
			conn.disconnect();
			return result.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
