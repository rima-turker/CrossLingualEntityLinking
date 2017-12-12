package util;

public class NerTag {
	private final String word;
	private NER_TAG nerTag;
	private final int startPosition;
	private final int endPosition;

	public NerTag(String word, NER_TAG nerTag, int startPosiiton, int endPosiiton) {
		if(word == null){
			throw new IllegalArgumentException("Word can not be null");
		}
		if(nerTag== null){
		//	throw new IllegalArgumentException("Ner tag can not be null");
		}
		this.word = word;
		this.nerTag = nerTag;
		this.startPosition = startPosiiton;
		this.endPosition = endPosiiton;
	}

	public NER_TAG getNerTag() {
		return nerTag;
	}

	public void setNerTag(NER_TAG nerTag) {
		this.nerTag = nerTag;
	}

	public String getWord() {
		return word;
	}

	public int getStartPosition() {
		return startPosition;
	}

	public int getEndPosition() {
		return endPosition;
	}

	@Override
	public String toString() {
		return "NerTag [word=" + word + ", nerTag=" + nerTag + ", startPosition=" + startPosition + ", endPosition="
				+ endPosition + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + endPosition;
		result = prime * result + ((nerTag == null) ? 0 : nerTag.hashCode());
		result = prime * result + startPosition;
		result = prime * result + ((word == null) ? 0 : word.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NerTag other = (NerTag) obj;
		if (endPosition != other.endPosition)
			return false;
		if (nerTag != other.nerTag)
			return false;
		if (startPosition != other.startPosition)
			return false;
		if (word == null) {
			if (other.word != null)
				return false;
		} else if (!word.equals(other.word))
			return false;
		return true;
	}

}
