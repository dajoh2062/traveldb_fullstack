package projects.traveldbbackend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Country {
    private long sourceId;
    private String countryId;
    private String countryNameEn;
    private String continent;
    private String wikipediaUrl;
    private String keywords;
    private boolean schengen;
}
