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

import org.bytedeco.javacpp.RealSense.context;

import java.util.Map.Entry;
import AnchorDictionaryGenerator.DictioanryGenerator;
import model.HtmlLink;
import util.Config;
import util.MapUtil;
import util.Tuple;
import util.URLUTF8Encoder;

public class Evaluation {
	private static final int TOP_N = Config.getInt("TOPN_CANDIDATES", -1);
	private static final Map<String, Map<String, Double>> dic = DictioanryGenerator.readDictionryFromFile();
	private static final List<Map<String, List<Helper>>> result = new CopyOnWriteArrayList<>();
	private static final AtomicInteger CAN_NOT_CALCULATE_FEATURES = new AtomicInteger(0);
	private static ExecutorService executor;

	public static void main(String[] args) {
		try{
			final EDBanchmark_DataExtraction dataClean = new EDBanchmark_DataExtraction();
			// mapContext stores sentenceID and the sentence itself
			final Map<String, String> mapContext = new HashMap<>(dataClean.getContextData());
			// mapAnchor stores sentenceID and the sentence's anchor and true URI
			final Map<String, List<Tuple>> mapAnchor = new HashMap<>(dataClean.getAnchorsGT());

			executor = Executors.newFixedThreadPool(55);

			for (Entry<String, String> entry : mapContext.entrySet()) {
				final String sentence = entry.getValue();
				final List<Tuple> list = mapAnchor.get(entry.getKey());
				if (list == null) {
					continue;
				}
				executor.execute(handle(sentence, list));
			}

			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

			System.err.println(result.size());
			System.err.println("Number of sentence we can not generate features: " + CAN_NOT_CALCULATE_FEATURES);
			evaluateTopN(result,1);
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void evaluate(List<Map<String, List<Helper>>> result2) {
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
			String candidate = null;
			float max = -1f;
			for(Helper h:filteredList){
				if(h.probability>max){
					max = h.probability;
					candidate = h.candidate;
				}
			}
			final String trueUrl = e.keySet().iterator().next();
			if(trueUrl.equals(candidate)){
				rightlyClassified++;
			}
			System.err.println(trueUrl+"\t==>\t"+candidate);
		}
		
		System.err.println("Total = "+result2.size());
		System.err.println("All Zero In CandidateList = "+allZeroInCandidateList+" -- percentage= "+((allZeroInCandidateList*100*1.0)/result2.size()*1.0)+"%");
		System.err.println("rightly classified = "+rightlyClassified+" -- percentage= "+((rightlyClassified*100*1.0)/(result2.size()*1.0-allZeroInCandidateList*1.0))+"%");
		
	}
	
	public static void evaluateTopN(List<Map<String, List<Helper>>> result2,int n) {
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
			}
		}
		
		System.err.println("Total = "+result2.size());
		System.err.println("All Zero In CandidateList = "+allZeroInCandidateList+" -- percentage= "+((allZeroInCandidateList*100*1.0)/result2.size()*1.0)+"%");
		System.err.println("rightly classified = "+rightlyClassified+" -- percentage= "+((rightlyClassified*100*1.0)/(result2.size()*1.0-allZeroInCandidateList*1.0))+"%");
		
	}

	private static Runnable handle(final String sentence, final List<Tuple> list) {
		return () -> {
			for (final Tuple t : list) {
				final String anchorText = t.getA();
				final String trueURL = URLUTF8Encoder.decodeJavaNative(t.getB().replace("<http://dbpedia.org/resource/", "").replace("> ;", "")).toLowerCase();							

				final Map<String, List<Helper>> trueUrlMap = new HashMap<>();

				final Map<String, Double> candidateList = dic.get(anchorText);
				if (candidateList == null || candidateList.isEmpty()) {
					// TODO:
				} else {
					final List<String> topNCandicates = MapUtil.getFirstNElementInList(candidateList, TOP_N);

					final List<Helper> listOfHelper = new ArrayList<>();
					for (String candidate : topNCandicates) {
						HtmlLink link = new HtmlLink();
						link.setAnchorText(anchorText);
						link.setUrl(candidate);
						link.setFullSentence(sentence);
						final StringBuilder features = TrainDataGenerator.processThisLine(sentence,
								new Vector<>(Arrays.asList(link)));
						if (features == null) {
							CAN_NOT_CALCULATE_FEATURES.incrementAndGet();
							continue;
						}

						final String[] onlyFeatures = features.toString().split("\t\t");
						final Tuple pythonResult = getPythonResult(onlyFeatures[1].replaceAll("\t", ","));
						final Helper helper = new Helper(candidate, Float.parseFloat(pythonResult.getA()),
								Float.parseFloat(pythonResult.getB()));
						listOfHelper.add(helper);
					}
					trueUrlMap.put(trueURL, listOfHelper);
					result.add(trueUrlMap);
				}
			}
		};
	}

	/**
	 * This function should be reimplemented again as it is shity now
	 * 
	 * @param features
	 * @return
	 */
	private static Tuple getPythonResult(String features) {
		try {
			String command = "python /home/rima/playground/PythonProjects/SVM_TEST.py " + features.toString()
			+ " /home/rima/playground/PythonProjects/Single_SVC";
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

