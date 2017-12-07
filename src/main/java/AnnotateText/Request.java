package AnnotateText;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import util.Config;

public class Request {

	private static final String BASE_URL = Config.getString("SERVER_BASE_URL", "");
	private static final HttpClient client = new DefaultHttpClient();
	
	public static void main(String[] args) throws Exception {
		System.err.println(sendGet("hello","hi"));
	}

	private static double sendGet(String entity1, String entity2) throws Exception {
		String url = BASE_URL+"similarity?"+"entity1="+entity1+"&entity2="+entity2;
		final HttpGet request = new HttpGet(url);
		final HttpResponse response = client.execute(request);
		final BufferedReader rd = new BufferedReader(
                       new InputStreamReader(response.getEntity().getContent()));

		final StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		return Double.parseDouble(result.toString());
	}
}