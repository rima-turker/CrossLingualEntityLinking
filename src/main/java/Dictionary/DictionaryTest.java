
package Dictionary;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.helpers.Util;

import AnchorDictionaryGenerator.DictioanryGenerator;
import BenchmarkPreparation.EDBanchmark_DataExtraction;
import CompareGTAndDic.EntityCandidateGenerator_yovisto;
import util.Config;
import util.MapUtil;
import util.NERTagger;
import util.Tuple;
import util.URLUTF8Encoder;

public class DictionaryTest 
{
	/***
	 * 
	 * This function tests the anchorlink dictionary which is created from
	 * wikipedia anchors. Basically we have GT comes from Named Entity benchmark dataset and
	 * and we have a DICTIONAYwe want to calculate the recall or topN where we find the correct
	 * entity from the dictionary  
	 * dictionary links decoded as well
	 *
	 * @param gt list<anhorText,decodedLink > groundTruth for now ELbenchmarks
	 * @param dictionary Map<anchorText map<link,probability>>
	 */
	private static final int topNCandidates = Config.getInt("TOPN_CANDIDATES", 0);
	
	
	public void testAnchorLinkDictionary() 
	{
		EDBanchmark_DataExtraction dataClean = new EDBanchmark_DataExtraction();
		Map <String, List<Tuple>> mapAnchor = new HashMap<>(dataClean.getAnchorsGT());
		List<Tuple> gt = new ArrayList<>(generateGT_touple(mapAnchor));
		DictioanryGenerator dictioanryGenerator = new DictioanryGenerator();
		Map<String,Map<String,Double>> dictionary = new ConcurrentHashMap<>(dictioanryGenerator.readDictionryFromFile());
		
		System.out.println("dictionaries are ready");
		List<Integer> lstTop = new ArrayList<>();
		List<String> lstnotInDict = new ArrayList<>();
		
		Map<String, Integer> mapAnchorTop = new HashMap<String, Integer>();
		int countFound=0;
		int notFound=0;
		int countFound_lowerCase=0;
		
		for (Tuple touple: gt) 
		{
			//TODO: add another fuction for normalization
			String anchor = touple.getA_mention().replace("  ", " ").replace(" 's", "'s");
			String gt_link = touple.getB_link();
			
			if (dictionary.containsKey(anchor))
			{
				countFound++;
				final Map<String, Double> links = new HashMap<String, Double>();
				
				final Map<String, Double> mapFirstNCandidates = new LinkedHashMap<String, Double>(
						MapUtil.getFirstNElement(MapUtil.sortByValueDescending(dictionary.get(anchor)), topNCandidates));
				final List<String> lstFirstNCandidates_decoded = new LinkedList<String>();
				
				for (Entry<String, Double> entry:mapFirstNCandidates.entrySet()) 
				{
					lstFirstNCandidates_decoded.add(URLUTF8Encoder.decodeJavaNative(entry.getKey()));
					
				}
				mapAnchorTop.put(anchor,DictionaryTest.findTopN(lstFirstNCandidates_decoded, gt_link));
				
				int findN=findTopN(mapFirstNCandidates, anchor);
				
				if (findN!=-1) 
				{
					lstTop.add(findN);
					
				}
				else
					System.out.println("not in top 50"+" "+anchor+" "+ gt_link);
				
			}
			
			else
			{
				notFound++;
				lstnotInDict.add(anchor);
				System.out.println("dictionary does not contain Anchor"+" "+anchor);
			}
		}
		//mapAnchorTop.forEach((k,v)-> System.out.println(k+", "+v));
		System.out.println("Entities in the dictionary "+ countFound+", Entities topN "+
		topNCandidates+" "+lstTop.size()+", entities lower case "+countFound_lowerCase+
		", notFound in the dictionary "+lstnotInDict.size());
	}
	private  List<Tuple> generateGT_touple(Map <String, List<Tuple>> mapAnchor) 
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
	
	public double calculateRecall()
	{
		double recall=0.0;
		EDBanchmark_DataExtraction dataClean = new EDBanchmark_DataExtraction();
		
		Map <String, String> mapContext = new HashMap<>(dataClean.getContextData());
		Map <String, List<Tuple>> mapAnchor = new HashMap<>(dataClean.getAnchorsGT());
		Map <String, List<Tuple>> mapTagged = new HashMap<>();
		
		
//		String exampleSentence = "US and British reulators have already fined Barclays , based in Britain , US$ 453 million for submitting false information between 2005 and 2009 to keep the interest rate , known as LIBOR , low .";
//		exampleSentence = "Albert Einstein comes from Germany";
//		System.out.println(tagSentence(exampleSentence));
		
		System.out.println("Sentences could not tagged with the corresponding anchor");
		System.out.println("id sentence anchor");
		
		int countTagged=0;
		for (Entry<String, String> entry: mapContext.entrySet()) 
		{
//			System.out.println(entry.getValue());
//			System.out.println(entry.getKey()+" "+tagSentence(entry.getValue()));
			List<Tuple> temp = new ArrayList<>(tagSentence(entry.getValue()));
			mapTagged.put(entry.getKey(),  temp);
			countTagged+=temp.size();
		}
//		System.out.println("countTagged "+countTagged);
//		for (int i = 0; i <500; i++) {
//			//System.out.println(mapTagged.get(String.valueOf(i)));
//		}
		//mapTagged.forEach((k,v)-> System.out.println(k+", "+v));
		int totalAnchorCount=0;
		int countNoCandidateFound = 0;
		int countCandidateFound = 0;
		int countNotTagged = 0;
		
		
		//iterate over mapAnchor which contains ID of the sentence + Anchors of sentence and their correct entity
		for (Entry<String, List<Tuple>> entry: mapAnchor.entrySet()) 
		{
			String id= entry.getKey();
			//System.out.println(entry.getValue());
			List<Tuple> lstAnchors = entry.getValue();
			
			//iterate over anchors
			for(Tuple tou:lstAnchors)
			{
				totalAnchorCount++;
				String anchor= tou.getA_mention(); //anchor itself
				String begTemp="<http://dbpedia.org/resource/";
				String endTemp="> ;";
				
				String gt = tou.getB_link().substring(begTemp.length(), tou.getB_link().indexOf(endTemp)).toLowerCase();
				//corresponding groundTruth
				
				/*See if this anchor already being tagged as a person, location or place
				 * If it is not tagged then no candidate is generated
				 * 
				 * */
				if (mapTagged.containsKey(id)) {
					
					List<Tuple> lstTagged = new ArrayList<>(mapTagged.get(id));
					
					boolean taggedAnchor = false;
					/*
					 * If you have the same anchor multiple times in the same sentence and each refers to different
					 * type then this will not work properly because as soon as you found the anchor you break
					 * now no break
					 */
					
					//int countTheSameAnchor =0;
					for(Tuple touple:lstTagged)
					{
						//it is tagged then you can generate candidates 
						if (touple.getA_mention().equals(anchor)) 
						{
							//System.out.println(touple.getB());
							List<String> candidates = new LinkedList<>(EntityCandidateGenerator_yovisto.getCandidateList(anchor, touple.getB_link()));
							if (candidates.contains(gt))
							{
								countCandidateFound++;
								System.out.println(anchor+"\t"+gt+"\t"+findTopN(candidates,gt));
								//System.out.println("YES");
							}
							else
							{
								countNoCandidateFound++;
								//System.err.println(touple.getA().equals(anchor));
							}
							//countTheSameAnchor++;
							taggedAnchor=true;
							break;
						}
					}
//					if (countTheSameAnchor>1) {
//						System.out.println("countTheSameAnchor "+ countTheSameAnchor);
//					}
					if (!taggedAnchor) {
						//System.out.println(id+"\t"+mapContext.get(id)+"\t"+anchor);
						countNotTagged+=1;
					}
				}
				
				else
				{
					System.err.println("There is no corresponding element int mapTagged, sentence ID "+id);
					break;
				}
			}
		}
		System.out.println("totalAnchorCount "+ totalAnchorCount+ ", countCandidateFound "+ countCandidateFound
				+", countNoCandidateFound "+countNoCandidateFound+ " countNotTagged, "+countNotTagged);
		
		return recall;
	}
	
	
	public int  findTopN(Map<String,Double> mapCandidates, String gt)
	{
		int top=0;
		for (Entry<String, Double> entry: mapCandidates.entrySet()) 
		{
			top++;
			if (entry.getKey().equals(gt)) {
				return top;
			}
		}
		return -1;
	}
	public static int  findTopN(List<String> candidates, String gt)
	{
		int top=0;
		
		for (int i = 0; i < candidates.size(); i++) 
		{
			top++;
			if (candidates.get(i).equals(gt)) {
				return top;
			}
		}
		return -1;
	}
	
	public List<Tuple> tagSentence(String sentence) 
	{
		List<Tuple> listTagged = new ArrayList<Tuple>();
		String strTagged = NERTagger.getNERTags(sentence);
		//System.out.println(strTagged);
		
		final String[] strSplit = strTagged.split("\t");
		
		if (strSplit.length>1) {
			for (int i = 0; i < strSplit.length; i+=2) 
			{
				listTagged.add(new Tuple(strSplit[i], strSplit[i+1]));
//				if (!strSplit[i+1].equals("<ORGANIZATION>") && !strSplit[i+1].equals("<PERSON>")) {
//
//					System.out.println(strSplit[i+1]);
//				}
			}
		}
		return listTagged;
	}
}
