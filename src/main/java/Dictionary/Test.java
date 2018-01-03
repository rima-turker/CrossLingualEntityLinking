package Dictionary;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import model.HtmlLink;
import util.MapUtil;
import util.Tuple;
import util.URLUTF8Encoder;

public class Test 
{
	public static void main(String[] args) {


		List<Tuple> gt = new ArrayList<>();
		Tuple t = new Tuple("Volodymyr Yaniv",URLUTF8Encoder.decodeJavaNative("Volodymyr Yaniv"));
		//gt.add(t);
		String s = "Aubrey Schenck";
		t = new Tuple(s,"Volodymyr Yaniv");
		//gt.add(t);
		s= "$";
		t = new Tuple(s,URLUTF8Encoder.decodeJavaNative("Canadian%20dollar"));
		//gt.add(t);
		t = new Tuple("#","Checkmate");
		gt.add(t);

		String fileDict="/home/rtue/workspace/CrossLingualEntityLinking/src/main/resources/test_result";
		Map<String,Map<String,Double>> dictionary = new HashMap<String,Map<String,Double>>(readDictionryFromFile(fileDict));
				for(Entry<String, Map<String, Double>> ent : dictionary.entrySet())
				{
					System.out.println(ent);
				}
		Map<String, Integer> mapAnchorTop = new HashMap<String, Integer>();
		for (Tuple touple: gt) 
		{
			String anchor = touple.getA_mention();
			String gt_link = touple.getB_link();

			if (dictionary.containsKey(anchor))
			{
				Map<String, Double> mapFirstNCandidates = new LinkedHashMap<String, Double>(
						MapUtil.getFirstNElement(MapUtil.sortByValueDescending(dictionary.get(anchor)), 50));

				Map<String, Double> mapFirstNCandidates_decoded = new LinkedHashMap<String, Double>();

				List<String> lstFirstNCandidates_decoded = new LinkedList<String>();
				
				for (Entry<String, Double> entry:mapFirstNCandidates.entrySet()) 
				{
					lstFirstNCandidates_decoded.add(URLUTF8Encoder.decodeJavaNative(entry.getKey()));
					
				}
				mapAnchorTop.put(anchor,DictionaryTest.findTopN(lstFirstNCandidates_decoded, gt_link));
			}
		}
		//System.out.println("Bitti");



	}

	public static Map<String,Map<String,Double>> readDictionryFromFile(String file) {
		Map<String,Map<String,Double>> result = new ConcurrentHashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
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
		} catch (IOException e) {
			e.printStackTrace();
		}		
		return result;
	}
}

