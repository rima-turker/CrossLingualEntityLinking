package util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Entity {
	
	    @JsonProperty("label")
	    public String label;

	    @JsonProperty("iri")
	    public String iri;
	    
	    @JsonProperty("categoryIri")
	    public String categoryIri;
	    
	    @JsonProperty("categoryLabel")
	    public String categoryLabel;
	    
	    @JsonProperty("originLabel")
	    public String originLabel;

		@Override
		public String toString() {
			return "Entity [label=" + label + ", iri=" + iri + ", categoryIri=" + categoryIri + ", categoryLabel="
					+ categoryLabel + ", originLabel=" + originLabel + "]";
		}
	}
