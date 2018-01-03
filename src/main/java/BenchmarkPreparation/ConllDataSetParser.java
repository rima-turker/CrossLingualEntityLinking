package BenchmarkPreparation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.common.base.FinalizablePhantomReference;

import DataPreparation.ConllTrainDataGeneration;
import util.Config;
import util.ConllData;
import util.FileUtil;
import util.Tuple;

public class ConllDataSetParser {

	private static final String CONLL_MAIN_FILE = Config.getString("CONLL_DATASET", "");
	private static final Logger LOG = Logger.getLogger(ConllDataSetParser.class);

	private Map<String, List<ConllData>> map_train ;
	private Map<String, List<ConllData>> map_testb ;
	
	private List<ConllData> lstSentencesAndMentions_testb = new ArrayList<>();
	private List<ConllData> lstSentencesAndMentions_train = new ArrayList<>();
	
	int  count=0;
	public ConllDataSetParser() {
		readDocbyDoc();
		findCountSentencesAndMentions();
		map_train= new HashMap<>(separateMnetionsByDocID(lstSentencesAndMentions_train));
		map_testb= new HashMap<>(separateMnetionsByDocID(lstSentencesAndMentions_testb));
	}
	private Map<String, List<ConllData>> separateMnetionsByDocID(List<ConllData> lstSentencesAndMentions_testb2) {
		Map<String, List<ConllData>> result = new HashMap<>();
		for(ConllData conll: lstSentencesAndMentions_testb)
		{
			if (result.containsKey(conll.getDocId())) {
				List<ConllData> lst = new ArrayList<>(result.get(conll.getDocId()));
				lst.add(conll);
				result.put(conll.getDocId(), lst);
			}
			else{
				List<ConllData> lst = new ArrayList<>();
				lst.add(conll);
				result.put(conll.getDocId(), lst);
			}
		}
		return result;
	}
	private void findCountSentencesAndMentions() {
		int countMentions=0;
		for (ConllData conll : lstSentencesAndMentions_train) 
		{
			countMentions+=conll.getMentionAndURI().size();
		}
		System.out.println("train sentences "+ lstSentencesAndMentions_train.size());
		System.out.println("train mentions "+ countMentions);

		countMentions=0;
		for (ConllData conll : lstSentencesAndMentions_testb) 
		{
			//System.out.println(conll.getMentionAndURI());
			countMentions+=conll.getMentionAndURI().size();
		}
		System.out.println();
		System.out.println("testb sentences "+ lstSentencesAndMentions_testb.size());
		System.out.println("testb mentions "+ countMentions);
	}

	public void readDocbyDoc() {
		try {
			BufferedReader bf = new BufferedReader(new FileReader(CONLL_MAIN_FILE));
			String line = null;
			String docID = null;
			boolean firstTime = true;
			List<String> doc = new ArrayList<>();
			while ((line = bf.readLine()) != null) {
				if (line.contains("-DOCSTART-")) {
					if (firstTime) {
						docID = line.replace("-DOCSTART- ", "").trim();
						firstTime = false;
						continue;
					}
					//processDoc_old(docID, doc);
					processDoc(docID, doc);
					doc.clear();
					docID = line.replace("-DOCSTART- ", "");
				} else {
					doc.add(line);
				}
			}

			//processDoc_old(docID, doc);
			processDoc(docID, doc);
			bf.close();
		}

		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * This function collects a sentence and its mentions returns it
	 * @param docID
	 * @param lst
	 * @return ConllData
	 */
	private ConllData processSentence(String docID,int sentenceID,List<String> lst) {
		StringBuilder sentence = new StringBuilder();
		List<Tuple> lstOfMetions = new ArrayList<>();
		ConllData conll = new ConllData();
		conll.setDocId(docID);
		conll.setSentenceId(sentenceID);
		for (String line : lst) {
			sentence.append(line.split("\t")[0]+" ");
			Charset.forName("UTF-8").encode(line);
			String[] elem = line.split("\t");
			if ((elem.length) >= 4) {
				//Tuple tp = new Tuple();
				String isCompleteMention = elem[1].trim();
				if (isCompleteMention.equals("B")) {
					String mention = elem[2].trim();
					String entity = elem[3];
					if (!entity.equalsIgnoreCase("--NME--")) {
						if (docID.contains("testb")) {
							count++;
						}
						String wikiLink = elem[4].trim();
						lstOfMetions.add(new Tuple(mention.trim(),wikiLink.replace("http://en.wikipedia.org/wiki/", "").trim().toLowerCase()));
					} 
				}
			}
		}
		String sentenceFinal = sentence.toString().replaceAll("\\s+", " ").trim();
		conll.setSentence(sentenceFinal);
		conll.setMentionAndURI(lstOfMetions);
		return conll;

	}

	private void processDoc(String docID, List<String> doc) {

		List<String> aSentenceAndMentions = new ArrayList<>();
		int sentenceID=0;
		
		// iterate over one doc line by line
		for (int i = 0; i < doc.size(); i++) {
			String line = doc.get(i);
			Charset.forName("UTF-8").encode(line);
			aSentenceAndMentions.add(line);
			if ((line.equals("") && doc.get(i - 1).equals("."))) { // new sentence
				if (docID.contains("testb")) {
					lstSentencesAndMentions_testb.add(processSentence(docID, sentenceID,aSentenceAndMentions));
				} else if (!docID.contains("testa")) {
					lstSentencesAndMentions_train.add(processSentence(docID, sentenceID,aSentenceAndMentions));
				}
				sentenceID++;
				aSentenceAndMentions.clear();
			}
		}
		if(aSentenceAndMentions.size()>1)
		{
			if (docID.contains("testb")) {
				lstSentencesAndMentions_testb.add(processSentence(docID,sentenceID, aSentenceAndMentions));
			} else if (!docID.contains("testa")) {
				lstSentencesAndMentions_train.add(processSentence(docID,sentenceID, aSentenceAndMentions));
			}
			sentenceID++;
			aSentenceAndMentions.clear();
		}
	}

	
	public static void parseDatasetSentences() throws NumberFormatException, IOException {
		String dataset = "/home/rtue/Documents/TestDatasets/AIDA-YAGO2-dataset.tsv";
		int numDocs = 0;
		List<String> list = new ArrayList<>();
		try (BufferedReader br = Files.newBufferedReader(Paths.get(dataset))) {
			list = br.lines().collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
		}
		Map<String, String> train = new HashMap<>();
		Map<String, String> testA = new HashMap<>();
		Map<String, String> testB = new HashMap<>();

		StringBuilder strbuild = new StringBuilder();
		List<String> result_testa = new ArrayList<>();
		List<String> result_testb = new ArrayList<>();

		for (String str : list) {
			if (str.contains("-DOCSTART-")) {

				String docID = str.split(" ")[1].replace(")", "").replace("(", "").trim();
				docID = str.split("-DOCSTART-")[1].trim();
				if (docID.contains("947testa CRICKET")) {
					strbuild.setLength(0);
				}
				if (docID.contains("testa")) {
					if (strbuild.length() > 1) {

						// System.out.println(strbuild.toString());
						result_testa.add(strbuild.toString());
					}
					// System.out.println(docID);
					result_testa.add(docID);
				} else if (docID.contains("testb")) {
					if (docID.contains("1163testb ")) {
						result_testa.add(strbuild.toString());
						System.out.println("testb " + docID);
						result_testb.add(docID);
					} else {
						System.out.println(strbuild.toString());
						result_testb.add(strbuild.toString());
						System.out.println(docID);
						result_testb.add(docID);
					}

				}
				strbuild.setLength(0);
			} else {
				strbuild.append(str.split("\t")[0] + " ");
			}
		}
		result_testb.add(strbuild.toString());
	}

	public Map<String, List<ConllData>> getMap_train() {
		return map_train;
	}
	public Map<String, List<ConllData>> getMap_testb() {
		return map_testb;
	}
	public List<ConllData> getLstSentencesAndMentions_testb() {
		return lstSentencesAndMentions_testb;
	}
	public List<ConllData> getLstSentencesAndMentions_train() {
		return lstSentencesAndMentions_train;
	}
}
