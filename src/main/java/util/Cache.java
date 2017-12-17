package util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Cache {
	private static final Map<String, double[]> cache = Collections.synchronizedMap(new HashMap<>());

	public static double[] getWordVector(String word) {
		final double[] ds = cache.get(word);
		if (ds != null) {
			return ds;
		} else {
			final double[] wordVector = Request_word2vecServer.getWordVector(word);
			cache.put(word, wordVector);
			return wordVector;
		}
	}

	public static int getSize() {
		return cache.size();
	}
	
	
}
