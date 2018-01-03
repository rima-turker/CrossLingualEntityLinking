package DataPreparation;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.bytedeco.javacpp.RealSense.context;

import java.util.Map.Entry;
import AnchorDictionaryGenerator.DictioanryGenerator;
import BenchmarkPreparation.EDBanchmark_DataExtraction;
import CompareGTAndDic.CompareConllWithDictionary;
import model.HtmlLink;
import util.Config;
import util.MapUtil;
import util.NERTagger;
import util.StopWordRemoval;
import util.Tuple;
import util.URLUTF8Encoder;

//Map<String,List<String>> readRedirectPages()
public class EvaluationConll {
	private static final Logger LOG = Logger.getLogger(EvaluationConll.class);
	static final Logger resultLog = Logger.getLogger("reportsLogger");

	private static final int TOP_N = Config.getInt("TOPN_CANDIDATES", -1);
	final static DictioanryGenerator dictioanryGenerator = new DictioanryGenerator();
	private final static Map<String, Map<String, Double>> dic = dictioanryGenerator.readDictionryFromFile();
	private static final List<Map<String, List<Helper>>> listResult = new CopyOnWriteArrayList<>();
	private static final AtomicInteger CAN_NOT_CALCULATE_FEATURES = new AtomicInteger(0);
	private static final String RUN_PYTHON_SVM = Config.getString("RUN_PYTHON_SVM", "");
	private static final String MODEL_PYTHON = Config.getString("MODEL_PYTHON", "");
	private static int noCandidateFound = 0;
	private static int trueURInotFoundTOPN = 0;
	private static ExecutorService executor;
	private static int numberOfMentionsProcessed=0;

	public void main(String[] args) {
		System.out.println("TESTA_SENTENCES_CONLL "+Config.getString("TESTA_SENTENCES_CONLL", ""));
		System.out.println(Config.getString("TESTA_SENTENCES_CONLL_MENTIONS", ""));
		System.out.println("TOP_N "+TOP_N);

		try {
			System.err.println("Dictioanry loading ... ");

			Map<String, String> mapContext = new HashMap<>(
					ConllTrainDataGeneration.readSentencesConll(Config.getString("TESTA_SENTENCES_CONLL", "")));
			Map<String, List<Tuple>> mapAnchor = new HashMap<>(
					ConllTrainDataGeneration.readMentionsConll(Config.getString("TESTA_SENTENCES_CONLL_MENTIONS", "")));
			int countSentence = mapAnchor.size();
			int countAnchor = 0;

			for (Entry <String, List<Tuple>> entry : mapAnchor.entrySet())
			{
				countAnchor+=entry.getValue().size();
			}
			System.out.println("CountSentence"+" "+countSentence+"counMention "+countAnchor);

			//			for (Entry<String, String> entry : mapContext.entrySet()) {
			//				final String sentence = entry.getValue();
			//				final List<Tuple> listOfMentions = mapAnchor.get(entry.getKey());
			//				if (listOfMentions == null) {
			//					continue;
			//				}
			//				//							System.out.println("sentence "+sentence);
			//				//							System.out.println("listOfMentions "+listOfMentions);
			//				System.out.println("sentence ID is prosessing "+entry.getKey());
			//				System.out.println("listOfMentions in th sentence "+listOfMentions.size());
			//				handle(sentence, listOfMentions);
			//			}


			executor = Executors.newFixedThreadPool(50);

			for (Entry<String, String> entry : mapContext.entrySet()) {
				final String sentence = entry.getValue();
				final List<Tuple> listOfMentions = mapAnchor.get(entry.getKey());
				if (listOfMentions == null) {
					continue;
				}

				System.out.println("sentence ID is prosessing "+entry.getKey());
				System.out.println("listOfMentions in th sentence "+listOfMentions.size());
				executor.execute(handle(sentence, listOfMentions));
			}

			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

			LOG.info(listResult.size());
			LOG.info("Number of candidates we can not generate features: " + CAN_NOT_CALCULATE_FEATURES);
			LOG.info("trueURInotFoundTOPN "+trueURInotFoundTOPN);

			System.err.println(listResult.size());
			System.err.println("Number of candidates we can not generate features: " + CAN_NOT_CALCULATE_FEATURES);
			System.out.println("trueURInotFoundTOPN "+trueURInotFoundTOPN);

			evaluateTopN(listResult, 1);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void evaluate(List<Map<String, List<Helper>>> result2) {
		int rightlyClassified = 0;
		int allZeroInCandidateList = 0;
		for (Map<String, List<Helper>> e : result2) {
			final List<Helper> collect = e.values().stream().flatMap(List::stream).collect(Collectors.toList());
			final List<Helper> filteredList = collect.stream().filter(p -> p.label == 1).collect(Collectors.toList());
			if (filteredList.isEmpty()) {
				// no 1 found
				allZeroInCandidateList++;
				continue;
			}
			String candidate = null;
			float max = -1f;
			for (Helper h : filteredList) {
				if (h.probability > max) {
					max = h.probability;
					candidate = h.candidate;
				}
			}
			final String trueUrl = e.keySet().iterator().next();
			if (trueUrl.equals(candidate)) {
				rightlyClassified++;
			}
			System.err.println(trueUrl + "\t==>\t" + candidate);
		}

		System.err.println("Total = " + result2.size());
		System.err.println("All Zero In CandidateList = " + allZeroInCandidateList + " -- percentage= "
				+ ((allZeroInCandidateList * 100 * 1.0) / result2.size() * 1.0) + "%");
		System.err.println("rightly classified = " + rightlyClassified + " -- percentage= "
				+ ((rightlyClassified * 100 * 1.0) / (result2.size() * 1.0 - allZeroInCandidateList * 1.0)) + "%");

	}

	public static void evaluateTopN(List<Map<String, List<Helper>>> result2, int n) {

		int rightlyClassified = 0;
		int allZeroInCandidateList = 0;
		for(Map<String, List<Helper>> e:result2){
			final List<Helper> collect = e.values().stream().flatMap(List::stream).collect(Collectors.toList());
			final List<Helper> filteredList = collect.stream().filter(p->p.label==1).collect(Collectors.toList());
			if(filteredList.isEmpty()){
				//no 1 found
				allZeroInCandidateList++;
				continue;
			}
			List<String> candidates = new ArrayList<>();
			Collections.sort(filteredList, (arg0, arg1) -> {
				if (arg1.probability - arg0.probability > 0) {
					return 1;
				} else {
					return -1;
				}
			});
			for(int i=0;i<n&&i<filteredList.size();i++){
				candidates.add(filteredList.get(i).candidate);
			}

			final String trueUrl = e.keySet().iterator().next();
			if(candidates.contains(trueUrl)){
				rightlyClassified++;
			}else{
				System.err.println(trueUrl+"\t==>\t"+candidates);
				LOG.info(trueUrl+"\t==>\t"+candidates);
			}
		}
		//		final Map<String,List<String>> redirectPages =  new HashMap<>(dictioanryGenerator.readRedirectPages());
		//		int rightlyClassified = 0;
		//		int allZeroInCandidateList = 0;
		//		for (Map<String, List<Helper>> e : result2) {
		//			final List<Helper> collect = e.values().stream().flatMap(List::stream).collect(Collectors.toList());
		//			// get the elements that their label is 1 filter the labels 0
		//			final List<Helper> filteredList = collect.stream().filter(p -> p.label == 1).collect(Collectors.toList()); 
		//			if (filteredList.isEmpty()) {
		//				// no 1 found
		//				allZeroInCandidateList++;
		//				continue;
		//			}
		//			List<String> candidates = new ArrayList<>();
		//			Collections.sort(filteredList, (arg0, arg1) -> {
		//				if (arg1.probability - arg0.probability > 0) {
		//					return 1;
		//				} else {
		//					return -1;
		//				}
		//			});
		//			for (int i = 0; i < n && i < filteredList.size(); i++) {
		//				candidates.add(filteredList.get(i).candidate);
		//			}
		//			boolean classified=false;
		//			final String trueUrl = e.keySet().iterator().next();
		//			if (candidates.contains(trueUrl)) {
		//				rightlyClassified++;
		//				classified=true;
		//			}
		//			else {
		//				for (String candidate: candidates) {
		//					if (redirectPages.containsKey(candidate)) 
		//					{
		//						if (redirectPages.get(candidate).contains(trueUrl)) {
		//							rightlyClassified++;
		//							classified=true;
		//							break;
		//							
		//						}
		//					}
		//				}
		//			}
		//			if(!classified) {
		//				System.err.println(trueUrl + "\t==>\t" + candidates);
		//			}
		//	}


		LOG.info("Total = " + result2.size());
		LOG.info("All Zero In CandidateList = " + allZeroInCandidateList + " -- percentage= "
				+ ((allZeroInCandidateList * 100 * 1.0) / result2.size() * 1.0) + "%");
		LOG.info("rightly classified = " + rightlyClassified + " -- percentage= "
				+ ((rightlyClassified * 100 * 1.0) / (result2.size() * 1.0 - allZeroInCandidateList * 1.0)) + "%");

		System.err.println("Total = " + result2.size());
		System.err.println("All Zero In CandidateList = " + allZeroInCandidateList + " -- percentage= "
				+ ((allZeroInCandidateList * 100 * 1.0) / result2.size() * 1.0) + "%");
		System.err.println("rightly classified = " + rightlyClassified + " -- percentage= "
				+ ((rightlyClassified * 100 * 1.0) / (result2.size() * 1.0 - allZeroInCandidateList * 1.0)) + "%");

	}

	/**
	 * This function gets a sentence and it is mentions which should be disambiguated
	 *  
	 * @param sentence
	 * @param listOfMentions
	 */
	//private void handle(final String sentence, final List<Tuple> listOfMentions) {
	private Runnable handle(final String sentence, final List<Tuple> listOfMentions) {
		return () -> {
			String cleanSentence = null;
			double[] sentenceVector = null;
			cleanSentence = sentence.toLowerCase();
			cleanSentence = util.StringUtil.removePuntionation(cleanSentence);
			String afterTrim = cleanSentence.trim().replaceAll(" +", " ");

			sentenceVector = ConllTrainDataGeneration.generateSentenceVector(StopWordRemoval.removeStopWords(afterTrim));
			for (final Tuple t : listOfMentions) {
				final String anchorText = t.getA_mention();
				final String trueURL = t.getB_link().toLowerCase();

				final Map<String, List<Helper>> trueUrlMap = new HashMap<>();

				Map<String, Double> candidateList = dic.get(anchorText);
				if (candidateList == null || candidateList.isEmpty()) {
					//notFoundInDic++;
				}
				// list contains true URI

				if (dicContainsCandidateTopN(anchorText, trueURL)) {
					candidateList = dic.get(anchorText);
				} else if (dicContainsCandidateTopN(anchorText.toLowerCase(), trueURL)) {
					candidateList = dic.get(anchorText.toLowerCase());
				} else if (dicContainsCandidateTopN(CompareConllWithDictionary.nameCapitilize(anchorText.toLowerCase()),
						trueURL)) {
					candidateList = dic.get(CompareConllWithDictionary.nameCapitilize(anchorText.toLowerCase()));
				}

				else {
					trueURInotFoundTOPN++;
				}

				if (candidateList != null) {

					final List<String> topNCandicates = MapUtil.getFirstNElementInList(candidateList, TOP_N);
					final List<Helper> listOfHelper = new ArrayList<>();
					for (String candidate : topNCandicates) {

						List<Tuple> lst = new ArrayList<>(
								enrichMentionListForCoherancyCalculation(anchorText, listOfMentions, trueURL));

						double f1 = ConllTrainDataGeneration.generateFeatureFromSentenceAndEntities(
								ConllTrainDataGeneration.prepareLink(candidate), sentenceVector);
						double f2 = ConllTrainDataGeneration.generateFeatureEntityCoherency(lst,
								ConllTrainDataGeneration.prepareLink(candidate));
						double f3 = ConllTrainDataGeneration.generateFeatureFromMentionsAndEntities(
								ConllTrainDataGeneration.prepareSurfaceform(anchorText),
								ConllTrainDataGeneration.prepareLink(candidate));

						if (f1 == 0.0 && f2 == 0.0 && f3 == 0.0) {
							CAN_NOT_CALCULATE_FEATURES.incrementAndGet();
							continue;
						}
						//
						// final String[] onlyFeatures =
						// features.toString().split("\t\t");
						// final Tuple pythonResult =
						// getPythonResult(onlyFeatures[1].replaceAll("\t", ","));
						// System.out.println(f1+","+f2+","+f3);

						String features = f1 + "," + f2 + "," + f3;
						final Tuple pythonResult = getPythonResult(features);
						try {
							final Helper helperCandidateList = new Helper(candidate, Float.parseFloat(pythonResult.getA_mention()),
									Float.parseFloat(pythonResult.getB_link()));
							listOfHelper.add(helperCandidateList); // for each candidate you have  [candidate=romesh_kaluwitharana, label=1.0,probability=0.9456178] and you keep them all inthe same list
							resultLog.info(anchorText+"\t\t"+trueURL+"\t\t"+candidate+"\t"+Float.parseFloat(pythonResult.getA_mention())+"\t"+Float.parseFloat(pythonResult.getB_link()));
							numberOfMentionsProcessed++;
						} catch (Exception e) {
							System.out.println("I am in the catch block");
							System.out.println(pythonResult);
							System.out.println(features);
						}

					}
					trueUrlMap.put(trueURL, listOfHelper); // true URI and candiates  result
					listResult.add(trueUrlMap); // list of map preparing for evaluation
				}

			}
			System.out.println("numberOfCandidates "+numberOfMentionsProcessed);
		};
	}
	private List<Tuple> enrichMentionListForCoherancyCalculation(String anchorText, List<Tuple> list,
			final String trueURL) {
		List<Tuple> result = new ArrayList<>();
		Tuple con = new Tuple(anchorText, trueURL);
		result.add(con);

		for (Tuple element : list) {
			if (!trueURL.equals(element.getB_link())) {

				final Map<String, Double> candidateList = dic.get(element.getA_mention());
				if (candidateList != null) {
					for (Entry<String, Double> entry : candidateList.entrySet()) {
						if (entry.getValue() > 0.90) {
							result.add(new Tuple(" ", entry.getKey()));
							break;
						}
					}
				}
			}
		}
		return result;
	}

	// private Runnable handle(final String sentence, final List<Tuple> list) {
	// return () -> {
	// String cleanSentence = null;
	// double[] sentenceVector = null;
	// cleanSentence = sentence.toLowerCase();
	// cleanSentence=util.StringUtil.removePuntionation(cleanSentence);
	// String afterTrim = cleanSentence.trim().replaceAll(" +", " ");
	//
	//
	// sentenceVector =
	// ConllTrainDataGeneration.generateSentenceVector(StopWordRemoval.removeStopWords(
	// afterTrim));
	//
	// Map<String, String> testSentences= new
	// HashMap<>(ConllTrainDataGeneration.readSentencesConll(Config.getString("TESTA_SENTENCES_CONLL","")));
	// Map<String, List<Tuple>> testMentions = new
	// HashMap<>(ConllTrainDataGeneration.readMentionsConll(Config.getString("TESTA_SENTENCES_CONLL_MENTIONS",
	// "")));
	//
	// int notFoundInDic=0;
	// for (final Tuple t : list) {
	// final String anchorText = t.getA_mention();
	// final String trueURL = t.getB_link().toLowerCase();
	//
	// final Map<String, List<Helper>> trueUrlMap = new HashMap<>();
	//
	// Map<String, Double> candidateList = dic.get(anchorText);
	// if (candidateList == null || candidateList.isEmpty()) {
	// notFoundInDic++;
	// }
	// //list contains true URI
	// if
	// (dicContainsCandidateTopN(anchorText,trueURL)||dicContainsCandidateTopN(anchorText.toLowerCase(),trueURL)
	// ||dicContainsCandidateTopN(CompareConllWithDictionary.nameCapitilize(anchorText.toLowerCase()),trueURL))
	// {
	// final List<String> topNCandicates =
	// MapUtil.getFirstNElementInList(candidateList, TOP_N);
	// //
	// final List<Helper> listOfHelper = new ArrayList<>();
	// for (String candidate : topNCandicates) {
	//
	// List< Tuple> lst = new
	// ArrayList<>(enrichMentionListForCoherancyCalculation(anchorText, list,
	// trueURL));
	//
	// double f1 =
	// ConllTrainDataGeneration.generateFeatureFromSentenceAndEntities(ConllTrainDataGeneration.prepareLink(candidate),sentenceVector);
	// double f2 =
	// ConllTrainDataGeneration.generateFeatureEntityCoherency(lst,ConllTrainDataGeneration.prepareLink(candidate));
	// double f3 =
	// ConllTrainDataGeneration.generateFeatureFromMentionsAndEntities(ConllTrainDataGeneration.prepareSurfaceform(anchorText),ConllTrainDataGeneration.prepareLink(candidate));
	//
	//
	// if (f1==0.0&&f2==0.0&&f3==0.0 ) {
	// CAN_NOT_CALCULATE_FEATURES.incrementAndGet();
	// continue;
	// }
	// //
	// //final String[] onlyFeatures = features.toString().split("\t\t");
	// //final Tuple pythonResult =
	// getPythonResult(onlyFeatures[1].replaceAll("\t", ","));
	// //System.out.println(f1+","+f2+","+f3);
	//
	// String features = f1+","+f2+","+f3;
	// final Tuple pythonResult = getPythonResult(features);
	//
	// try {
	// final Helper helper = new Helper(candidate,
	// Float.parseFloat(pythonResult.getA_mention()),
	// Float.parseFloat(pythonResult.getB_link()));
	// } catch (Exception e) {
	// System.out.println("I am in the catch block");
	// System.out.println(pythonResult);
	// System.out.println(features);
	// }
	// final Helper helper = new Helper(candidate,
	// Float.parseFloat(pythonResult.getA_mention()),
	// Float.parseFloat(pythonResult.getB_link()));
	// listOfHelper.add(helper); //for each candidate you have
	// [candidate=romesh_kaluwitharana, label=1.0, probability=0.9456178] and
	// you keep them all in the same list
	// }
	// trueUrlMap.put(trueURL, listOfHelper); // true URI and candiates result
	// listResult.add(trueUrlMap); //list of map preparing for evaluation
	// }
	// else{
	// trueURInotFoundTOPN++;
	// }
	// }
	// };
	// }

	public static boolean dicContainsCandidateTopN(String mention, String trueUrl) {

		Map<String, Double> urlCandidates = dic.get(mention);
		if (urlCandidates == null) {
			return false;
		} else {
			List<String> topNCandicates = MapUtil.getFirstNElementInList(urlCandidates, TOP_N);
			topNCandicates = normalize(topNCandicates);
			if (topNCandicates.contains(trueUrl)) {
				return true;
			} else
				return false;
		}
	}

	private static List<String> normalize(List<String> topNCandicates) {
		final List<String> result = new ArrayList<>();
		for (String string : topNCandicates) {
			result.add(URLUTF8Encoder.decodeJavaNative(string));
		}
		return result;
	}

	/**
	 * This function should be reimplemented again as it is shity now
	 * 
	 * @param features
	 * @return
	 */
	private static Tuple getPythonResult(String features) {
		try {
			String command = RUN_PYTHON_SVM + " " + features.toString() + " " + MODEL_PYTHON;
			// command = "python
			// /home/rima/playground/PythonProjects/SVM_TEST.py " +
			// features.toString()
			// + " /home/rima/playground/PythonProjects/Single_SVC";
			// System.out.println(command);
			final Process p = Runtime.getRuntime().exec(command);
			final BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			final BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String s;
			boolean fisrtLine = true;
			String a = null;
			String b = null;
			while ((s = stdInput.readLine()) != null) {
				if (fisrtLine) {
					a = s.replace("[", "").replace("]", "").trim();
					fisrtLine = false;
				} else {
					b = s.replace("[[", "").replace("]]", "").split("  ")[1];
				}
			}
			while ((s = stdError.readLine()) != null) {
				// System.err.println(s);
			}
			return new Tuple(a, b);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
