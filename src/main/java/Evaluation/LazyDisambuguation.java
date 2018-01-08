package Evaluation;

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

	ConllDataSetParser parser = new ConllDataSetParser();
	private List<ConllData> lstnoCandidateGenerated = new ArrayList<>(findEntitiesNoCandidate(parser.getLstSentencesAndMentions_testb()));
	private Map<String, String> map_surnameNameandSurname=new HashMap<>();
	
	public String disambiguateWithOneCandidate(String mention, String trueURI) {
		disambiguateSurname(lstnoCandidateGenerated);// now you have the map
		Map<String, Double> candidates = CompareConllWithDictionary.generateCandidates(mention);
		if (candidates == null) {
			if (map_surnameNameandSurname.containsKey(mention)) {
				String mentionWithSurname = map_surnameNameandSurname.get(mention);
				candidates = CompareConllWithDictionary.generateCandidates(mentionWithSurname);
			}
		}
		if (candidates != null) {
			if (candidates.size() == 1) {
				Map.Entry<String, Double> entry = candidates.entrySet().iterator().next();
				String candidateURI = entry.getKey();
				List<String> redirects = CompareConllWithDictionary.getRedirectPages(candidateURI);
				String umlautRemovedCandidateURI = StringUtil.removeUmlaut(candidateURI);
				String umlautRemovedTrueURI = StringUtil.removeUmlaut(trueURI);
				if (umlautRemovedCandidateURI.equals(umlautRemovedTrueURI)) {
					return "true";
				} else if (StringUtil.levenshteinDistance(umlautRemovedCandidateURI, umlautRemovedTrueURI) == 2) {
					return "true";
					// System.out.println(candidateURI+"\t"+trueURI);
				} else if (redirects != null) {
					for (String strRedirect : redirects) {
						if (StringUtil.removeUmlaut(strRedirect).equals(umlautRemovedTrueURI)) {
							return "true";
						}
					}
					return "false";

				} else {
					return "false";
				}
			}
			else
			{
				return "moreCandidates";
			}
		}
		return null; // no candidate
	}
	public void disambiguateEasyMentions(final Map<String, List<ConllData>> map_testb,
			final List<ConllData> lstSentencesAndMentions_testb) {
		map_surnameNameandSurname = new HashMap<>();// a mention as a surname
													// but it has a name in the
													// beggining of the doc
		lstnoCandidateGenerated = new ArrayList<>(findEntitiesNoCandidate(lstSentencesAndMentions_testb));
		disambiguateSurname(lstnoCandidateGenerated);
		disambiguateOnlyOneCandidate(lstSentencesAndMentions_testb);
	}

	private List<ConllData> findEntitiesNoCandidate(List<ConllData> lstSentencesAndMentions_testb) {
		List<ConllData> entities = new ArrayList<>();
		int noCandidate = 0;
		CompareConllWithDictionary candidateGenerator = new CompareConllWithDictionary();
		for (ConllData conll : lstSentencesAndMentions_testb) {
			List<Tuple> lstMentionsAndURIs = new ArrayList<>(conll.getMentionAndURI());
			for (Tuple tp : lstMentionsAndURIs) {
				String mention = tp.getA_mention();
				Map<String, Double> candidates = candidateGenerator.generateCandidates(mention);
				if (candidates == null) {
					ConllData conllNoCand = new ConllData();
					conllNoCand.setMentionAndURI(Arrays.asList((tp)));
					conllNoCand.setDocId(conll.getDocId());
					conllNoCand.setSentence(conll.getSentence());
					conllNoCand.setSentenceId(conll.getSentenceId());
					// lstnoCandidateGenerated.add(conllNoCand);
					entities.add(conllNoCand);
					noCandidate++;
				}
			}
		}
		System.out.println("Candidate could not generated " + entities.size());
		System.out.println("Candidate could not generated " + noCandidate);
		return entities;

	}

	/***
	 * This function gets the list of mentions which no candidates are generated
	 * those mentions might be a surname and name and surname is in the begginin
	 * g of the document mentioned. Tries to match it.
	 * 
	 * map_noCandidateSurname is stores the surnames and their corresponding
	 * name and the surname in the document
	 *
	 * @param lstnoCandidateGenerated
	 */
	private void disambiguateSurname(List<ConllData> lstnoCandidateGenerated) {

		ConllDataSetParser conllParser = new ConllDataSetParser();
		Map<String, List<ConllData>> map_testb = new HashMap<>(conllParser.getMap_testb());
		// iterate over all the mentions that you could not found a candidate
		for (ConllData conll : lstnoCandidateGenerated) {
			List<Tuple> lstMentionsAndURIs = new ArrayList<>(conll.getMentionAndURI());
			Tuple tp = (Tuple) CollectionUtils.get(lstMentionsAndURIs, 0);
			String mentionNoCandidate = tp.getA_mention();
			String trueURI = tp.getB_link();
			String docID = conll.getDocId();
			// You get all the mentions in the corresponding document and then
			// try to match the surname with the name and surname
			List<ConllData> lstMentionsInDoc = new ArrayList<>(map_testb.get(docID));
			for (ConllData conllMentionInTheDoc : lstMentionsInDoc) {
				List<Tuple> lstTpMentions = conllMentionInTheDoc.getMentionAndURI();
				for (Tuple tup : lstTpMentions) {
					String mentionInDoc = tup.getA_mention();
					if (mentionInDoc.contains(mentionNoCandidate) && !mentionInDoc.equals(mentionNoCandidate)) {

						//System.err.println(mentionInDoc + " " + mentionNoCandidate);
						map_surnameNameandSurname.put(mentionNoCandidate, mentionInDoc);
					}
				}
			}
		}

	}

	private void disambiguateOnlyOneCandidate(List<ConllData> lstSentencesAndMentions_testb) {
		System.out.println("One candiate generated but disambuguated wrongly");
		System.out.println("true URI\tcandiateURI");
		int countOneCandidateDisambiguatedCorrectly = 0;
		int countOneCandidateDisambiguatedWrong = 0;
		int countOnlyOneCandidate = 0;
		int noCandidate = 0;
		for (ConllData conll : lstSentencesAndMentions_testb) {
			List<Tuple> lstMentionsAndURIs = new ArrayList<>(conll.getMentionAndURI());
			for (Tuple tp : lstMentionsAndURIs) {
				String mention = tp.getA_mention();
				String trueURI = tp.getB_link();
				Map<String, Double> candidates = CompareConllWithDictionary.generateCandidates(mention);
				if (candidates == null) {
					if (map_surnameNameandSurname.containsKey(mention)) {
						String mentionWithSurname = map_surnameNameandSurname.get(mention);
						candidates = CompareConllWithDictionary.generateCandidates(mentionWithSurname);
					}
				}
				if (candidates != null) {
					if (candidates.size() == 1) {
						countOnlyOneCandidate++;
						Map.Entry<String, Double> entry = candidates.entrySet().iterator().next();
						String candidateURI = entry.getKey();
						List<String> redirects = CompareConllWithDictionary.getRedirectPages(candidateURI);
						String umlautRemovedCandidateURI = StringUtil.removeUmlaut(candidateURI);
						String umlautRemovedTrueURI = StringUtil.removeUmlaut(trueURI);
						if (umlautRemovedCandidateURI.equals(umlautRemovedTrueURI)) {
							countOneCandidateDisambiguatedCorrectly++;
						} else if (StringUtil.levenshteinDistance(umlautRemovedCandidateURI,
								umlautRemovedTrueURI) == 2) {
							countOneCandidateDisambiguatedCorrectly++;
							// System.out.println(candidateURI+"\t"+trueURI);
						} else if (redirects != null) {
							for (String strRedirect : redirects) {
								if (StringUtil.removeUmlaut(strRedirect).equals(umlautRemovedTrueURI)) {
									countOneCandidateDisambiguatedCorrectly++;
									break;
								}
							}
							countOneCandidateDisambiguatedWrong++;
							System.out.println(candidateURI + "\t" + trueURI);

						} else {
							countOneCandidateDisambiguatedWrong++;
							System.out.println(candidateURI + "\t" + trueURI);
						}
					}

				} else {
					noCandidate++;
				}
			}
		}
		System.out.println("countOnlyOneCandidate " + countOnlyOneCandidate);
		System.out.println("number of mentions had one candidate and its URI correctly disambiguated "
				+ countOneCandidateDisambiguatedCorrectly);
		System.out.println("number of mentions had one candidate and its URI wrongly disambiguated "
				+ countOneCandidateDisambiguatedWrong);
		System.out.println("No candidate generated " + noCandidate);
	}

	private void disambiguateOnlyOneCandidate_old(List<ConllData> lstSentencesAndMentions_testb) {
		System.out.println("One candiate generated but disambuguated wrongly");
		System.out.println("true URI\tcandiateURI");
		int countOneCandidateDisambiguatedCorrectly = 0;
		int countOneCandidateDisambiguatedWrong = 0;
		int countOnlyOneCandidate = 0;
		int noCandidate = 0;
		CompareConllWithDictionary candidateGenerator = new CompareConllWithDictionary();
		for (ConllData conll : lstSentencesAndMentions_testb) {
			List<Tuple> lstMentionsAndURIs = new ArrayList<>(conll.getMentionAndURI());
			for (Tuple tp : lstMentionsAndURIs) {
				String mention = tp.getA_mention();
				String trueURI = tp.getB_link();
				Map<String, Double> candidates = candidateGenerator.generateCandidates(mention);
				if (candidates != null) {
					if (candidates.size() == 1) {
						countOnlyOneCandidate++;
						Map.Entry<String, Double> entry = candidates.entrySet().iterator().next();
						String candidateURI = entry.getKey();
						List<String> redirects = candidateGenerator.getRedirectPages(candidateURI);
						String umlautRemovedCandidateURI = StringUtil.removeUmlaut(candidateURI);
						String umlautRemovedTrueURI = StringUtil.removeUmlaut(trueURI);
						if (umlautRemovedCandidateURI.equals(umlautRemovedTrueURI)) {
							countOneCandidateDisambiguatedCorrectly++;
						} else if (StringUtil.levenshteinDistance(umlautRemovedCandidateURI,
								umlautRemovedTrueURI) == 2) {
							countOneCandidateDisambiguatedCorrectly++;
							// System.out.println(candidateURI+"\t"+trueURI);
						} else if (redirects != null) {
							for (String strRedirect : redirects) {
								if (StringUtil.removeUmlaut(strRedirect).equals(umlautRemovedTrueURI)) {
									countOneCandidateDisambiguatedCorrectly++;
									break;
								}
							}
							countOneCandidateDisambiguatedWrong++;
							System.out.println(candidateURI + "\t" + trueURI);

						} else {
							countOneCandidateDisambiguatedWrong++;
							System.out.println(candidateURI + "\t" + trueURI);
						}
					}

				} else {
					if (map_surnameNameandSurname.containsKey(mention)) {
						String mentionWithSurname = map_surnameNameandSurname.get(mention);
					}
					noCandidate++;
				}
			}
		}
		System.out.println("countOnlyOneCandidate " + countOnlyOneCandidate);
		System.out.println("number of mentions had one candidate and its URI correctly disambiguated "
				+ countOneCandidateDisambiguatedCorrectly);
		System.out.println("number of mentions had one candidate and its URI wrongly disambiguated "
				+ countOneCandidateDisambiguatedWrong);
		System.out.println("No candidate generated " + noCandidate);
	}
}