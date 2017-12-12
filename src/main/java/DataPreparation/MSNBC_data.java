package DataPreparation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class MSNBC_data 
{
	private static final Logger LOG  = Logger.getLogger(MSNBC_data.class);
	final String DATA_FOLDER =  "/home/rtue/workspace/Word2VecJava/MSNBC/CleanDocuments/";

	public void compareData()
	{

		String file= "Existence_Candidates_noPadding";//"Existence_Candidates";
		//System.out.println("Old model without padding");
		String fileGT = "MSNBC_GroundTruth";
		try {
			BufferedReader br = new BufferedReader(new FileReader(DATA_FOLDER+file));
			String line;
			HashMap<String, LinkedHashSet<String>> mapWVCandidates= new HashMap<>(); 
			LinkedHashSet<String> cand = new LinkedHashSet<>();
			String key =br.readLine();
			while ((line = br.readLine()) != null) 
			{
				if (!line.contains("dbr:")) 
				{
					if (!cand.isEmpty()) 
					{
						mapWVCandidates.put(key,  cand);
//						System.out.println(key);
//						System.out.println(cand);
						cand = new LinkedHashSet<>();

					}
					key = line;
				}
				else if (line.contains("dbr:")) 
				{

					cand.add(line);
				}
			}
			mapWVCandidates.put(key,  cand);
			calculateTopN(mapWVCandidates);

		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public void calculateTopN(HashMap<String, LinkedHashSet<String>> mapWVCandidates)
	{
		String fileGT = "MSNBC_GroundTruth";
		LOG.info("size of the candidate map " + mapWVCandidates.size());
		try {
			BufferedReader br = new BufferedReader(new FileReader(DATA_FOLDER+fileGT));
			String line;
			HashSet<String> setNotExist = new HashSet<>();
			HashSet<String> setExist = new HashSet<>();
			Map< Integer, HashSet<String>> map = new HashMap<Integer, HashSet<String>>();
			HashSet<String> setCandidateNotFound = new HashSet<>();
			
			while ((line = br.readLine()) != null) 
			{
				String surface = line.split(" ")[0];
				String link = line.split(" ")[1];
				//System.out.println(surface);
				LOG.info(surface);
				int topN=1;
				if (mapWVCandidates.containsKey(surface)) 
				{
					setExist.add(surface) ;
					LinkedHashSet<String> listCandi = new LinkedHashSet<>(mapWVCandidates.get(surface));
					
					for (String candidate: listCandi) 
					{
						
						if (candidate.replace("dbr:", "").equals(link)) 
						{
							//System.out.println(" top: "+ topN);
							if (map.containsKey(topN)) 
							{
								HashSet<String> set = new HashSet<>(map.get(topN));
								set.add(surface);
								map.put(topN, set);
							}
							else
							{
								HashSet<String> set = new HashSet<>();
								set.add(surface);
								map.put(topN, set);
							}
							
							LOG.info("top: "+ topN++);
							break;
						}
						else
						{
							topN++;
						}
					}
					if (topN>19) 
					{
						setCandidateNotFound.add(surface);
						//System.err.println("Surface form is there but could not find matching candidate "+surface);
					}
				}
				
				else
				{
					//System.out.println("mapWVCandidates does not contain word " + surface);
					setNotExist.add(surface);
					//countNotInDataSet++;
				}

			}
			System.out.println("Number of surface forms does not exist in our model " + setNotExist.size());
			System.out.println("Number of surface forms exist in our model " + setExist.size());
			System.out.println("Total " + (setExist .size()+setNotExist.size()));
			int total =0;
			for(java.util.Map.Entry<Integer, HashSet<String>> entry : map.entrySet())
			{
				//System.out.println(entry.getKey()+"  "+entry.getValue());
				System.out.println(entry.getKey()+"  "+entry.getValue().size());
				total+=entry.getValue().size();
			}
			System.out.println("Candidate not found "+setCandidateNotFound.size());
		} catch (Exception e) {
			// TODO: handle exception
		}
	
	}
	public void cleanData_Key()
	{
		final long now = System.currentTimeMillis();

		int count=0;
		try {
			final File[] listOfFiles = new File(DATA_FOLDER).listFiles();
			Arrays.sort(listOfFiles);
			HashSet<String> testSet = new HashSet<>();
			for (int i = 0; i < listOfFiles.length; i++) {
				final String file = listOfFiles[i].getName();

				int lineCounter = 0;
				int sentenceCounter = 0;

				final BufferedReader br = new BufferedReader(new FileReader(DATA_FOLDER+file));
				String line;
				StringBuilder strbulilder = new StringBuilder();
				while ((line = br.readLine()) != null) 
				{


					/*<SurfaceForm>
				Ganges
				</SurfaceForm>
				<Offset>
				17
				</Offset>
				<Length>
				6
				</Length>
				<ChosenAnnotation>
				http://en.wikipedia.org/wiki/Ganges_River
				</ChosenAnnotation>*/

					if (line.equals("<SurfaceForm>")) 
					{
						line = br.readLine().toLowerCase();

						String finalKey= "";

						if (line.split(" ").length>1) 
						{

							String[] split = line.split(" ");
							for (int j = 0; j < split.length; j++) 
							{
								finalKey=finalKey.concat(split[j]+"_");
							}
							finalKey=finalKey.substring(0,finalKey.length()-1);

						}
						else
							finalKey=line;
						strbulilder.append(finalKey+" ");
						testSet.add(strbulilder.toString());
						System.out.println(strbulilder.toString());

					}
					else if (line.equals("<ChosenAnnotation>")) 
					{
						line = br.readLine().toLowerCase().replace("http://en.wikipedia.org/wiki/", "");
						strbulilder.append(line+" ");
						//testSet.add(strbulilder.toString());
						//						System.out.println(strbulilder.toString());
						//						LOG.info(strbulilder.toString());
						strbulilder = new StringBuilder();
						count++;
					}

				}

				System.out.println("Number of Entities unique "+ count);
				br.close();
			}
			for (String str : testSet) {
				LOG.info(str);
			}

		} catch (IOException e) {

			e.printStackTrace();

		}
		//System.out.println("sentenceCount "+ sentenceCounter+ " lineCount "+ lineCounter);
		System.err.println(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()-now));
	}
	public static void cleanData_Sentence()
	{
		String file="/home/rtue/workspace/Word2VecJava/MSNBC/RawTextsSimpleChars_utf8/Wor16447201.txt";
		LOG.info("Wor16447201.txt annotations");
		final long now = System.currentTimeMillis();
		int lineCounter = 0;
		int sentenceCounter = 0;
		try {
			final BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			int count=0;
			StringBuilder strbulilder = new StringBuilder();
			while ((line = br.readLine()) != null) 
			{


				/*<SurfaceForm>
				Ganges
				</SurfaceForm>
				<Offset>
				17
				</Offset>
				<Length>
				6
				</Length>
				<ChosenAnnotation>
				http://en.wikipedia.org/wiki/Ganges_River
				</ChosenAnnotation>*/

				if (line.equals("<SurfaceForm>")) 
				{
					line = br.readLine();
					strbulilder.append(line+" ");

				}
				else if (line.equals("<ChosenAnnotation>")) 
				{
					line = br.readLine();
					strbulilder.append(line+" ");
					LOG.info(strbulilder.toString());
					//System.out.println(strbulilder.toString());
					strbulilder = new StringBuilder();
					count++;
				}


			}
			System.out.println("Number of Entities "+ count);
			br.close();					
		} catch (IOException e) {

			e.printStackTrace();

		}
		//System.out.println("sentenceCount "+ sentenceCounter+ " lineCount "+ lineCounter);
		System.err.println(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()-now));
	}
}
