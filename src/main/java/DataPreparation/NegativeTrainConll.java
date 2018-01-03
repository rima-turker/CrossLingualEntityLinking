package DataPreparation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import AnchorDictionaryGenerator.DictioanryGenerator;
import model.HtmlLink;
import util.Config;
import util.FileUtil;
import util.HTMLLinkExtractor;
import util.Tuple;
import util.URLUTF8Encoder;

public class NegativeTrainConll 
{
	private static final Logger LOG = Logger.getLogger(NegativeTrainConll.class);
	
	public static Map<String, List<Tuple>> replaceWithWrongEntities()
	{
		Map<String, String> trainSentencs= new HashMap<>(ConllTrainDataGeneration.readSentencesConll(Config.getString("TRAIN_SENTENCES_CONLL","")));
		Map<String, List<Tuple>> trainMentions = new HashMap<>(ConllTrainDataGeneration.readMentionsConll(Config.getString("TRAIN_SENTENCES_CONLL_MENTIONS", "")));
		Map<String, List<Tuple>> wrongTrainMentions = new HashMap<>();
		System.err.println("Dictioanry loading ... ");
		DictioanryGenerator dictioanryGenerator = new DictioanryGenerator();
		final Map<String, Map<String, Double>> dic = dictioanryGenerator.readDictionryFromFile();
		System.err.println("Dictioanry loaded");

		int totalMentions=0;
		int noCandidate=0;
		int candiateFound=0;
		for (Entry <String, String> ent:trainSentencs.entrySet()) 
		{
			String docID = ent.getKey();
			if (trainMentions.containsKey(docID)) 
			{
				List<Tuple> lst = new ArrayList<>(trainMentions.get(ent.getKey()));
				List<Tuple> lstWrong = new ArrayList<>();
				int countOnlyOneCandidate =0;
				for(Tuple conll : lst)
				{
					String decodedTrueUrl=conll.getB_link();
					final Map<String, Double> urlCandidates = dic.get(conll.getA_mention());
					totalMentions++;
					if (urlCandidates!=null) 
					{
						candiateFound++;
						final List<String> allCandicates = new ArrayList<>(urlCandidates.keySet());
						allCandicates.removeIf(p-> URLUTF8Encoder.decodeJavaNative(p).equalsIgnoreCase(decodedTrueUrl));
						if(allCandicates.size()>=1)
						{
							final Random rn = new Random();
							int randomNum = rn.nextInt(allCandicates.size());
							String candidate=allCandicates.get(randomNum);
							Tuple conWrong = new Tuple(conll.getA_mention(), candidate);
							lstWrong.add(conWrong);
						}
						else
						{
							countOnlyOneCandidate++;
						}
					}
					else if(dic.get(conll.getA_mention()) != null)
					{
						final Map<String, Double> urlCandidatesFromMention = dic.get(conll.getA_mention());
						candiateFound++;
						final List<String> allCandicates = new ArrayList<>(urlCandidatesFromMention.keySet());
						if(allCandicates.size()>1)
						{
							allCandicates.removeIf(p-> URLUTF8Encoder.decodeJavaNative(p).equalsIgnoreCase(decodedTrueUrl));
							final Random rn = new Random();
							final int randomNum = rn.nextInt(allCandicates.size());
							String candidate=allCandicates.get(randomNum);
							
							Tuple conWrong = new Tuple(conll.getA_mention(), candidate);
							lstWrong.add(conWrong);
						}
						else
						{
							countOnlyOneCandidate++;
							System.out.println();
						}
					}
					else
					{
						//System.out.println("No candidate found "+ conll.getMentionForcandidateGeneration());
						noCandidate++;
					}
				}
				//System.out.println("originalSize " + lst.size()+" new WrongSize "+lstWrong.size());
				if ((countOnlyOneCandidate+lstWrong.size())!=lst.size()) 
				{
					//System.err.println("HATAAAAA");
				}
				wrongTrainMentions.put(docID, lstWrong);
			}

		}
		System.out.println("Original mapSize "+trainMentions.size()+" Wrong mapSize "+wrongTrainMentions.size());
		System.out.println("Total mentions "+totalMentions+" noCAndidateFound "+noCandidate+" CAndidateFound "+ candiateFound);
		
		return wrongTrainMentions;
	}
	
	
}
