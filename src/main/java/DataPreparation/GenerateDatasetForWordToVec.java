package DataPreparation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.twelvemonkeys.lang.StringUtil;

import model.HtmlLink;
import util.Config;
import util.HTMLLinkExtractor;


public class GenerateDatasetForWordToVec {
	private static final String WIKIPEDIA_ALLANNOTATED_SENTENCES = Config.getString("WIKIPEDIA_ALLANNOTATED_SENTENCES", "");
	private static final Logger LOG = Logger.getLogger(GenerateDatasetForWordToVec.class);
	static final Logger secondLOD = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	//private static final String fileMapping = Global.FILE_MAP;
	//private static final String fileWikiSentencesWithLinks =Global.FILE_DE_ALL_SENTENCES_CONTAINSLINK;

	/*
	 * LOG.info("LOG1");
		secondLOD.info("LOG2");
		resultLog.info("LOG3");
		private static final Logger LOG = Logger.getLogger(Main.class);
	static final Logger secondLOD = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	 */
	private static final String fileMapping = "interlangual_en_de_mapping";

	/**
	 * This function creates a new dataset for word2vec
	 * simply gets as a input all the sentences that are annotated(in german) 
	 * and replaces the corresponding anchor text and URI with the 
	 * english URI, keeps the words(but not the anchor text as it is replaced) and removes the puctiotaions
	 */
	public void generate_dataSet_DE_enEntity_deWordSim() {
		final Map<String, String> de_engMap = new HashMap<String, String>(createMapping(fileMapping));

		final HTMLLinkExtractor htmlLinkExtractor = new HTMLLinkExtractor();
		try (final BufferedReader br = new BufferedReader(new FileReader(WIKIPEDIA_ALLANNOTATED_SENTENCES))) {
			String line;
			while ((line = br.readLine()) != null) {
				final Map<String, String> punctuationHelper = new HashMap<>();
				int offset = 0;
				StringBuilder resultLine = new StringBuilder(line);
				final Vector<HtmlLink> links = htmlLinkExtractor.grabHTMLLinks(line);
				for (final Iterator<?> iterator = links.iterator(); iterator.hasNext();) {
					final HtmlLink htmlLink = (HtmlLink) iterator.next();
					final String deUrl = htmlLink.getDecodedUrl();
					final String enUrl = de_engMap.get(deUrl);
					String finalEnUrl = "dbr:" + enUrl;
					String randomString = getSaltString();
					if (enUrl == null) {
						finalEnUrl = "";
					}
					punctuationHelper.put(randomString, finalEnUrl);
					resultLine.replace(htmlLink.getStart() + offset, htmlLink.getEnd() + offset, randomString);
					offset += (randomString.length() - (htmlLink.getEnd() - htmlLink.getStart()));
				}

				resultLine = new StringBuilder(util.StringUtil.removePuntionation(resultLine.toString()).toLowerCase());
				String finalResultLine = new String(resultLine.toString());
				for (Entry<String, String> entry : punctuationHelper.entrySet()) {
					finalResultLine = finalResultLine.replace(entry.getKey(), entry.getValue());
				}

				finalResultLine = finalResultLine.toString().replaceAll(" +", " ").trim();
				if (finalResultLine.contains("dbr:")) {
					LOG.info(finalResultLine);
				}
				// System.err.println(line);
				// System.err.println(finalResultLine);
				// System.err.println("---------------------------------------------------");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * This function creates a data set for word2vec keeps the only entities 
	 */
	public void generate_dataSet_EN_enEntity_enEntitySimilarity() {
		final HTMLLinkExtractor htmlLinkExtractor = new HTMLLinkExtractor();
		try (final BufferedReader br = new BufferedReader(new FileReader(WIKIPEDIA_ALLANNOTATED_SENTENCES))) {
			String line;
			StringBuilder strBuild = new StringBuilder();
			while ((line = br.readLine()) != null) {
				final Vector<HtmlLink> links = htmlLinkExtractor.grabHTMLLinks(line);
				for (final Iterator<?> iterator = links.iterator(); iterator.hasNext();) {
					final HtmlLink htmlLink = (HtmlLink) iterator.next();
					final String enUrl = htmlLink.getDecodedUrl();
					String finalEnUrl = "dbr:" + enUrl;
					strBuild.append(finalEnUrl+" ");
				}
				if (strBuild.toString().contains("dbr:")) {
					secondLOD.info(strBuild.toString());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Exactly the same as de_getWordsEntities_en_mapping
	 * but for english sentences (english URI)
	 */
	public void generate_dataSet_EN_enEntity_enWordSimityWordSim() {
		System.out.println("generate_dataSet_EN_enEntity_enWordSimityWordSim");
		final HTMLLinkExtractor htmlLinkExtractor = new HTMLLinkExtractor();
		try (final BufferedReader br = new BufferedReader(new FileReader(WIKIPEDIA_ALLANNOTATED_SENTENCES))) {
			String line;
			while ((line = br.readLine()) != null) {
				final Map<String, String> punctuationHelper = new HashMap<>();
				int offset = 0;
				StringBuilder resultLine = new StringBuilder(line);
				final Vector<HtmlLink> links = htmlLinkExtractor.grabHTMLLinks(line);
				for (final Iterator<?> iterator = links.iterator(); iterator.hasNext();) {
					final HtmlLink htmlLink = (HtmlLink) iterator.next();
					final String enUrl = htmlLink.getDecodedUrl();
					String finalEnUrl = "dbr:" + enUrl;
					String randomString = getSaltString();
					punctuationHelper.put(randomString, finalEnUrl);
					resultLine.replace(htmlLink.getStart() + offset, htmlLink.getEnd()+ offset, randomString);
					offset += (randomString.length() - (htmlLink.getEnd()- htmlLink.getStart()));
				}

				resultLine = new StringBuilder(
						resultLine.toString().replaceAll("[^\\w\\s]", "").replaceAll("[\\d]", "").toLowerCase());
				String finalResultLine = new String(resultLine.toString());
				for (Entry<String, String> entry : punctuationHelper.entrySet()) {
					finalResultLine = finalResultLine.replace(entry.getKey(), entry.getValue());
				}

				finalResultLine = finalResultLine.toString().replaceAll(" +", " ").trim();
				if (finalResultLine.contains("dbr:")) {
					LOG.info(finalResultLine.toLowerCase());
				}
//				 System.err.println(line);
//				 System.err.println(finalResultLine);
//				 System.err.println("---------------------------------------------------");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Data set generator removes tags keeps mentions i.e only words plain text 
	 * You could also use whole wikipedia for this dataset generation
	 */
	public void generate_dataSet_EN_enWord_enWordSim() {

		System.out.println("generate_dataSet_EN_enWord_enWordSim");
		final HTMLLinkExtractor htmlLinkExtractor = new HTMLLinkExtractor();
		try (final BufferedReader br = new BufferedReader(new FileReader(WIKIPEDIA_ALLANNOTATED_SENTENCES))) {
//		try (final BufferedReader br = new BufferedReader(new FileReader("/home/rtue/workspace/Wikipedia/bin/temp_WikiSentencesLinks"))) {
			String line;
			while ((line = br.readLine()) != null) {
				int offset = 0;
				StringBuilder resultLine = new StringBuilder(line);
				final Vector<HtmlLink> links = htmlLinkExtractor.grabHTMLLinks(line);
				for (final Iterator<?> iterator = links.iterator(); iterator.hasNext();) {
					final HtmlLink htmlLink = (HtmlLink) iterator.next();
					String anchor = htmlLink.getAnchorText();
					resultLine.replace(htmlLink.getStart() + offset, htmlLink.getEnd() + offset, anchor);
					offset += (anchor.length() - (htmlLink.getEnd() - htmlLink.getStart()));
				}
				resultLine = new StringBuilder(util.StringUtil.removePuntionation(resultLine.toString()).toLowerCase());
				String finalResultLine = new String(resultLine.toString());

				finalResultLine = finalResultLine.toString().replaceAll(" +", " ").trim();
				secondLOD.info(finalResultLine);
//				 System.err.println(line);
//				 system.err.println(finalresultline);
//				 system.err.println("---------------------------------------------------");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This function generates random string
	 * 
	 * @return
	 */
	private String getSaltString() {
		String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toLowerCase();
		StringBuilder salt = new StringBuilder();
		Random rnd = new Random();
		while (salt.length() < 18) { // length of the random string.
			int index = (int) (rnd.nextFloat() * SALTCHARS.length());
			salt.append(SALTCHARS.charAt(index));
		}
		String saltStr = salt.toString();
		return saltStr;

	}

	/**
	 * This function reads a file and generate a mapping between de url and
	 * corresponding en url
	 * 
	 * @param file
	 *            this is the dump file
	 * @return mapping between de and en urls
	 */
	private Map<String, String> createMapping(String file) {
		final Map<String, String> de_enMap = new HashMap<String, String>();
		try (final BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
				final String[] split = line.split("\t");
				String englishUrl = split[0];
				String germanUrl = split[1];

				englishUrl = englishUrl.replace("<http://dbpedia.org/resource/", "").replace(">", "");
				germanUrl = germanUrl.replace("<http://de.dbpedia.org/resource/", "").replace(">", "");

				if (isValid(englishUrl)) {
					de_enMap.put(germanUrl, englishUrl);
				} else {
					// ignore
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.err.println("Size of de en map = " + de_enMap.size());
		return de_enMap;
	}

	/**
	 * Check to see if a url is a valid url or not for example
	 * http://dbpedia.org/resource/Category:XXX is not valid but
	 * http://dbpedia.org/resource/XXX is valid
	 * 
	 * @param englishUrl
	 * @return
	 */
	private boolean isValid(String englishUrl) {
		String[] split = englishUrl.split(":");
		if (split.length <= 1) {
			return true;
		} else {
			String firstPart = split[0];
			if (firstPart.equalsIgnoreCase("Category") || firstPart.equalsIgnoreCase("template")
					|| firstPart.equalsIgnoreCase("wikipedia") || firstPart.equalsIgnoreCase("portal")) {
				return false;
			} else
				return true;
		}
	}

}
