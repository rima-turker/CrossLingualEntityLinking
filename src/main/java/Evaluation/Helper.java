package Evaluation;

/**
 * This is just a helper class to keep these three elements together
 * @author rtue
 *
 */
public class Helper{
	String candidate;
	float label;
	float probability;
	
	public Helper(String candidate, float label,float probability){
		this.candidate = candidate;
		this.label = label;
		this.probability = probability;
	}

	@Override
	public String toString() {
		return "Helper [candidate=" + candidate + ", label=" + label + ", probability=" + probability + "]";
	}
}
