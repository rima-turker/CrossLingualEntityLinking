package DataPreparation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.bytedeco.javacpp.presets.opencv_core.Str;

import AnchorDictionaryGenerator.DictioanryGenerator;
import BenchmarkPreparation.ConllDataSetParser;
import CompareGTAndDic.CompareConllWithDictionary;
import util.ConllData;
import util.StringUtil;
import util.Tuple;

public class LazyDisambuguation {
	private List<ConllData> lstnoCandidateGenerated = new ArrayList<>();
	
	public void disambiguateEasyMentions(final Map<String, List<ConllData>> map_testb,
			final List<ConllData> lstSentencesAndMentions_testb)
	{
		disambiguateOnlyOneCandidate(lstSentencesAndMentions_testb);
		disambiguateSurname(lstnoCandidateGenerated);
	}
	private void disambiguateSurname(List<ConllData> lstnoCandidateGenerated) {
		ConllDataSetParser conllParser = new ConllDataSetParser(); 
		Map<String, List<ConllData>> map_testb = new HashMap<>(conllParser.getMap_testb());
		
		for(ConllData conll: lstnoCandidateGenerated){
			List<Tuple> lstMentionsAndURIs = new ArrayList<>(conll.getMentionAndURI());
			Tuple tp = (Tuple) CollectionUtils.get(lstMentionsAndURIs, 0);
			String mentionNoCandidate = tp.getA_mention();
			String trueURI = tp.getB_link();
			String docID=conll.getDocId();
			List<Tuple> lstMentionsInDoc = new ArrayList<>(conll.getMentionAndURI());
			for(Tuple tpMentionInTheDoc:lstMentionsInDoc)
			{
				String mentionInDoc = tpMentionInTheDoc.getA_mention();
				if (mentionInDoc.contains(mentionNoCandidate)&&!mentionInDoc.equals(mentionNoCandidate)) {
					System.out.println(mentionInDoc+" "+mentionNoCandidate);
				}
			}
		}
		
	}
	private void disambiguateOnlyOneCandidate(List<ConllData> lstSentencesAndMentions_testb){
		System.out.println("One candiate generated but disambuguated wrongly");
		System.out.println("true URI\tcandiateURI");
		int countOneCandidateDisambiguatedCorrectly=0;
		int countOneCandidateDisambiguatedWrong=0;
		int countOnlyOneCandidate=0;
		int noCandidate=0;
		CompareConllWithDictionary candidateGenerator = new CompareConllWithDictionary();
		for(ConllData conll: lstSentencesAndMentions_testb){
			List<Tuple> lstMentionsAndURIs = new ArrayList<>(conll.getMentionAndURI());
			for(Tuple tp: lstMentionsAndURIs){
				String mention = tp.getA_mention();
				String trueURI = tp.getB_link();
				Map<String, Double> candidates = candidateGenerator.generateCandidates(mention);
				if (candidates!=null) {
					if (candidates.size()==1) {
						countOnlyOneCandidate++;
						Map.Entry<String,Double> entry = candidates.entrySet().iterator().next();
						String candidateURI = entry.getKey();
						List<String> redirects = candidateGenerator.getRedirectPages(candidateURI);
						String umlautRemovedCandidateURI = StringUtil.removeUmlaut(candidateURI);
						String umlautRemovedTrueURI = StringUtil.removeUmlaut(trueURI);
						if (umlautRemovedCandidateURI.equals(umlautRemovedTrueURI)) {
							countOneCandidateDisambiguatedCorrectly++;
						}
						else if (StringUtil.levenshteinDistance(umlautRemovedCandidateURI,umlautRemovedTrueURI)==2) {
							countOneCandidateDisambiguatedCorrectly++;
							//System.out.println(candidateURI+"\t"+trueURI);
						}
						else if(redirects!=null )
						{
							for(String strRedirect : redirects)
							{
								if (StringUtil.removeUmlaut(strRedirect).equals(umlautRemovedTrueURI)) {
									countOneCandidateDisambiguatedCorrectly++;
									break;
								}
							}
							countOneCandidateDisambiguatedWrong++;
							System.out.println(candidateURI+"\t"+trueURI);
							
						}
						else
						{
							countOneCandidateDisambiguatedWrong++;
							System.out.println(candidateURI+"\t"+trueURI);
						}
					}
				}
				else
				{
					ConllData conllNoCand = new ConllData();
					conllNoCand.setMentionAndURI(Arrays.asList((tp)));
					conllNoCand.setDocId(conll.getDocId());
					conllNoCand.setSentence(conll.getSentence());
					conllNoCand.setSentenceId(conll.getSentenceId());
					lstnoCandidateGenerated.add(conllNoCand);
					noCandidate++;
				}
			}
		}
		System.out.println("countOnlyOneCandidate "+countOnlyOneCandidate);
		System.out.println("number of mentions had one candidate and its URI correctly disambiguated "+ countOneCandidateDisambiguatedCorrectly);
		System.out.println("number of mentions had one candidate and its URI wrongly disambiguated "+ countOneCandidateDisambiguatedWrong);
		System.out.println("No candidate generated "+noCandidate);
	}
}