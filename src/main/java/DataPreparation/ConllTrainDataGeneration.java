package DataPreparation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import BenchmarkPreparation.ConllDataSetParser;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import info.debatty.java.stringsimilarity.Levenshtein;
import model.HtmlLink;
import util.Cache;
import util.Config;
import util.ConllData;
import util.FileUtil;
import util.HTMLLinkExtractor;
import util.Tuple;
import util.URLUTF8Encoder;
import util.ConllData;

public class ConllTrainDataGeneration {


	private static final boolean TRAIN_POSITIVE=Config.getBoolean("TRAIN_POSITIVE", false);
	private static final String RESULT_FILE_CONLL;
	private static final String LABEL;
	private static String TRAIN_MENTIONS;
	private static final String TRAIN_SENTENCES= Config.getString("TRAIN_SENTENCES_CONLL","");
	
	static{
		if(TRAIN_POSITIVE){
			RESULT_FILE_CONLL = "TrainSetConll.ttl";
			LABEL = "1";
			
		}else{
			RESULT_FILE_CONLL = "NegativeTrainSetConll.ttl";
			LABEL = "0";
		}
	}

	private static final int SIZE_OF_WORD_VECTOR = 100;
	private static final String DBR = "dbr:";		
	private static final Set<String> trainSet = new CopyOnWriteArraySet<>();
	private static final Logger LOG = Logger.getLogger(ConllTrainDataGeneration.class);
	private static AtomicInteger ALL_FEATURES_ARE_ZERO_ERROR = new AtomicInteger(0);
	private static AtomicInteger NO_ANCHOR_ERROR = new AtomicInteger(0);

	public static void main(String[] args) throws IOException {
		
		ConllDataSetParser conll = new ConllDataSetParser();
		
		//docID, SentenceID,sentence
//		Map<String, Map<Integer, String>> mapConllTrainSentences = new HashMap<>(conll.getMapSentence_train());
//		//docID SentenceID, Tuple<mention,wikilink>
//		Map<String, List<ConllData>> mapConllTrainMentions = new HashMap<>(conll.getMapMention_train());
//		Map<String, Map<Integer, String>> mapConllTestbSentences = new HashMap<>(conll.getMapSentence_testb());
//		Map<String, List<ConllData>> mapConllTestbMentions = new HashMap<>(conll.getMapMention_testb());

		
		Map<String, List<ConllData>> mapConllTrainSentences = new HashMap<>(conll.getMap_train());
		//docID SentenceID, Tuple<mention,wikilink>
		Map<String, List<ConllData>> mapConllTrainMentions = new HashMap<>();
		Map<String, Map<Integer, String>> mapConllTestbSentences = new HashMap<>();
		Map<String, List<ConllData>> mapConllTestbMentions = new HashMap<>();

		
		//showSizeOfResultPeridically();
	//	Map<String, String> trainSentencs= new HashMap<>(readSentencesConll(TRAIN_SENTENCES));
		if(TRAIN_POSITIVE){
			System.out.println("Positive Dataset generation");
			
			//iteratingOverDocs --> DocId, Connll
			//collect sentence and its mentions so send the sentence and its mentions
			for (Entry<String, List<ConllData>> entry:mapConllTrainMentions.entrySet()){
				System.out.println();
				String docID = entry.getKey();
				List<ConllData> lstOfMentionsInDoc = new ArrayList<>(entry.getValue());
				//String[] sentencesInDoc = new String[mapConllTrainSentences.get(docID).size()];
				Map<Integer, List<Tuple>> mapSentencesAndMentions= new HashMap<>();
				
				for (ConllData conllMention: lstOfMentionsInDoc)
				{
					Map<Integer, String> sentenceIDSentence ;
					Integer sentenceID = conllMention.getSentenceId();
					String sentence = "";
					System.out.println(sentence);
					if (mapSentencesAndMentions.containsKey(sentenceID)) {
						List<Tuple> lstTemp = new ArrayList<>(mapSentencesAndMentions.get(sentenceID));
						//lstTemp.add(conllMention.getMentionAndURI());
						mapSentencesAndMentions.put(sentenceID, lstTemp);
					} 
					else {
						List<Tuple> lstTemp = new ArrayList<>();
						//lstTemp.add(conllMention.getMentionAndURI());
						mapSentencesAndMentions.put(sentenceID, lstTemp);
					}
					
				}
//				for (Entry<Integer, List<Tuple>> entry:mapSentencesAndMentions.entrySet()) {
//					processThisLine(, final List<Tuple> mentionsIntheSentence)
//				}
				
				
			}
			
		}else{
			System.out.println("Negatives Dataset generation");//NegativeTrainConll.replaceWithWrongEntities();	
			//generateTrainingData(trainSentencs,NegativeTrainConll.replaceWithWrongEntities());
			
		}
		System.err.println("Start writing to file....");
		FileUtil.writeDataToFile(new ArrayList<>(trainSet), RESULT_FILE_CONLL, false);
		System.err.println("ALL_FEATURES_ARE_ZERO_ERROR= "+ALL_FEATURES_ARE_ZERO_ERROR);
//		System.err.println("NO_ANCHOR_ERROR= "+NO_ANCHOR_ERROR);
	}

	private static void generateTrainingData(Map<String, String> trainSentencs,
			Map<String, List<Tuple>> trainMentions) {
		for(Entry<String,String> entry: trainSentencs.entrySet())
		{
			String docId = entry.getKey();
			String sentence = entry.getValue();
			if (trainMentions.containsKey(docId))
			{
				handleThisLine(sentence,trainMentions.get(docId));
			}
		}

	}

	public static  Map<String, String> readSentencesConll(String trainSentences) {
		Map<String, String> trainSentencs = new HashMap<>();

		try (BufferedReader br = new BufferedReader(new FileReader(trainSentences)))
		{
			String line="";
			Integer sentenceNumber=1;
			while ((line = br.readLine()) != null) 
			{
				//System.out.println(line);
				trainSentencs.put(line, br.readLine());
				sentenceNumber++;
			}
		}
		catch (IOException e) {

			//System.err.println("Error at line== "+line);
			e.printStackTrace();
		}
		return trainSentencs;
	}
	
	public static Map<String, List<Tuple>> readMentionsConll(String mentionsConll) {
		Map<String, List<Tuple>> trainMentions = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(mentionsConll)))
		{
			String line="";
			Integer sentenceNumber=1;
			Tuple tp;
			while ((line = br.readLine()) != null) 
			{
				String[] splitLine=line.split("\t\t");
				String docID= splitLine[0];
				String[] splitSentence = splitLine[1].split("\t");
				String mention = splitSentence[0];
				String link=splitSentence[1].replace("http://en.wikipedia.org/wiki/", "").toLowerCase();
				//System.out.println(line);
				if (trainMentions.containsKey(docID)) {
					List<Tuple> lst = new ArrayList<>(trainMentions.get(docID));
					tp= new Tuple(mention, link);
					lst.add(tp);
					trainMentions.put(docID, lst);
				}
				else
				{
					List<Tuple> lst = new ArrayList<>();
					tp= new Tuple(mention, link);
					lst.add(tp);
					trainMentions.put(docID, lst);
				}
				sentenceNumber++;
			}
		}
		catch (IOException e) {

			//System.err.println("Error at line== "+line);
			e.printStackTrace();
		}
		System.out.println("We have "+trainMentions.size()+" number of sentences which are annotated with wikipedia");
		return trainMentions;
	}

	private static void handleThisLine(String line, List<Tuple> listMentions) {

		StringBuilder oneRow = processThisLine(line, listMentions);
		if(oneRow!=null){
			trainSet.add(oneRow.toString());
			LOG.info(oneRow.toString());
		}
	}

	/**
	 * This is exactly same as handleThisLine, but as we want to use it in other class
	 * we created it
	 * @param line
	 * @param mentionsIntheSentence
	 * @return
	 */
	public static StringBuilder processThisLine(String line, final List<Tuple> mentionsIntheSentence) {
		
		double[] sentenceVector = generateSentenceVector(prepareForSentenceVectorGeneration(line));
		StringBuilder oneSentence = new StringBuilder();
		int count =0;

		for(final Tuple mention:mentionsIntheSentence)
		{
			/**
			 * Word2Vec data set is generated based on following modification on the surface form and link
			 * From sentence only punctions are removed
			 * 
			 * 	final String surfaceForm = util.StringUtil.convertUmlaut(htmlLink.getAnchorText()).replaceAll("[^\\w\\s]", " ").replaceAll("[\\d]", "").toLowerCase();
				String finalSurfaceAndUrl = surfaceForm+" dbr:" + enUrl.toLowerCase();
			 * 
			 * 
			 * 
			 */
			//long start = System.currentTimeMillis();
			double f1 = generateFeatureFromSentenceAndEntities(prepareLink(mention.getB_link()),sentenceVector);
			//System.out.println(" f1 "+prepareLink(mention.getDecodedLink())+" "+sentenceVector);
			//System.err.println(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()-start));
			//start = System.currentTimeMillis();
			double f2 = generateFeatureEntityCoherency(mentionsIntheSentence,prepareLink(mention.getB_link()));
			//System.out.println(" f2 "+prepareLink(mention.getDecodedLink())+" "+sentenceVector);
			double f3 = generateFeatureFromMentionsAndEntities(prepareSurfaceform(mention.getA_mention()),prepareLink(mention.getB_link()));
			//System.out.println(" f3 "+prepareSurfaceform(mention.getMention())+" "+prepareLink(mention.getDecodedLink()));
			
			//System.err.println(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()-start));
			//TODO: adding f4 = coherence between entities
			//double f4 = 

			if(f1==0 && f2 ==0 && f3 ==0)
			{
				ALL_FEATURES_ARE_ZERO_ERROR.incrementAndGet();
				//System.out.println("All features are zero "+ mention);
			}
			else
			{
				oneSentence.append(line).append("\t\t").append(f1).append("\t").append(f2).append("\t").append(f3).append("\t\t").append(LABEL+"\n");
				if (oneSentence.toString().contains("http://10.10.4.10:4567")) 
				{
					System.out.println(oneSentence.toString());
				}
				count++;
			}
		}
		if (oneSentence.length()>0) {

//			System.out.println(oneSentence);
//			System.out.println("sentence Count "+count);
			count=0;
			return oneSentence;
		}
		else
			return null;
	}
	public static String prepareForSentenceVectorGeneration(String sentence)
	{
		String cleanSentence = null;
		cleanSentence = sentence.toLowerCase();
		cleanSentence=util.StringUtil.removePuntionation(cleanSentence);
		String afterTrim = cleanSentence.replaceAll("\\s+"," ").trim();
		//sentenceVector = generateSentenceVector(StopWordRemoval.removeStopWords( afterTrim));
		return afterTrim;
		
	}
	public static String prepareSurfaceform(String str)
	{
		return str.replaceAll("[^\\w\\s]", " ").replaceAll("[\\d]", "").toLowerCase().trim();
		
	}
	public static String prepareLink(String str)
	{
		return "dbr:" + str.toLowerCase().trim();
		
	}
	private static String removePunctioationNoreggex(String x) {
	    return x.replaceAll("[\\Q][(){},.;!?=&+$§^°/\"<>%\\E]" , "");
	}
	
	private static void showSizeOfResultPeridically() {
		final Thread t = new Thread(() -> {
			while(true){
				//System.err.println(trainSet.size() + "\t"+SimilarityCache.getSize()+"\t"+Cache.getSize());
				System.err.println("Number of generated trainset= "+trainSet.size() + "\t requestCache Size= "+Cache.getSize());
				try {
					Thread.sleep(TimeUnit.SECONDS.toMillis(5));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
	}

	public  static double generateFeatureEntityCoherency(List<Tuple> links,String URL) {
		double localSimilairity = 0;
		
		int localSize= 0;
		for(final Tuple link:links) {
			final String decodedUrl = prepareLink(link.getB_link());
			//System.out.println(decodedUrl+" "+URL);
			if (!decodedUrl.equals(URL)) 
			{
				Double sim = getSimilairty(URL,decodedUrl);
				if(sim!=null && !Double.isNaN(sim)){
					localSimilairity+= sim;
					localSize++;
				}
			}
		}
		//System.out.println(localSimilairity+" "+localSize+" "+localSimResult);
		final double localSimResult = localSimilairity/localSize;
		if(!Double.isNaN(localSimResult))
		{
			
			return localSimResult;
		}
		return 0;
	}

	private static Double getSimilairty(String word, String decodedUrl) {
		final double[] v1 = getWordVectorAfterLowerCasing(word);
		final double[] v2 = getWordVectorAfterLowerCasing(decodedUrl);

		if(v1!=null && v2!=null){
			final double sim = cosineSimilairity(v1, v2);
			if(!Double.isNaN(sim)){
				return new Double(sim);
			}else{
				return null;
			}
		}else{
			return null;
		}
	}

	public static double generateFeatureFromMentionsAndEntities(String mention, String entity) {
		double similarities = 0;
		int size = 0;
		final String decodedUrl = entity;
		final double[] mentionVector = generateSentenceVector(mention);
		final double[] urlVector = getWordVectorAfterLowerCasing(decodedUrl);
		if(urlVector!=null && mentionVector!=null){
			similarities=cosineSimilairity(urlVector,mentionVector);
			size++;
		}
		if(size==0){
			return 0;
		}
		return similarities;
	}
	public static double calculateStringDistance(String str1,String str2)
	{
		return Levenshtein.Distance(str1, str2);
	}

	public static double generateFeatureFromSentenceAndEntities(String url, double[] sentenceVector) {
		double similarities = 0;
		final String decodedUrl = url;
		final double[] urlVector = getWordVectorAfterLowerCasing(decodedUrl);
		if(urlVector!=null && sentenceVector!=null){
			similarities=cosineSimilairity(urlVector,sentenceVector);
		}
		return similarities;
	}

	private static double cosineSimilairity(double[] urlVector, double[] sentenceVector) {

		//		final Double sim = SimilarityCache.getWordVector(word1, word2);
		//		if(sim!=null){
		//			return sim.doubleValue();
		//		}

		float dotProduct = 0;
		float normA = 0;
		float normB = 0;
		for (int i = 0; i < urlVector.length; i++) {
			dotProduct += urlVector[i] * sentenceVector[i];
			normA += urlVector[i] * urlVector[i];
			normB += sentenceVector[i] * sentenceVector[i];
		}
		//if (dotProduct == 0) {
		//    return Float.NaN;
		//}
		final double similarity = (float)(dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
		//        SimilarityCache.add(word1,word2,similarity);
		return similarity;
	}

	public static double[] generateSentenceVector(String cleanSentence) {
		final List<String> tokens = tokenize(cleanSentence);
		double[] averageVector = new double[SIZE_OF_WORD_VECTOR];
		/**
		 * Number of vectors that we add to averageVector
		 * Used for taking average
		 */
		int size= 0 ;
		for(String w:tokens){
			double[] wordVector = getWordVectorAfterLowerCasing(w);
			if(wordVector!=null){
				addToAverageVector(averageVector,wordVector);
				size++;
			}	
		}
		if(size==0){
			return null;
		}
		return calculateAverageVector(averageVector,size);
	}

	private static double[] getWordVectorAfterLowerCasing(String w) {
		return Cache.getWordVector(w.toLowerCase());
	}

	private static double[] calculateAverageVector(double[] averageVector, int size) {
		for(int i=0;i<averageVector.length;i++){
			averageVector[i]/=size;
		}
		return averageVector;
	}

	private static void addToAverageVector(double[] averageVector, double[] wordVector) {
		for(int i=0;i<averageVector.length;i++){
			averageVector[i]+=wordVector[i];
		}
	}

	public static List<String> tokenize(String fullSentence){
		final TokenizerFactory<Word> tf = PTBTokenizer.factory();
		final List<String> tokens_words = tf.getTokenizer(new StringReader(fullSentence)).tokenize().stream().map(p->p.value()).collect(Collectors.toList());
		return tokens_words;
	}

}
