package CompareGTAndDic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import AnchorDictionaryGenerator.DictioanryGenerator;
import DataPreparation.EDBanchmark_DataExtraction;
import util.Config;
import util.MapUtil;
import util.Tuple;
import util.URLUTF8Encoder;

public class CompareGroundTruthWithDictionary {

	private static final int TOP_N = Config.getInt("TOPN_CANDIDATES", -1);

	public static void main(String[] args) {
		// Read DIC
		System.err.println("Dictioanry loading ... ");
		final Map<String, Map<String, Double>> dic = DictioanryGenerator.readDictionryFromFile();
		System.err.println("Dictioanry loaded");
		// READ GT
		System.err.println("GT loading ... ");
		EDBanchmark_DataExtraction dataClean = new EDBanchmark_DataExtraction();
		final Map<String, List<Tuple>> mapAnchor = new HashMap<>(dataClean.getAnchorsGT());
		final List<Tuple> gt = new ArrayList<>(generateGT_touple(mapAnchor));

		System.err.println("GT loaded");
		long inTopN = 0;
		long canNotFound = 0;
		long foundButNotInTopN = 0;
		for (Tuple tp : gt) {
			final String mention = tp.getA();
			final String url = tp.getB().toLowerCase();

			final Map<String, Double> urlCandidates = dic.get(mention);
			if (urlCandidates == null) {
				canNotFound++;
			} else {
				List<String> topNCandicates = MapUtil.getFirstNElementInList(urlCandidates, TOP_N);
				topNCandicates = normalize(topNCandicates);
				if (topNCandicates.contains(url)) {
					inTopN++;
				} else {					
					foundButNotInTopN++;
				}
			}
		}
		System.err.println("Out of " + gt.size() + ", " + inTopN + " exist in top " + TOP_N);
		System.err.println("Can not found in dictionary "+canNotFound);
		System.err.println("Exist in dictionary but not in top  "+TOP_N+ ", "+foundButNotInTopN);
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
				final Tuple newT = new Tuple(t.getA(),
						t.getB().replace("<http://dbpedia.org/resource/", "").replace("> ;", ""));
				result.add(newT);
			}
		}
		return result;
	}
}
