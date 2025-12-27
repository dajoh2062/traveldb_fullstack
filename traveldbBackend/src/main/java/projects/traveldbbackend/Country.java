package projects.traveldbbackend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Country {
    private String countryId;
    private String countryNameEn;
    private boolean schengen;
}
