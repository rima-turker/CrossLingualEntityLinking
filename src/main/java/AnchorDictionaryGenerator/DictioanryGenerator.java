package AnchorDictionaryGenerator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import model.HtmlLink;
import util.Config;
import util.FileUtil;
import util.HTMLLinkExtractor;
import util.MapUtil;

/**
 * Generate dictionary from Wikipedia anchor text FORGOT TO ADD REDIRECT PAGES
 * some point;
 * - we do not touch the mention itself only double cottaions are removed from beggining and at the end such as "Rima" --> Rima
 * 	- we just keep links which are wikipeida links
 * 		-- E.g we remove www.google.com
 * 
 *  - we convert all the urls to decoded url and to lowercase
 * @author rtue
 *
 */

//TODO ADD REDIRECT PAGES to your dictionary to enrich it
public class DictioanryGenerator {

	private final String RESULT_FILE = Config.getString("ANCHOR_DICTIONARY", "");
	private static final String FILES_ADDRESS = Config.getString("SPLIT_WIKIPEDIA_ALLANNOTATED_SENTENCES_FOLDER", "");
	private static ExecutorService executor;
	//private static int NUMBER_OF_THREADS = 1;
	private static Map<String,Map<String,Double>> dictionary = new ConcurrentHashMap<>();
	private static Map<String,List<String>> redirect = new HashMap<>();
	private final String REDIRECT_FILE = Config.getString("REDIRECT_FILE", "");

	//WIKIPEDIA_REDIRECT_PAGESConfig.getString("ANCHOR_DICTIONARY", "");
	//private 

	//https://en.wikipedia.org/w/api.php?action=query&blfilterredir=redirects&bllimit=max&bltitle=Yahoo!&format=json&list=backlinks

	//	public static void main(String[] args) {
	//		createDictionary();		
	//		//dictionary = readDictionryFromFile();
	//	}


	private void createDictionary() {
		try {
			//executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS );
			executor = Executors.newSingleThreadExecutor();

			final File[] listOfFiles = new File(FILES_ADDRESS).listFiles();
			Arrays.sort(listOfFiles);
			for (int i = 0; i < listOfFiles.length; i++) {
				final String file = listOfFiles[i].getName();
				executor.execute(handle(FILES_ADDRESS + File.separator + file));
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

			convertFrequencyToProbability();
			writeDictionary();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}


	private void writeDictionary() {
		final List<String> result = new ArrayList<>();
		for(Entry<String, Map<String, Double>> entry:dictionary.entrySet()) {
			final StringBuilder s = new StringBuilder();
			s.append(entry.getKey());
			s.append("\t");
			for(Entry<String, Double> e:entry.getValue().entrySet()) {
				s.append(e.getKey()).append(" ").append(e.getValue());
				s.append("\t");
			}
			result.add(s.toString());
		}
		FileUtil.writeDataToFile(result, RESULT_FILE,false);
		System.err.println("Size of dictioanry = "+dictionary.size());
	}


	private static void convertFrequencyToProbability() {
		for(Entry<String, Map<String, Double>> entry:dictionary.entrySet()) {
			Map<String, Double> listOfUrls = entry.getValue();
			final double totalNumber = listOfUrls.values().stream().mapToDouble(Double::doubleValue).sum();
			for(Entry<String, Double> e:listOfUrls.entrySet()) {
				listOfUrls.put(e.getKey(), (e.getValue()/totalNumber));
			}
			listOfUrls = MapUtil.sortByValueDescending(listOfUrls);
			dictionary.put(entry.getKey(), listOfUrls);
		}
	}


	private static Runnable handle(String pathTofile) {
		return () -> {
			try {
				final List<String> lines = Files.readAllLines(Paths.get(pathTofile), StandardCharsets.UTF_8);
				final HTMLLinkExtractor htmlLinkExtractor = new HTMLLinkExtractor();
				for(String line: lines) {
					final Vector<HtmlLink> links = htmlLinkExtractor.grabHTMLLinks(line);
					for(HtmlLink link:links) {
						final String decodedUrl = link.getDecodedUrl().toLowerCase();
						final String normalizeMention = normalizeMention(link.getAnchorText().trim());
						if(isValidWikipediaUrl(decodedUrl) && isValidDecodedUrl(decodedUrl) && isValidMention(normalizeMention)){
							addToDictionary(normalizeMention,decodedUrl.trim());
						}
					}
				}
				System.err.println("file "+pathTofile+" handled.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		};
	}


	private static boolean isValidMention(String normalizeMention) {
		if(normalizeMention.isEmpty()){
			return false;
		}
		return true;
	}


	private static boolean isValidDecodedUrl(String url) {
		if (url.contains("\n")) 
		{
			return false;
		}
		if(url.isEmpty()){
			return false;
		}
		//TODO I am not sure about this part
		if(url.contains("%")){
			return false;
		}
		return true;
	}


	/**
	 * Remove " from beginning and ending of the word
	 * @param trim
	 * @return
	 */
	private static String normalizeMention(String word) {
		String result = new String(word);
		if(result.length()>=1 && result.charAt(0)=='"'){
			result = result.substring(1, result.length());
		}
		if(result.length()>=1 && result.charAt(result.length()-1)=='"'){
			result = result.substring(0, result.length()-1);
		}
		return result;
	}


	private static boolean isValidWikipediaUrl(String url) {
		if(StringUtils.containsIgnoreCase(url,"http%3A//")){
			return false;
		}
		if(StringUtils.containsIgnoreCase(url,"https%3A//")){
			return false;
		}
		if(StringUtils.containsIgnoreCase(url,"http://")){
			return false;
		}
		if(StringUtils.containsIgnoreCase(url,"https://")){
			return false;
		}

		return true;
	}


	private static void addToDictionary(String anchorText, String decodedUrl) {
		final Map<String, Double> listOfLinks = dictionary.get(anchorText);
		if(listOfLinks==null) {
			final Map<String,Double> map = new ConcurrentHashMap<>();
			map.put(decodedUrl,1.);
			dictionary.put(anchorText, map);
		}else {
			final Double frequency = listOfLinks.get(decodedUrl);
			if(frequency==null) {
				listOfLinks.put(decodedUrl, 1.0);
				dictionary.put(anchorText, listOfLinks);
			}else {
				listOfLinks.put(decodedUrl, (frequency+1));
				dictionary.put(anchorText, listOfLinks);
			}
		}
	}

	/***
	 * This function collects the redirect pages 
	 * Map<String,List<String>>
	 * String= main page the one that we see on the browser
	 * List<String> stores the list of pages that redirects to that page
	 * @return
	 */

	public Map<String,List<String>> readRedirectPages()
	{
		System.out.println(REDIRECT_FILE);
		Map<String,List<String>> result = new HashMap<>();
		String line=null;
		try (BufferedReader br = new BufferedReader(new FileReader(REDIRECT_FILE))) {
			while ((line = br.readLine()) != null) {
				final String[] split = line.toLowerCase().split("\t");
				final String fromRedirects = split[0].toLowerCase();
				final String toRedirects = split[1].toLowerCase();//mainpage(exist one)
				if (!fromRedirects.contains("template:")&&!toRedirects.contains("template:")) {
					List<String> lstTemp ;
					if (result.containsKey(toRedirects)) //mainPage
					{
						lstTemp = new ArrayList<>(result.get(toRedirects));
					}
					else
					{
						lstTemp = new ArrayList<>();
					}
					lstTemp.add(fromRedirects);
					result.put(toRedirects, lstTemp);
				}
			}
		} catch (Exception e) {
			System.err.println(line);
			e.printStackTrace();
		}	

		return result;
	}
	public Map<String,List<String>> readRedirectPages_otherway()
	{
		System.out.println(REDIRECT_FILE);
		Map<String,List<String>> result = new HashMap<>();
		String line=null;
		try (BufferedReader br = new BufferedReader(new FileReader(REDIRECT_FILE))) {
			while ((line = br.readLine()) != null) {
				final String[] split = line.toLowerCase().split("\t");
				final String fromRedirects = split[0].toLowerCase();
				final String toRedirects = split[1].toLowerCase();//mainpage(exist one)
				if (!fromRedirects.contains("template:")&&!toRedirects.contains("template:")) {
					List<String> lstTemp ;
					if (result.containsKey(fromRedirects)) //mainPage
					{
						lstTemp = new ArrayList<>(result.get(fromRedirects));
					}
					else
					{
						lstTemp = new ArrayList<>();
					}
					lstTemp.add(toRedirects);
					result.put(fromRedirects, lstTemp);
				}

			}
		} catch (Exception e) {
			System.err.println(line);
			e.printStackTrace();
		}	
		return result;
	}
	public Map<String,Map<String,Double>> readDictionryFromFile() {

		System.out.println("reading dictionary");
		Map<String,Map<String,Double>> result = new ConcurrentHashMap<>();
		String line=null;
		try (BufferedReader br = new BufferedReader(new FileReader(RESULT_FILE))) {
			while ((line = br.readLine()) != null) {
				final String[] split = line.split("\t");
				final String anchorText = split[0];
				final Map<String,Double> map = new ConcurrentHashMap<>();
				for(int i=1;i<split.length;i++) {
					final String[] link_frequency = split[i].split(" ");
					map.put(link_frequency[0],Double.parseDouble(link_frequency[1]));
				}
				result.put(anchorText,MapUtil.sortByValueDescending(map));
			}
		} catch (Exception e) {
			System.err.println(line);
			e.printStackTrace();
		}		
		return result;
	}
}
