package DataPreparation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import AnchorDictionaryGenerator.DictioanryGenerator;
import model.HtmlLink;
import util.Config;
import util.FileUtil;
import util.HTMLLinkExtractor;
import util.URLUTF8Encoder;

public class NegativeTrainDataGenerator {
	private static final String NAME_OF_NEGATIVE_DATA_SET = "NegativeTrainSet.txt";
	private static final String ADDRESS_OF_POSTIVE_TARINSET = Config.getString("ADDRESS_OF_POSTIVE_TARINSET", "");
	private static final Set<String> negativeTrainSet = new HashSet<>();

	public static void main(String[] args){
		showSizeOfResultPeridically();

		System.err.println("Dictioanry loading ... ");
		DictioanryGenerator dictioanryGenerator = new DictioanryGenerator();
		final Map<String, Map<String, Double>> dic = dictioanryGenerator.readDictionryFromFile();
		System.err.println("Dictioanry loaded");

		final HTMLLinkExtractor htmlLinkExtractor = new HTMLLinkExtractor();
		String line=null;
		try (BufferedReader br = new BufferedReader(new FileReader(ADDRESS_OF_POSTIVE_TARINSET)))
		{
			while ((line = br.readLine()) != null) 
			{
				final String sentence = line.split("\t\t")[0];
				StringBuilder result = new StringBuilder(sentence);
				final Vector<HtmlLink> links = htmlLinkExtractor.grabHTMLLinks(sentence);
				int offset = 0;
				for(final HtmlLink link:links) {
					final String mention = link.getAnchorText();
					final String decodedTrueUrl = link.getDecodedUrl();

					final Map<String, Double> urlCandidates = dic.get(mention);

					if (urlCandidates == null || urlCandidates.size()==1) {
						result = new StringBuilder(result.replace(link.getStart()+offset, link.getEnd()+offset, mention));
						offset += (mention.length() - (link.getEnd() - link.getStart()));
					} else {
						final List<String> allCandicates = new ArrayList<>(urlCandidates.keySet());
						allCandicates.removeIf(p-> URLUTF8Encoder.decodeJavaNative(p).equalsIgnoreCase(decodedTrueUrl));
						if(allCandicates.size()<=0){
							result = new StringBuilder(result.replace(link.getStart()+offset, link.getEnd()+offset, mention));
							offset += (mention.length() - (link.getEnd() - link.getStart()));
						}
						else{
							final Random rn = new Random();
							final int randomNum = rn.nextInt(allCandicates.size());
							String candidate=allCandicates.get(randomNum);
							String anchorText = generateAnchorTextWrongLink(link,candidate);
							result = new StringBuilder(result.replace(link.getStart()+offset, link.getEnd()+offset, anchorText));
							offset += (anchorText.length() - (link.getEnd() - link.getStart()));
						}
					}
				}
				if (result.toString().contains("<a href=\"")&&result.toString().contains("</a>")) {
					negativeTrainSet.add(result.toString());
				}
			}
			FileUtil.writeDataToFile(new ArrayList<>(negativeTrainSet), NAME_OF_NEGATIVE_DATA_SET, false);
		}
		catch (IOException e) {
			System.err.println("Error at line== "+line);
			e.printStackTrace();
		}
	}

	private static void showSizeOfResultPeridically() {
		final Thread t = new Thread(() -> {
			while(true){
				System.err.println("Number of generated trainset= "+negativeTrainSet.size());
				try {
					Thread.sleep(TimeUnit.SECONDS.toMillis(5));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
	}

	private static String generateAnchorTextWrongLink(HtmlLink link, String selectedCandidate) {
		final StringBuilder result = new StringBuilder();
		result.append("<a href=\"");
		result.append(selectedCandidate).append("\">");
		result.append(link.getAnchorText()).append("</a>");
		return result.toString();
	}

}
