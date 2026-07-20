package projects.traveldbbackend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Airport {

    private long sourceId;
    private String ident;
    private String iataCode;
    private String icaoCode;
    private String gpsCode;
    private String localCode;
    private String name;
    private String city;
    private String regionCode;
    private String country;
    private String countryCode;
    private String continent;
    private String airportType;
    private boolean scheduledService;
    private double latitude;
    private double longitude;
    private Integer elevationFt;
    private String officialUrl;
    private String wikipediaUrl;
    private String keywords;
    private boolean schengen;
}
