package projects.traveldbbackend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Airport {

    private String iataCode;
    private String name;
    private String country;
    private String countryCode;
    private boolean schengen;
}
