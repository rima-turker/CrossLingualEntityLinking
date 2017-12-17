package CandidateGeneration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.collections4.bag.SynchronizedSortedBag;

public class deneme {

	static {
		System.setProperty("java.net.useSystemProxies", "true");
	}
	
	public static void main(String[] args) {
		
		Request_yovisto r = new Request_yovisto();
		r.setQuery("https://en.wikipedia.org/w/api.php?action=query&blfilterredir=redirects&bllimit=max&bltitle=Yahoo!&format=json&list=backlinks");
		r.setDataFormat(DataFormat.JSON);
		
		
		
		try {
			
			String encodeQuery = URLEncoder.encode(r.getQuery(), "UTF-8").replace("+", "%20");
			
			//System.out.println(encodeQuery);
			
			final URL url = new URL(r.getQuery());
			System.out.println(url);
			final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", r.getDataFormat().text);

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
		System.out.println(result.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
