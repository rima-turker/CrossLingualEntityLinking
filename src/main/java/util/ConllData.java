package util;

import java.util.ArrayList;
import java.util.List;

import util.Tuple;

public class ConllData 
{
	private String docId;
	private int sentenceId;
	private List<Tuple> mentionAndURI;
	private String sentence;
	
	public String getSentence() {
		return sentence;
	}
	public void setSentence(String sentence) {
		this.sentence = sentence;
	}
	public String getDocId() {
		return docId;
	}
	public void setDocId(String docId) {
		this.docId = docId;
	}
	public int getSentenceId() {
		return sentenceId;
	}
	public void setSentenceId(int sentenceId) {
		this.sentenceId = sentenceId;
	}
	public List<Tuple> getMentionAndURI() {
		return mentionAndURI;
	}
	public void setMentionAndURI(List<Tuple> mentionAndURI) {
		this.mentionAndURI = new ArrayList<>(mentionAndURI);
	}
	
	@Override
	public String toString() {
		return "[docId=" + docId + ", sentence=" + sentence +"\n"+ 
				"mentionAndURI=" + mentionAndURI.toString() +"]";
	}
	
	
}
