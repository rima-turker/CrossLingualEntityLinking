package BenchmarkPreparation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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

import util.Config;
import util.Tuple;

public class EDBanchmark_DataExtraction 
{
	//read all the context first

	private static final String GT_DATA_FILE_500= Config.getString("GT_DATA_FILE_500", "");
	
	final static String ContextStart = "<http://aksw.org/N3/RSS-500/";
	final static String ContextEnd = "#char=0,";
	final static String nifString = "nif:isString \""; 
	final static int NUMBER_OF_CONTEXT = 500;


	public Map <String, String> getContextData()
	{
		Map <String, String> result = new HashedMap<>();
		int countSentence =0;
		try (BufferedReader br = new BufferedReader(new FileReader(GT_DATA_FILE_500)))
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
									countSentence++;
									break;
								}

							}
						}
					}

					tempList.clear();
				}

			}
			System.out.println("Total sentence "+countSentence);
		}

		catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public void writeAnchorsToFile()
	{
		Map <String, List<Tuple>> mapAnchor = new HashMap<>(getAnchorsGT());

		try (Writer writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream("anchors_500"), "utf-8"))) {

			for (Entry<String, List<Tuple>> entry: mapAnchor.entrySet()) 
			{
				List<Tuple> lst = new ArrayList<>(entry.getValue());
				for (Tuple t: lst) {

					writer.write(t.getA_mention());
					writer.write("\n");	
				}
			}
		}

		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public Map <String, List<Tuple>> getAnchorsGT()
	{
		String anchor = "nif:anchorOf \"";
		String begIndex = "nif:beginIndex \"";
		String endIndex = "nif:endIndex \"";
		String anchorEnd = "\"^^xsd:string";
		Map <String, List<Tuple>> mapAnchors = new HashedMap<>();

		try (BufferedReader br = new BufferedReader(new FileReader(GT_DATA_FILE_500)))
		{
			String line;
			List<String> tempList = new ArrayList<>();
			int totalAnchor=0;
			int realAnchor=0;
			int countAnchor =0;
			int countNotLinkableAnchor =0;
			int linkableAnchor =0;
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
							countAnchor++;
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

										List<Tuple> lst = new ArrayList<>(mapAnchors.get(id));
										lst.add(new Tuple(anchorText, link));
										mapAnchors.put(id, lst);
									}
									else
									{
										List<Tuple> lst = new ArrayList<>();
										lst.add(new Tuple(anchorText, link));
										mapAnchors.put(id, lst);
									}

									ids.add(id);
									hsetIDs.add(id);

									realAnchor++;
								}
								if (tempList.get(i).contains("<http://aksw.org/notInWiki/")) 
								{
									countNotLinkableAnchor++;
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
			System.out.println("Total anchor "+totalAnchor+" linkable anchor "+realAnchor+" unique number of anchors "+mapAnchors.size());
			//System.out.println("total anchor "+countAnchor+", not linkable anchor "+countNotLinkableAnchor);
			count=0;
			for(Entry<String, List<Tuple>> ent : mapAnchors.entrySet())
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



