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
import CompareGTAndDic.CompareConllWithDictionary;
import model.HtmlLink;
import util.Config;
import util.ConllData;
import util.FileUtil;
import util.HTMLLinkExtractor;
import util.Tuple;
import util.URLUTF8Encoder;

public class NegativeTrainConll 
{
	private static final Logger LOG = Logger.getLogger(NegativeTrainConll.class);

	public static List<ConllData> replaceWithWrongEntities(List<ConllData> lstSentencesAndMentions_train)
	{
		List<ConllData> lstWrongTrainMentions = new ArrayList<>();
		System.err.println("Dictionary loading ... ");
		DictioanryGenerator dictioanryGenerator = new DictioanryGenerator();
		final Map<String, Map<String, Double>> dic = dictioanryGenerator.readDictionryFromFile();
		System.err.println("Dictioanry loaded");

		int totalMentions=0;
		int noCandidate=0;
		int candiateFound=0;
		int totalWrongMentions=0;
		int countOnlyOneCandidate =0;
		
		for (ConllData ent:lstSentencesAndMentions_train) 
		{
			ConllData conllDataWrongEntity = new ConllData();
			conllDataWrongEntity.setDocId(ent.getDocId());
			conllDataWrongEntity.setSentence(ent.getSentence());
			conllDataWrongEntity.setSentenceId(ent.getSentenceId());
			
			List<Tuple> lstTrueMentionsAndURIs = new ArrayList<>(ent.getMentionAndURI());
			List<Tuple> lstWrongMentionsAndURIs = new ArrayList<>();
			for(Tuple conll : lstTrueMentionsAndURIs)
			{
				String decodedTrueUrl=conll.getB_link();
				final Map<String, Double> urlCandidates = CompareConllWithDictionary.generateCandidates(conll.getA_mention());
				totalMentions++;
				if (urlCandidates!=null) 
				{
					candiateFound++;
					final List<String> allCandicates = new ArrayList<>(urlCandidates.keySet());
					allCandicates.removeIf(p-> URLUTF8Encoder.decodeJavaNative(p).equalsIgnoreCase(decodedTrueUrl));
					if(allCandicates.size()>=1){
						final Random rn = new Random();
						int randomNum = rn.nextInt(allCandicates.size());
						String candidate=allCandicates.get(randomNum);
						Tuple conWrong = new Tuple(conll.getA_mention(), candidate);
						lstWrongMentionsAndURIs.add(conWrong);
						totalWrongMentions++;
					}
					else{
						countOnlyOneCandidate++;
					}
				}
				else{//no candidate generated
					noCandidate++;
				}
			}
			if (lstWrongMentionsAndURIs.size()>0) {
				conllDataWrongEntity.setMentionAndURI(lstWrongMentionsAndURIs);
				//totalWrongMentions+=lstWrongMentionsAndURIs.size();
				lstWrongTrainMentions.add(conllDataWrongEntity);
			}
			else{
				
			}
		}
		if (noCandidate==countOnlyOneCandidate) {
			System.out.println("yes");
		}
		System.out.println("Original listSize "+lstSentencesAndMentions_train.size()+" Wrong listSize "+lstWrongTrainMentions.size());
		System.out.println("Total mentions "+totalMentions+" noCAndidateFound "+noCandidate+" CAndidateFound "+ candiateFound+" only one candidate "+countOnlyOneCandidate);
		System.out.println("totalWrongMentions "+totalWrongMentions);
		return lstWrongTrainMentions;
	}
}
