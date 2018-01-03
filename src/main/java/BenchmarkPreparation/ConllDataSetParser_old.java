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

public class ConllDataSetParser_old {

	private static final String CONLL_MAIN_FILE = Config.getString("CONLL_DATASET", "");
	private static final Logger LOG = Logger.getLogger(ConllDataSetParser_old.class);

	private Map<String, Map<Integer, String>> mapSentence_train = new HashMap<>();
	private Map<String, List<ConllData>> mapMention_train = new HashMap<>();

	private Map<String, Map<Integer, String>> mapSentence_testb = new HashMap<>();
	private Map<String, List<ConllData>> mapMention_testb = new HashMap<>();

	private List<ConllData> lstMentions_testb = new ArrayList<>();
	private List<ConllData> lstMentions_train = new ArrayList<>();

	private List<ConllData> lstSentencesAndMentions_testb = new ArrayList<>();
	private List<ConllData> lstSentencesAndMentions_train = new ArrayList<>();
	
	int  count=0;
	public ConllDataSetParser_old() {
		readDocbyDoc();

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
		
		
		
		//		System.out.println("train sentence and mention map size "+
		//				mapSentence_testb.size()+" "+mapMention_testb.size());
		//		System.out.println("train sentence and mention map size "+
		//				mapSentence_train.size()+" "+mapMention_train.size());
		//		System.out.println("train sentence and mention size "+
		//				mapSentence_train.size()+" "+lstMentions_train.size());
		//		System.out.println("testb sentence and mention size "+
		//				mapSentence_testb.size()+" "+lstMentions_testb.size());
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
					String entity = elem[3].replace("\\u0028","(").
							replace("\\u0029",")").replace("\\u0027","'").replace("\\u00fc","ü").replace("\\u002c",",").
							replace("\\u0163","ţ").replace("\\u00e1s","á").replace("\\u0159","ř").replace("\\u00e9","é").
							replace("\\u00ed","í").replace("\\u00e1","á").replace("\\u2013","-").replace("\\u0107","ć").
							replace("\\u002e",".").replace("\\u00f3","ó").replace("\\u002d","-").replace("\\u00e1","Ž").
							replace("\\u0160","Š").replace("\\u0105","ą").replace("\\u00eb","ë").replace("\\u017d","Ž").
							replace("\\u00e7","ç").replace("\\u00f8","ø").replace("\\u0161","š").replace("\\u0107","ć").
							replace("\\u00f6","ö").replace("\\u010c","Č").replace("\\u00fd","ý").replace("\\u00d6","Ö").
							replace("\\u00c0","À").replace("\\u0026","&").replace("\\u00df","ß").replace("\\u00ea","ê").
							replace("\\u017","ž").replace("\\u011b","ě").replace("\\u00f6","ö").replace("\\u00e3","ã").
							replace("\\u0103","ă").replace("\\u00c1","Á").replace("\\u002f","/").replace("\\u00e4","ä").
							replace("\\u00c5","Å").replace("\\u0142","ł").replace("\\u0117","ė").replace("\\u00ff","ÿ").
							replace("\\u00f1","ñ").replace("\\u015f","ş").replace("\\u015e","Ş").replace("\\u0131","ı").
							replace("\\u0131k","Ç").replace("\\u0144","ń").replace("\\u0119","ę").replace("\\u00c9","É").
							replace("\\u0111","đ").replace("\\u00e2","â").replace("\\u010d","č").replace("\\u015a","Ś").
							replace("\\u0141","Ł").replace("\\u00e8","è").replace("\\u00c9","É").replace("\\u00e5","å").
							replace("\\u014d","ō").replace("\\u00e6","æ").replace("\\u00d3","Ó").replace("\\u00da","Ú").
							replace("\\u0151","ő").replace("\\u0148","ň").replace("\\u00fa","ú").replace("\\u00ee","î").
							replace("\\u015b","ś").replace("\\u00c7","Ç").replace("\\u00f4","ô").replace("\\u013d","Ľ").
							replace("\\u013e","ľ").replace("\\u011f","ğ").replace("\\u00e0","à").replace("\\u00dc","Ü").
							replace("\\u0021","!").replace("_"," ");

					if (!entity.equalsIgnoreCase("--NME--")) {
						if (docID.contains("testb")) {

							count++;
						}
						String wikiLink = elem[4].trim();
						lstOfMetions.add(new Tuple(mention.trim(),wikiLink.replace("http://en.wikipedia.org/wiki/", "").trim()));

						//	System.out.println(new Tuple(mention.trim(),wikiLink.replace("http://en.wikipedia.org/wiki/", "").trim()));

					} 

				}
			}
		}
		String sentenceFinal = sentence.toString().replaceAll("\\s+", " ").trim();
		conll.setSentence(sentenceFinal);
		conll.setMentionAndURI(lstOfMetions);
		//System.out.println(conll);
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

	private void processDoc_old(String docID, List<String> doc) {

		Integer intSentenceID = 0;
		StringBuilder sentence = new StringBuilder();
		Tuple tp = new Tuple();
		List<String> aSentenceAndMentions = new ArrayList<>();
		// iterate over one doc line by line
		for (int i = 0; i < doc.size(); i++) {
			ConllData conll = new ConllData();
			String line = doc.get(i);
			Charset.forName("UTF-8").encode(line);
			aSentenceAndMentions.add(line);
			//			if (line.equals("") && doc.get(i - 1).equals(".")) //{ // new sentence
			//				sentenceCount_old++;
			//				String sentenceFinal = sentence.toString().replaceAll("\\s+", " ").trim();
			//				if (sentenceFinal.equals(null)) {
			//					System.out.println(sentenceFinal);
			//				}
			//				if (docID.contains("testb")) {
			//					if (mapSentence_testb.containsKey(docID)) {
			//						Map<Integer, String> tempSentenceIdSentence = new HashMap<>(mapSentence_testb.get(docID));
			//						tempSentenceIdSentence.put(intSentenceID, sentenceFinal);
			//						mapSentence_testb.put(docID, tempSentenceIdSentence);
			//					} else {
			//						Map<Integer, String> tempSentenceIdSentence = new HashMap<>();
			//						tempSentenceIdSentence.put(intSentenceID, sentenceFinal);
			//						mapSentence_testb.put(docID, tempSentenceIdSentence);
			//					}
			//
			//					// System.out.println(intSentenceID+"
			//					// "+sentence.toString().replaceAll("\\s+", " ").trim());
			//				} else if (!docID.contains("testa")) {
			//					if (mapSentence_train.containsKey(docID)) {
			//						Map<Integer, String> tempSentenceIdSentence = new HashMap<>(mapSentence_train.get(docID));
			//						tempSentenceIdSentence.put(intSentenceID, sentenceFinal);
			//						mapSentence_train.put(docID, tempSentenceIdSentence);
			//					} else {
			//						Map<Integer, String> tempSentenceIdSentence = new HashMap<>();
			//						tempSentenceIdSentence.put(intSentenceID, sentenceFinal);
			//						mapSentence_train.put(docID, tempSentenceIdSentence);
			//					}
			//
			//					// System.out.println(intSentenceID+"
			//					// "+sentence.toString().replaceAll("\\s+", " ").trim());
			//				}
			//
			//				intSentenceID++;
			//				sentence.setLength(0);
			//				// System.out.println("Yes");
			//			} else {
			//				sentence.append(line.split("\t")[0] + " ");
			//			}
			String[] elem = line.split("\t");
			if ((elem.length) >= 4) {
				String isCompleteMention = elem[1].trim();
				if (isCompleteMention.equals("B")) {
					String mention = elem[2].trim();
					String entity = elem[3].replace("\\u0028", "(").replace("\\u0029", ")").replace("\\u0027", "'")
							.replace("\\u00fc", "ü").replace("\\u002c", ",").replace("\\u0163", "ţ")
							.replace("\\u00e1s", "á").replace("\\u0159", "ř").replace("\\u00e9", "é")
							.replace("\\u00ed", "í").replace("\\u00e1", "á").replace("\\u2013", "-")
							.replace("\\u0107", "ć").replace("\\u002e", ".").replace("\\u00f3", "ó")
							.replace("\\u002d", "-").replace("\\u00e1", "Ž").replace("\\u0160", "Š")
							.replace("\\u0105", "ą").replace("\\u00eb", "ë").replace("\\u017d", "Ž")
							.replace("\\u00e7", "ç").replace("\\u00f8", "ø").replace("\\u0161", "š")
							.replace("\\u0107", "ć").replace("\\u00f6", "ö").replace("\\u010c", "Č")
							.replace("\\u00fd", "ý").replace("\\u00d6", "Ö").replace("\\u00c0", "À")
							.replace("\\u0026", "&").replace("\\u00df", "ß").replace("\\u00ea", "ê")
							.replace("\\u017", "ž").replace("\\u011b", "ě").replace("\\u00f6", "ö")
							.replace("\\u00e3", "ã").replace("\\u0103", "ă").replace("\\u00c1", "Á")
							.replace("\\u002f", "/").replace("\\u00e4", "ä").replace("\\u00c5", "Å")
							.replace("\\u0142", "ł").replace("\\u0117", "ė").replace("\\u00ff", "ÿ")
							.replace("\\u00f1", "ñ").replace("\\u015f", "ş").replace("\\u015e", "Ş")
							.replace("\\u0131", "ı").replace("\\u0131k", "Ç").replace("\\u0144", "ń")
							.replace("\\u0119", "ę").replace("\\u00c9", "É").replace("\\u0111", "đ")
							.replace("\\u00e2", "â").replace("\\u010d", "č").replace("\\u015a", "Ś")
							.replace("\\u0141", "Ł").replace("\\u00e8", "è").replace("\\u00c9", "É")
							.replace("\\u00e5", "å").replace("\\u014d", "ō").replace("\\u00e6", "æ")
							.replace("\\u00d3", "Ó").replace("\\u00da", "Ú").replace("\\u0151", "ő")
							.replace("\\u0148", "ň").replace("\\u00fa", "ú").replace("\\u00ee", "î")
							.replace("\\u015b", "ś").replace("\\u00c7", "Ç").replace("\\u00f4", "ô")
							.replace("\\u013d", "Ľ").replace("\\u013e", "ľ").replace("\\u011f", "ğ")
							.replace("\\u00e0", "à").replace("\\u00dc", "Ü").replace("\\u0021", "!").replace("_", " ");

					if (!entity.equalsIgnoreCase("--NME--")) {
						String wikiLink = elem[4].trim();

						// String entityId = elem[5].trim();
						// System.out.println(docID+"\t"+"\t"+intSentenceID+"\t"+"\t"+mention+"\t"+wikiLink);
						conll.setDocId(docID);
						conll.setSentenceId(intSentenceID);

						if (docID.contains("testb")) {

							if (mapMention_testb.containsKey(docID)) {
								List<ConllData> lstConll = new ArrayList<>(mapMention_testb.get(docID));
								lstConll.add(conll);
								mapMention_testb.put(docID, lstConll);
							} else {
								List<ConllData> lstConll = new ArrayList<>();
								lstConll.add(conll);
								mapMention_testb.put(docID, lstConll);
							}

							lstMentions_testb.add(conll);
						} else if (!docID.contains("testa")) {

							if (mapMention_train.containsKey(docID)) {
								List<ConllData> lstConll = new ArrayList<>(mapMention_train.get(docID));
								lstConll.add(conll);
								mapMention_train.put(docID, lstConll);
							} else {
								List<ConllData> lstConll = new ArrayList<>();
								lstConll.add(conll);
								mapMention_train.put(docID, lstConll);
							}

							lstMentions_train.add(conll);
						}
						// LOG.info(docID+"\t"+"\t"+sentenceID+"\t"+"\t"+mention+"\t"+wikiLink);
					}
				}
			}
		}
	}

	// FileUtil.writeDataToFile(lst, "AIDA-YAGO-mentions",false);

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

	public Map<String, Map<Integer, String>> getMapSentence_train() {
		return mapSentence_train;
	}

	public Map<String, List<ConllData>> getMapMention_train() {
		return mapMention_train;
	}

	public Map<String, Map<Integer, String>> getMapSentence_testb() {
		return mapSentence_testb;
	}

	public Map<String, List<ConllData>> getMapMention_testb() {
		return mapMention_testb;
	}

	public List<ConllData> getLstMentions_testb() {
		return lstMentions_testb;
	}

	public List<ConllData> getLstmentionsTrain() {
		return lstMentions_train;
	}

}
