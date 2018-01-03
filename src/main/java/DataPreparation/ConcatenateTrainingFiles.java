package DataPreparation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import util.Config;
import util.FileUtil;

public class ConcatenateTrainingFiles 
{
	private static final String NEGATIVE_TRAINSET= Config.getString("NEGATIVE_TRAINSET", "");
	private static final String POSITIVE_TRAINSET= Config.getString("POSITIVE_TRAINSET", "");
	private static final List<String> allTrainSet = new ArrayList<>();
	
	public static void main(String[] args) { 
		String line="";
		try (BufferedReader br = new BufferedReader(new FileReader(NEGATIVE_TRAINSET)))
		{
			System.out.println(NEGATIVE_TRAINSET);
			while ((line = br.readLine()) != null) 
			{
				if (line.length()>1) {
					String[] split = line.split("\t\t");
					if (split.length>2) {

						allTrainSet.add(split[1]+"\t"+split[2]);
					}
					else
					{
						System.out.println("negative line is not processed properly "+line);
					}
				}

				
			}
		}
		catch (IOException e) {
			System.err.println("Error at line== "+line);
			e.printStackTrace();
		}
		
		try (BufferedReader br = new BufferedReader(new FileReader(POSITIVE_TRAINSET)))
		{
			System.out.println(POSITIVE_TRAINSET);
			while ((line = br.readLine()) != null) 
			{
				if (line.length()>1) {
					String[] split = line.split("\t\t");
					if (split.length>2) {

						allTrainSet.add(split[1]+"\t"+split[2]);
					}
					else
					{
						System.out.println("positive line is not processed properly "+line);
					}
				}
			}
		}
		catch (IOException e) {
			System.err.println("Error at line== "+line);
			e.printStackTrace();
		}
		System.out.println("writing to a file");
		Collections.shuffle(allTrainSet);
		FileUtil.writeDataToFile(allTrainSet, "TrainingSetFeatureAndLabelFile", false);
	}
}
