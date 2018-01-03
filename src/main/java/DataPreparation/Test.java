package DataPreparation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.map.HashedMap;

import BenchmarkPreparation.EDBanchmark_DataExtraction;
import util.Tuple;

public class Test {

	public static void main(String[] args) {
		
//		System.err.println(SimilarityCache.getWordVector("rima", "farshad"));
//		SimilarityCache.add("rima", "farshad", 0.5);
//		System.err.println(SimilarityCache.getWordVector("rima", "farshad"));
//		System.err.println(SimilarityCache.getWordVector("farshad","rima"));
//		
		//neue_deutsche_härte

		//neue_deutsche_h%c3%a4rte
		
		EDBanchmark_DataExtraction dataClean = new EDBanchmark_DataExtraction();
		Map <String, List<Tuple>> mapAnchor = new HashMap<>(dataClean.getAnchorsGT());
		Map <String, String> result = new HashedMap<>(dataClean.getContextData());
		List<Tuple> gt = new ArrayList<>(generateGT_touple(mapAnchor));
		int countt=0;
		for (Entry <String, List<Tuple>> ent : mapAnchor.entrySet()) 
		{
			if (ent.getValue().size()>1) {
				countt++;
				System.out.println(result.get(ent.getKey()));
			}
		}
		
		System.out.println();
//		final Helper helper1 = new Helper("XXX1", 1f,0.2f);
//		final Helper helper2 = new Helper("XXX2", 1f,0.1f);
//		final Helper helper3 = new Helper("XXX3", 1f,0.5f);
//		
//		final Map<String, List<Helper>> trueUrlMap = new HashMap<>();
//		trueUrlMap.put("XXX1",Arrays.asList(helper1,helper2,helper3));
//		
//		
//		Evaluation.evaluate(Arrays.asList(trueUrlMap));
//		Evaluation.evaluateTopN(Arrays.asList(trueUrlMap),2);
		
//		String decodedTrueUrl = "Nizam-ı_Cedid";
//		List<String> allCandicates = new ArrayList<>(Arrays.asList("nizam-ı_cedid","nizam-i_cedid"));
//		System.err.println(allCandicates);
//		allCandicates.removeIf(p-> URLUTF8Encoder.decodeJavaNative(p).equalsIgnoreCase(decodedTrueUrl));
//		System.err.println(allCandicates);
//		
//		
//		System.err.println(URLUTF8Encoder.decodeJavaNative("nizam-ı_cedid"));
//		System.err.println(URLUTF8Encoder.decodeJavaNative("nizam-i_cedid"));
//		System.err.println("nizam-ı_cedid".equalsIgnoreCase("nizam-i_cedid"));
//		NERTagger.getNERTags("asd adad ad ");
//		String sent = "<a href=\"dbr:musical%20theatre\">musical i theatre actor</a> is i <a href=\"dbr:germany\">good</a>.";
//		HTMLLinkExtractor htmlLinkExtractor = new HTMLLinkExtractor();
//		Vector<HtmlLink> links = htmlLinkExtractor.grabHTMLLinks(sent);
//
//		System.err.println(TrainDataGenerator.generateFeatureFromSentenceAndEntities(links, TrainDataGenerator.generateSentenceVector("musical theatre")));
//		System.err.println(TrainDataGenerator.generateFeatureFromMentionsAndEntities(links));

//		System.err.println(TrainDataGenerator.generateFeatureFromWordsAndEntities(links, "musical i theatre actor is i good"));
//		sent = "<a href=\"dbr:musical%20theatre\">music</a> is good.";
//		htmlLinkExtractor = new HTMLLinkExtractor();
//		links = htmlLinkExtractor.grabHTMLLinks(sent);
//		System.err.println(TrainDataGenerator.generateFeatureFromSentenceAndEntities(links, TrainDataGenerator.generateSentenceVector("music is good.")));
//
//		sent = "<a href=\"dbr:musical%20theatre\">rima</a> is good.";
//		htmlLinkExtractor = new HTMLLinkExtractor();
//		links = htmlLinkExtractor.grabHTMLLinks(sent);
//		System.err.println(TrainDataGenerator.generateFeatureFromSentenceAndEntities(links, TrainDataGenerator.generateSentenceVector("rima is good.")));
//		
//		sent = "<a href=\"dbr:musical%20theatre\">i</a> is good.";
//		htmlLinkExtractor = new HTMLLinkExtractor();
//		links = htmlLinkExtractor.grabHTMLLinks(sent);
//		System.err.println(TrainDataGenerator.generateFeatureFromSentenceAndEntities(links, TrainDataGenerator.generateSentenceVector("i is good.")));
//		
//		sent = "<a href=\"dbr:musical%20theatre\">i</a>";
//		htmlLinkExtractor = new HTMLLinkExtractor();
//		links = htmlLinkExtractor.grabHTMLLinks(sent);
//		System.err.println(TrainDataGenerator.generateFeatureFromSentenceAndEntities(links, TrainDataGenerator.generateSentenceVector("i")));

		//		final double[] generateSentenceVector = ;
		//		final double[] generateSentenceVector2 = TrainDataGenerator.generateSentenceVector("he i am john");


	}
	private static List<Tuple> generateGT_touple(Map <String, List<Tuple>> mapAnchor) 
	{
		List<Tuple> result = new ArrayList<>();
		
		for (Entry<String, List<Tuple>> entry: mapAnchor.entrySet()) 
		{
			List<Tuple> lstTemp = new ArrayList<>(entry.getValue());
			for(Tuple t : lstTemp)
			{
				Tuple newT = new Tuple(t.getA_mention(),t.getB_link().replace("<http://dbpedia.org/resource/", "").replace("> ;", ""));
				result.add(newT);
			}
		}
		return result;
	}

}
