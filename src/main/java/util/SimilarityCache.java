package util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Deprecated
public class SimilarityCache {
	private static final Map<String, Double> cache = Collections.synchronizedMap(new HashMap<>());

	public static Double getWordVector(String word1,String word2) {
		final String key1 = word1+"\t"+word2;
		final String key2 = word2+"\t"+word1;
		
		final Double sim1 = cache.get(key1);
		final Double sim2 = cache.get(key2);
		
		if(sim1!=null){
			return sim1;
		}
		else if(sim2!=null){
			return sim2;
		} else {
			return null;
		}
	}

	public static void add(String word1, String word2, double similarity) {
		final String key1 = word1+"\t"+word2;
		cache.put(key1, similarity);
	}

	public static int getSize() {
		return cache.size();
	}
}

