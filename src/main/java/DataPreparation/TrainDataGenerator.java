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
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import info.debatty.java.stringsimilarity.Levenshtein;
import model.HtmlLink;
import util.Cache;
import util.Config;
import util.FileUtil;
import util.HTMLLinkExtractor;

public class TrainDataGenerator {

	private static final boolean TRAIN_POSITIVE=Config.getBoolean("TRAIN_POSITIVE", true);
	private static final String NAME_OF_THE_RESULT_FILE;
	private static final String LABEL;
	private static final String SUBSAMPLE_ALLANNOTATED_SENTENCES;
	private static final String FOLDER_SPLIT_SUBSET_WIKIANNOTATEDSENTENCES;
	
	static{
		if(TRAIN_POSITIVE){
			NAME_OF_THE_RESULT_FILE = "TrainSetWikipedia.ttl";
			LABEL = "1";
			SUBSAMPLE_ALLANNOTATED_SENTENCES = Config.getString("SUBSAMPLE_WIKIPEDIA_ALLANNOTATED_SENTENCES","");
			FOLDER_SPLIT_SUBSET_WIKIANNOTATEDSENTENCES = Config.getString("SUBSAMPLE_WIKIPEDIA_ALLANNOTATED_DATA_FOLDER","");
		}else{
			NAME_OF_THE_RESULT_FILE = "NegativeTrainSetWikipedia.ttl";
			LABEL = "0";
			SUBSAMPLE_ALLANNOTATED_SENTENCES = Config.getString("NEGATIVE_SUBSAMPLE_WIKIPEDIA_ALLANNOTATED_SENTENCES","");
			FOLDER_SPLIT_SUBSET_WIKIANNOTATEDSENTENCES = null;
		}
	}
	
	private static final int SIZE_OF_WORD_VECTOR = 100;
	private static final String DBR = "dbr:";		
	private static final Set<String> trainSet = new CopyOnWriteArraySet<>();
	private static final Logger LOG = Logger.getLogger(TrainDataGenerator.class);
	private static AtomicInteger ALL_FEATURES_ARE_ZERO_ERROR = new AtomicInteger(0);
	private static AtomicInteger NO_ANCHOR_ERROR = new AtomicInteger(0);
	
	private static ExecutorService executor;
	
	public static void main(String[] args) throws IOException {
		showSizeOfResultPeridically();
		if(TRAIN_POSITIVE){
			parseFileParallel();
		}else{
			parseFileSequential();	
		}
		System.err.println("Start writing to file....");
		FileUtil.writeDataToFile(new ArrayList<>(trainSet), NAME_OF_THE_RESULT_FILE, false);
		System.err.println("ALL_FEATURES_ARE_ZERO_ERROR= "+ALL_FEATURES_ARE_ZERO_ERROR);
		System.err.println("NO_ANCHOR_ERROR= "+NO_ANCHOR_ERROR);
	}

	private static void parseFileParallel() {
		int NUMBER_OF_THREADS = 55;
		try {
			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			final File[] listOfFiles = new File(FOLDER_SPLIT_SUBSET_WIKIANNOTATEDSENTENCES).listFiles();
			Arrays.sort(listOfFiles);
			for (int i = 0; i < listOfFiles.length; i++) {
				final String file = listOfFiles[i].getName();
				executor.execute(handle(FOLDER_SPLIT_SUBSET_WIKIANNOTATEDSENTENCES + File.separator + file));
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static Runnable handle(String pathTofile) {
		return () -> {
			try {
				final List<String> lines = Files.readAllLines(Paths.get(pathTofile), StandardCharsets.UTF_8);
				final HTMLLinkExtractor htmlLinkExtractor = new HTMLLinkExtractor();
				for(String line: lines) {
					handleThisLine(line, htmlLinkExtractor);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		};
	}

	private static void parseFileSequential() {
		final HTMLLinkExtractor htmlLinkExtractor = new HTMLLinkExtractor();
		String line=null;
		try (BufferedReader br = new BufferedReader(new FileReader(SUBSAMPLE_ALLANNOTATED_SENTENCES)))
		{
			while ((line = br.readLine()) != null) 
			{
				handleThisLine(line,htmlLinkExtractor);
			}
		}
		catch (IOException e) {
			System.err.println("Error at line== "+line);
			e.printStackTrace();
		}
	}

	private static void handleThisLine(String line, HTMLLinkExtractor htmlLinkExtractor) {
		final Vector<HtmlLink> links = htmlLinkExtractor.grabHTMLLinks(line);
		StringBuilder oneRow = processThisLine(line, links);
		if(oneRow!=null){
			trainSet.add(oneRow.toString());
			LOG.info(oneRow.toString());
		}
	}

	/**
	 * This is exactly same as handleThisLine, but as we want to use it in other class
	 * we created it
	 * @param line
	 * @param links
	 * @return
	 */
	public static StringBuilder processThisLine(String line, final Vector<HtmlLink> links) {
		String cleanSentence = null;
		double[] sentenceVector = null;

		if(links.isEmpty()){
			//throw new IllegalArgumentException("Sentence does not contain any anchor text. IT IS WRONG. It should have at leaset one anchor text");
			NO_ANCHOR_ERROR.incrementAndGet();
			return null;
		}
		boolean isFirstTime =true;
		for(final HtmlLink link:links) {
			if(isFirstTime){
				cleanSentence = link.getFullSentence().toLowerCase();
				cleanSentence=util.StringUtil.removePuntionation(cleanSentence);
				sentenceVector = generateSentenceVector(cleanSentence);
				isFirstTime=false;
			}
			link.setUrl(DBR+link.getDecodedUrl().toLowerCase());
		}
		//long start = System.currentTimeMillis();
		double f1 = generateFeatureFromSentenceAndEntities(links,sentenceVector,cleanSentence);
		//System.err.println(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()-start));
		//start = System.currentTimeMillis();
		double f2 = generateFeatureFromWordsAndEntities(links, cleanSentence);
		//System.err.println(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()-start));
		//start = System.currentTimeMillis();
		double f3 = generateFeatureFromMentionsAndEntities(links);
		//System.err.println(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()-start));
		//TODO: adding f4 = coherence between entities
		//double f4 = 
		
		if(f1==0 && f2 ==0 && f3 ==0){
			ALL_FEATURES_ARE_ZERO_ERROR.incrementAndGet();
			return null;
		}
		StringBuilder oneRow = new StringBuilder();
		oneRow.append(line).append("\t\t").append(f1).append("\t").append(f2).append("\t").append(f3).append("\t\t").append(LABEL);
		return oneRow;
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

	public  static double generateFeatureFromWordsAndEntities(Vector<HtmlLink> links, String cleanSentence) {
		final List<String> tokenize = tokenize(cleanSentence);
		double similarity = 0;
		int size = 0;
		for(final HtmlLink link:links) {
			final String decodedUrl = link.getDecodedUrl();
			double localSimilairity = 0;
			int localSize= 0;
			for(String word:tokenize){
				Double sim = getSimilairty(word,decodedUrl);
				if(sim!=null && !Double.isNaN(sim)){
					localSimilairity+= sim;
					localSize++;
				}
			}
			final double localSimResult = localSimilairity/localSize;
			if(!Double.isNaN(localSimResult)){
				similarity+=localSimResult;
				size++;
			}
		}
		if(size==0){
			return 0;
		}
		return similarity/size;
	}

	private static Double getSimilairty(String word, String decodedUrl) {
		final double[] v1 = getWordVectorAfterLowerCasing(word);
		final double[] v2 = getWordVectorAfterLowerCasing(decodedUrl);
		
		if(v1!=null && v2!=null){
			final double sim = cosineSimilairity(v1, v2,word,decodedUrl);
			if(!Double.isNaN(sim)){
				return new Double(sim);
			}else{
				return null;
			}
		}else{
			return null;
		}
	}

	public static double generateFeatureFromMentionsAndEntities(Vector<HtmlLink> links) {
		double similarities = 0;
		int size = 0;
		for(final HtmlLink link:links) {
			final String decodedUrl = link.getDecodedUrl();
			final String mention = link.getAnchorText();
			final double[] mentionVector = generateSentenceVector(mention);
			final double[] urlVector = getWordVectorAfterLowerCasing(decodedUrl);
			if(urlVector!=null && mentionVector!=null){
				similarities+=cosineSimilairity(urlVector,mentionVector,decodedUrl,mention);
				size++;
			}
		}
		if(size==0){
			return 0;
		}
		return similarities/size;
	}
	public static double calculateStringDistance(String str1,String str2)
	{
		return Levenshtein.Distance(str1, str2);
	}

	public static double generateFeatureFromSentenceAndEntities(Vector<HtmlLink> links, double[] sentenceVector, String cleanSentence) {
		double similarities = 0;
		int size = 0;
		for(final HtmlLink link:links) {
			final String decodedUrl = link.getDecodedUrl();
			final double[] urlVector = getWordVectorAfterLowerCasing(decodedUrl);
			if(urlVector!=null && sentenceVector!=null){
				similarities+=cosineSimilairity(urlVector,sentenceVector,decodedUrl,cleanSentence);
				size++;
			}
		}
		if(size==0){
			return 0;
		}
		return similarities/size;
	}

	private static double cosineSimilairity(double[] urlVector, double[] sentenceVector, String word1, String word2) {
		
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
