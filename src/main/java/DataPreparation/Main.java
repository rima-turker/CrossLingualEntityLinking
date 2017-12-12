package DataPreparation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import CompareGTAndDic.CompareGTWithYovisto;

public class Main {

	public static void main(String[] args) {
//		GenerateDatasetForWordToVec data = new GenerateDatasetForWordToVec();
//		data.generate_dataSet_EN_enEntity_enWordSimityWordSim();
		
//		EDBanchmark_DataExtraction d = new EDBanchmark_DataExtraction();
//		d.getContextData();
//		d.getAnchorsGT();
		
//		CompareGTWithYovisto comp = new CompareGTWithYovisto();
//		comp.compareGTWithYovisto();
		
		//temp_neg
		String line="";
		try (BufferedReader br = new BufferedReader(new FileReader("temp_neg")))
		{
			while ((line = br.readLine()) != null) 
			{
				String[] split = line.split("\t\t");
//				for (int i = 0; i < split.length; i++) {
//					System.out.println(split[i]);
//				}
				System.out.println(split[1]+"\t"+split[2]);
			}
		}
		catch (IOException e) {
			System.err.println("Error at line== "+line);
			e.printStackTrace();
		}
	
	}

}
