package DataPreparation;

public class Main {

	public static void main(String[] args) {
		
		EvaluationConll ev = new EvaluationConll();
		ev.main(null);
		
//		System.out.println(removePunctioationNoreggex("küuöbhzy,likmj+++ääärs\"\"§$&/§&(§&/()==&$$§§°°^^"));
//		GenerateDatasetForWordToVec data = new GenerateDatasetForWordToVec();
//		data.generate_dataSet_EN_enEntity_enWordSimityWordSim_SurfaceForm();
//		EDBanchmark_DataExtraction d = new EDBanchmark_DataExtraction();
//		d.getContextData();
//		d.getAnchorsGT();
		
//		CompareGTWithYovisto comp = new CompareGTWithYovisto();
//		comp.compareGTWithYovisto();
		
	}
	private static String removePunctioationNoreggex(String x) {
	    return x.replaceAll("[\\Q][(){},.;!?=&+$§^°/\"<>%\\E]" , "");
	}
}
