package CompareGTAndDic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import AnchorDictionaryGenerator.DictioanryGenerator;
import DataPreparation.ConllTrainDataGeneration;
import util.Config;
import util.MapUtil;
import util.Tuple;
import util.URLUTF8Encoder;

public class CompareConllWithDictionary {

	private static int TOP_N = Config.getInt("TOPN_CANDIDATES", -1);
	final static DictioanryGenerator dictioanryGenerator = new DictioanryGenerator();
	final static Map<String, Map<String, Double>> dic = dictioanryGenerator.readDictionryFromFile();
	public static void main(String[] args) {
		
		for (int i = 50; i <=50; i+=10) {
			
			TOP_N=i;
			Map<String, String> testSentences= new HashMap<>(ConllTrainDataGeneration.readSentencesConll(Config.getString("TESTB_SENTENCES_CONLL","")));
			Map<String, List<Tuple>> testMentions = new HashMap<>(ConllTrainDataGeneration.readMentionsConll(Config.getString("TESTB_SENTENCES_CONLL_MENTIONS", "")));
			int countList=0;
			for(Entry <String, List<Tuple>> e:testMentions.entrySet())
			{
				countList+=e.getValue().size();
			}
			System.out.println("number of mentions which are annotated with wikipedia "+countList+" "+testSentences.size());
			
			// Read DIC
			
			// READ GT
			System.err.println("GT loading ... ");
			final List<Tuple> gt = new ArrayList<>(generateGT_touple(testMentions));

			System.err.println("GT loaded");
			long inTopN = 0;
			long canNotFound = 0;
			long foundButNotInTopN = 0;
			long candidateGeneratedButCorrectURI=0;
			
			for (Tuple tp : gt) {
				final String mention = tp.getA_mention();
				final String url = tp.getB_link().toLowerCase();

				 Map<String, Double> urlCandidates = dic.get(mention);
				if (urlCandidates == null) {
					urlCandidates = dic.get(mention.toLowerCase());
				} 
				if (urlCandidates == null) {
					urlCandidates = dic.get(nameCapitilize(mention.toLowerCase()));
					
				}
				if (urlCandidates == null) {
					canNotFound++;
					System.out.println(mention);
				}
				else {
					if (urlCandidates.containsKey(url)) 
					{
						List<String> topNCandicates = MapUtil.getFirstNElementInList(urlCandidates, TOP_N);
						topNCandicates = normalize(topNCandicates);
						if (topNCandicates.contains(url)) {
							inTopN++;
						} else {					

							foundButNotInTopN++;
						}
					}
					else
					{
						//Candidates are generated but could not found the true URI
						candidateGeneratedButCorrectURI++;
					}
				}
				//candidateGenerationBasedOnCOntextSimilarity(String sentence,urlCandidates);
			}
			System.err.println("Out of " + gt.size() + ", " + inTopN + " exist in top " + TOP_N);
			System.err.println("Cannot found in dictionary - no candidate generated "+canNotFound);
			System.err.println("Correct URI found in candidate list but not in top  "+TOP_N+ ", "+foundButNotInTopN);
			System.err.println("Candidates are generated but correct URI could not found  "+TOP_N+ ", "+candidateGeneratedButCorrectURI);
		}
	}
	private void candidateGenerationBasedOnCOntextSimilarity(String anchor) 
	{
		Map<String, String> testSentences= new HashMap<>(ConllTrainDataGeneration.readSentencesConll(Config.getString("TESTB_SENTENCES_CONLL","")));
		Map<String, List<Tuple>> testMentions = new HashMap<>(ConllTrainDataGeneration.readMentionsConll(Config.getString("TESTB_SENTENCES_CONLL_MENTIONS", "")));
		Map<String, Map<String,Double>> result = new HashMap<>();
		
		for(Entry<String, String> entry: testSentences.entrySet())
		{
//			String sentenceID = entry.getKey();
//			String sentence = entry.getValue();
//			
//			if (testMentions.containsKey(sentenceID)) {
//				double[] sentenceVector = ConllTrainDataGeneration.generateSentenceVector(ConllTrainDataGeneration.prepareForSentenceVectorGeneration(sentence));
//				
//				
//				List<Tuple> tpGroundTruth = new ArrayList<>(testMentions.get(sentenceID));
//				Map<String,Double> temp = new HashMap<>();
//				for (Tuple tp : tpGroundTruth) {
//					final String mention = tp.getA_mention();
//					final String trueURL = tp.getB_link().toLowerCase();
//					Map<String, Double> urlCandidates = dic.get(mention);
//					for
//					double similarity = ConllTrainDataGeneration.generateFeatureFromSentenceAndEntities(ConllTrainDataGeneration.prepareLink(tp.getB_link()),sentenceVector);
//					temp.put(key, value)
//				
//				}

//			}
			
		}

	}
	
	

	/**
	 * decode urls to format of.._.._.._
	 * @param topNCandicates
	 * @return
	 */
	private static List<String> normalize(List<String> topNCandicates) {
		final List<String> result = new ArrayList<>();
		for (String string : topNCandicates) {
			result.add(URLUTF8Encoder.decodeJavaNative(string));
		}
		return result;
	}

	/**
	 * Convert ground truth to tuples
	 * - A is mention
	 * - B is True Url
	 * @param mapAnchor
	 * @return
	 */
	private static List<Tuple> generateGT_touple(Map<String, List<Tuple>> mapAnchor) {
		final List<Tuple> result = new ArrayList<>();
		for (final Entry<String, List<Tuple>> entry : mapAnchor.entrySet()) {
			final List<Tuple> lstTemp = new ArrayList<>(entry.getValue());
			for (final Tuple t : lstTemp) {
				final Tuple newT = new Tuple(t.getA_mention(),
						t.getB_link().replace("<http://dbpedia.org/resource/", "").replace("> ;", ""));
				result.add(newT);
			}
		}
		return result;
	}
	public static String nameCapitilize(String str)
	{
		String[] split = str.split(" "); 
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < split.length; i++) 
		{
			String s1 = split[i].substring(0, 1).toUpperCase();
		    String nameCapitalized = s1 + split[i].substring(1);
		    result.append(nameCapitalized+" ");
		}
		String finalString = result.toString().trim();
	    return finalString;
	}
	
}
