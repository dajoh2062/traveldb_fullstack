package projects.traveldbbackend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Airline {

    private String airlineId;
    private String airlineName;
    private String countryId;
}
