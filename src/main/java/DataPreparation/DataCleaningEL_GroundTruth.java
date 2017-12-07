package DataPreparation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.map.HashedMap;

import java_cup.runtime.Symbol;
import util.Touple;

public class DataCleaningEL_GroundTruth 
{
	//read all the context first
	final static String DATA_FILE= "/home/rtue/workspace/CrossLingualEntityLinking/res/datasets/N3/RSS-500.ttl";

	final static String ContextStart = "<http://aksw.org/N3/RSS-500/";
	final static String ContextEnd = "#char=0,";
	final static String nifString = "nif:isString \""; 
	final static int NUMBER_OF_CONTEXT = 500;


	public Map <String, String> getContextData()
	{
		Map <String, String> result = new HashedMap<>();

		try (BufferedReader br = new BufferedReader(new FileReader(DATA_FILE)))
		{
			String line;
			List<String> tempList = new ArrayList<>();
			while ((line = br.readLine()) != null) 
			{
				tempList.add(line);
				if (line.equals("")) 
				{
					if (tempList.get(0).contains(ContextStart)&&tempList.get(0).contains("#char=")) 
					{
						String id = tempList.get(0).substring(ContextStart.length(), tempList.get(0).indexOf("#"));

						if (tempList.get(1).contains("nif:String")) 
						{
							for (int i = 2; i < tempList.size(); i++) 
							{
								//String id2="";
								if (tempList.get(i).contains("nif:isString")) 
								{
									//System.out.println(line);
									line = tempList.get(i).trim();
									line = 	line.substring((line.indexOf(nifString)+nifString.length()), line.indexOf("@en ."));
									result.put(id,line);
									//System.out.println(id+" "+line);
									break;
								}

							}
						}
					}

					tempList.clear();
				}

			}

		}

		catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public void writeAnchorsToFile()
	{
		Map <String, List<Touple>> mapAnchor = new HashMap<>(getAnchorsGT());

		try (Writer writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream("anchors_500"), "utf-8"))) {

			for (Entry<String, List<Touple>> entry: mapAnchor.entrySet()) 
			{
				List<Touple> lst = new ArrayList<>(entry.getValue());
				for (Touple t: lst) {

					writer.write(t.getA());
					writer.write("\n");	
				}
			}
		}

		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public Map <String, List<Touple>> getAnchorsGT()
	{
		String anchor = "nif:anchorOf \"";
		String begIndex = "nif:beginIndex \"";
		String endIndex = "nif:endIndex \"";
		String anchorEnd = "\"^^xsd:string";
		Map <String, List<Touple>> mapAnchors = new HashedMap<>();

		try (BufferedReader br = new BufferedReader(new FileReader(DATA_FILE)))
		{
			String line;
			List<String> tempList = new ArrayList<>();
			int totalAnchor=0;
			int realAnchor=0;

			List<String> ids = new ArrayList<>();
			HashSet<String> hsetIDs = new HashSet<>();
			while ((line = br.readLine()) != null) 
			{
				tempList.add(line);
				if (line.equals("")) 
				{
					if (tempList.get(0).contains(ContextStart)&&tempList.get(0).contains("#char=")) 
					{
						String id = tempList.get(0).substring(ContextStart.length(), tempList.get(0).indexOf("#"));

						if (tempList.get(2).contains("nif:anchorOf")) 
						{
							String anchorText = tempList.get(2).trim();
							anchorText = anchorText.substring(anchorText.indexOf(anchor)+anchor.length(), anchorText.indexOf(anchorEnd));
							//System.out.println(anchorText);
							totalAnchor++;
							for (int i = 3; i < tempList.size(); i++) 
							{
								String id2="";
								if (tempList.get(i).contains("<http://aksw.org/N3/RSS-500/")) 
								{
									String temp = tempList.get(i).trim();
									id2 = temp.substring(ContextStart.length(), tempList.get(0).indexOf("#"));
								}

								//	System.out.println(tempList.get(i));

								if (tempList.get(i).contains("itsrdf:taIdentRef <http://dbpedia.org/resource/")) 
								{
									String link =tempList.get(i).trim();
									link = link.substring(link.indexOf(("<http:")));

									if (mapAnchors.containsKey(id)) 
									{

										List<Touple> lst = new ArrayList<>(mapAnchors.get(id));
										lst.add(new Touple(anchorText, link));
										mapAnchors.put(id, lst);
									}
									else
									{
										List<Touple> lst = new ArrayList<>();
										lst.add(new Touple(anchorText, link));
										mapAnchors.put(id, lst);
									}

									ids.add(id);
									hsetIDs.add(id);

									realAnchor++;
								}
								if (tempList.get(i).contains("<http://aksw.org/notInWiki/")) 
								{

								}
								if (!id.equals(id2)&&!id2.equals("")) 
								{
									System.err.println("IDs are not matching");
								}


							}
						}
					}

					tempList.clear();
				}

			}
			int count=0;
			//		for (Integer i = 0; i < 500; i++)
			//		{
			//			if (!hsetIDs.contains(i)) {
			//				//System.out.println(i);
			//				System.out.println(mapAnchors.get(i));
			//				count++;
			//			}	
			//		}
			System.out.println("Total anchor "+totalAnchor+" lible anchor "+realAnchor+" size of the map"+mapAnchors.size());
			count=0;
			for(Entry<String, List<Touple>> ent : mapAnchors.entrySet())
			{
				count+= ent.getValue().size();
			}
			//mapAnchors.forEach((k,v)-> System.out.println(k+", "+v));
			System.out.println("Total linkible anchor size "+ count );
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return mapAnchors;
	}

	//<http://aksw.org/N3/Reuters-128/62#char=0,533>
	//					if (line.contains(ContextStart+key+ContextEnd)) 
	//					{
	//						line = br.readLine(); 
	//						if (!line.contains("nif:Context")) 
	//						{
	//							/*nif:anchorOf "Texas Department"^^xsd:string ;
	//						      nif:beginIndex "137"^^xsd:nonNegativeInteger ;
	//						      nif:endIndex "153"^^xsd:nonNegativeInteger ;*/
	//							//input.substring(0, input.indexOf('.'));
	//							line = br.readLine().trim();
	//							if (line.contains("nif:anchorOf")) {
	//								String anchorText = line.substring((line.indexOf(anchor)+anchor.length()), line.indexOf(str));
	//								//String 
	//								System.out.println(anchorText);
	//							}
	//
	//						}
	//					}






}



