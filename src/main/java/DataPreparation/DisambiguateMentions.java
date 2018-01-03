package DataPreparation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import AnchorDictionaryGenerator.DictioanryGenerator;
import BenchmarkPreparation.ConllDataSetParser;
import CompareGTAndDic.CompareConllWithDictionary;
import util.ConllData;
import util.StringUtil;
import util.Tuple;

public class DisambiguateMentions {
	public void disambiguateEasyMentions(final Map<String, List<ConllData>> map_testb,
			final List<ConllData> lstSentencesAndMentions_testb)
	{
		disambiguateOnlyOneCandidate(lstSentencesAndMentions_testb);
	}
	private void disambiguateOnlyOneCandidate(List<ConllData> lstSentencesAndMentions_testb){
		System.out.println("One candiate generated but disambuguated wrongly");
		System.out.println("true URI\tcandiateURI");
		int countOneCandidateDisambiguatedCorrectly=0;
		int countOneCandidateDisambiguatedWrong=0;
		int countOnlyOneCandidate=0;
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
						else if(redirects!=null )
						{
							for(String strRedirect : redirects)
							{
								if (StringUtil.removeUmlaut(strRedirect).equals(umlautRemovedTrueURI)) {
									countOneCandidateDisambiguatedCorrectly++;
									break;
								}
							}
//							System.out.println(candidateURI);
//							System.out.println(redirects);
							
						}
						else
						{
							countOneCandidateDisambiguatedWrong++;
							System.out.println("true URI "+candidateURI+" candidateURI "+trueURI);
						}
					}
				}
			}
		}
		System.out.println("countOnlyOneCandidate "+countOnlyOneCandidate);
		System.out.println("number of mentions had one candidate and its URI correctly disambiguated "+ countOneCandidateDisambiguatedCorrectly);
		System.out.println("number of mentions had one candidate and its URI wrongly disambiguated "+ countOneCandidateDisambiguatedWrong);
	}
	
	
}