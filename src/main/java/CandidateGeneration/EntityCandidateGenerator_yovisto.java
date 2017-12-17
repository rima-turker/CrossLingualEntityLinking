package CandidateGeneration;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import util.Entity;
import util.NER_TAG;

public class EntityCandidateGenerator_yovisto {

	public static List<String> getCandidateList(String surfaceForm, String type) {
		// Map<String, String> mapResult = new HashMap<String, String>();
		List<String> linkList = new LinkedList<String>();
		if (type.equals("<LOCATION>")) {
			type = "<PLACE>";
		}
		Request_yovisto r = new Request_yovisto();
		r.setQuery(surfaceForm);
		r.setDataFormat(DataFormat.JSON);

		String result = Caller_yovisto.runYovisto(r);
		// String result = Caller.runDBpedia(r);

		ObjectMapper mapper = new ObjectMapper();

		try {
			JsonNode rootNode = mapper.readTree(result);

			int size = rootNode.get("entities").size();
			Entity[] array = new Entity[size];
			for (int i = 0; i < size; i++) {
				JsonNode jsonNode = rootNode.get("entities").get(i);
				Entity object = mapper.readValue(jsonNode.toString(), Entity.class);
				array[i] = object;

				if (array[i].iri.contains("http://dbpedia.org/resource/")) {
					if (type.contains((NER_TAG.ORGANIZATION.text))) {
						if (!array[i].categoryIri.toLowerCase().contains((NER_TAG.LOCATION.text.toLowerCase()))
								&& !array[i].categoryIri.toLowerCase().contains((NER_TAG.PERSON.text.toLowerCase()))) {
							linkList.add(array[i].iri.replaceAll("http://dbpedia.org/resource/", "").toLowerCase());
						}
					} else if (array[i].categoryIri.toLowerCase()
							.equals(type.replaceAll(">", "").replaceAll("<", "").toLowerCase())) {
						linkList.add(array[i].iri.replaceAll("http://dbpedia.org/resource/", "").toLowerCase());
					}
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return linkList;
	}
}
