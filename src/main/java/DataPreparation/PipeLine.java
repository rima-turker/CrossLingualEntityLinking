package DataPreparation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;

import AnchorDictionaryGenerator.DictioanryGenerator;
import BenchmarkPreparation.ConllDataSetParser_old;
import CompareGTAndDic.CompareConllWithDictionary;
import Evaluation.Evaluation;
import Evaluation.EvaluationConll;
import util.StringUtil;

public class PipeLine {

	public static CompareConllWithDictionary candidateGenerator = new CompareConllWithDictionary();
	private static final Logger LOG = Logger.getLogger(ConllDataSetParser_old.class);
	public static void main(String[] args) {
		//		ConllTrainDataGeneration trainData = new ConllTrainDataGeneration();
		//		trainData.startGeneratingTrainingSetConll();

		//		ConcatenateTrainingFiles c=new ConcatenateTrainingFiles();
		//		c.main(null);

				EvaluationConll ev = new EvaluationConll();
	//			ev.main(null);
		
	ev.evaluateFromResultFile("/home/rtue/workspace/CrossLingualEntityLinking/Results/DocBasedResultToBeEvaluated");
	}

}
