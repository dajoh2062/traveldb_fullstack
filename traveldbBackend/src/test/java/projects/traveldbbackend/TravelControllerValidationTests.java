package projects.traveldbbackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import projects.traveldbbackend.service.JourneyRequestValidator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:journey-validation-tests")
@AutoConfigureMockMvc(addFilters = false)
class TravelControllerValidationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void acceptsAndNormalizesAValidMinimalJourney() throws Exception {
        mockMvc.perform(post("/api/journey/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalityCountryCode": " no ",
                                  "route": [" osl ", "lhr"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.documentCheck.provider").value("TRAVELDB_LOCAL_RULES"));
    }

    @Test
    void returnsAirportSearchResultsWithTheExistingResponseShape() throws Exception {
        mockMvc.perform(get("/api/airports/search")
                        .param("q", "jfk")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.airports[0].iataCode").value("JFK"))
                .andExpect(jsonPath("$.airports[0].scheduledService").isBoolean())
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.hasMore").isBoolean());
    }

    @Test
    void returnsCountriesWithTheExistingResponseShape() throws Exception {
        mockMvc.perform(get("/api/countries"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Vercel-CDN-Cache-Control", "public, max-age=3600"))
                .andExpect(jsonPath("$[?(@.countryId == 'NO')].countryNameEn").value("Norway"));
    }

    @Test
    void healthEndpointIsAvailable() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void rejectsANullJourneyWithAStableProblemResponse() throws Exception {
        mockMvc.perform(post("/api/journey/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:traveldb:error:invalid-journey-request"))
                .andExpect(jsonPath("$.code").value("INVALID_JOURNEY_REQUEST"))
                .andExpect(jsonPath("$.instance").value("/api/journey/check"))
                .andExpect(jsonPath("$.errors[0].field").value("request"));
    }

    @Test
    void rejectsMalformedJsonWithoutLeakingParserDetails() throws Exception {
        mockMvc.perform(post("/api/journey/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nationalityCountryCode":"NO","route":["OSL","LHR"]
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
                .andExpect(jsonPath("$.detail").value(
                        "Request body must contain valid JSON matching the journey request schema."
                ))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void reportsAllInvalidCountryAirportDateAndAgeFields() throws Exception {
        mockMvc.perform(post("/api/journey/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalityCountryCode": "NOR",
                                  "route": ["O", "ZZZ"],
                                  "documents": {
                                    "residenceCountryCode": "XX",
                                    "passportIssuingCountryCode": "NOR",
                                    "passportExpiryDate": "2020-01-01",
                                    "departureDate": "2020-02-01",
                                    "travelerAge": 121,
                                    "residencePermitCountryCodes": ["USA"],
                                    "visaCountryCodes": ["XX"]
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JOURNEY_REQUEST"))
                .andExpect(jsonPath("$.errors[?(@.field == 'nationalityCountryCode')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'route[0]')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'route[1]')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'documents.residenceCountryCode')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'documents.passportIssuingCountryCode')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'documents.passportExpiryDate')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'documents.departureDate')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'documents.travelerAge')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'documents.residencePermitCountryCodes[0]')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'documents.visaCountryCodes[0]')]").exists());
    }

    @Test
    void rejectsRoutesOutsideTheSupportedLength() throws Exception {
        mockMvc.perform(post("/api/journey/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nationalityCountryCode":"NO","route":["OSL"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'route')]").exists());

        String route = "\"OSL\",".repeat(JourneyRequestValidator.MAX_ROUTE_AIRPORTS)
                + "\"LHR\"";
        mockMvc.perform(post("/api/journey/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nationalityCountryCode\":\"NO\",\"route\":[" + route + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'route')]").exists());
    }

    @Test
    void rejectsInvalidDateFormatsAndPassportDatesBeforeTravel() throws Exception {
        mockMvc.perform(post("/api/journey/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalityCountryCode":"NO",
                                  "route":["OSL","LHR"],
                                  "documents":{"departureDate":"tomorrow"}
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));

        mockMvc.perform(post("/api/journey/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalityCountryCode":"NO",
                                  "route":["OSL","LHR"],
                                  "documents":{
                                    "departureDate":"2030-06-01",
                                    "passportExpiryDate":"2030-05-31"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("documents.passportExpiryDate"));
    }

    @Test
    void reportsNestedTravelDocumentValidationErrors() throws Exception {
        mockMvc.perform(post("/api/journey/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalityCountryCode":"NO",
                                  "route":["OSL","LHR"],
                                  "documents":{
                                    "departureDate":"2030-06-01",
                                    "travelDocuments":[
                                      {"type":"SPACE_PASS","primary":true},
                                      {"type":"OTHER","customType":" ","primary":true},
                                      {"type":"PASSPORT","expiryDate":"2020-01-01"}
                                    ]
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'documents.travelDocuments[0].type')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'documents.travelDocuments[1].primary')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'documents.travelDocuments[1].customType')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'documents.travelDocuments[2].issuingCountryCode')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'documents.travelDocuments[2].expiryDate')]").exists());
    }

    @Test
    void requiresOnePrimaryDocumentAndLimitsTheDocumentList() throws Exception {
        mockMvc.perform(post("/api/journey/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nationalityCountryCode":"NO",
                                  "route":["OSL","LHR"],
                                  "documents":{
                                    "travelDocuments":[
                                      {"type":"PASSPORT","issuingCountryCode":"NO"}
                                    ]
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'documents.travelDocuments')]").exists());

        String document = "{\"type\":\"PASSPORT\",\"issuingCountryCode\":\"NO\",\"primary\":true}";
        String tooManyDocuments = String.join(",", java.util.Collections.nCopies(
                JourneyRequestValidator.MAX_TRAVEL_DOCUMENTS + 1,
                document
        ));
        mockMvc.perform(post("/api/journey/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nationalityCountryCode\":\"NO\",\"route\":[\"OSL\",\"LHR\"],"
                                + "\"documents\":{\"travelDocuments\":[" + tooManyDocuments + "]}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'documents.travelDocuments')]").exists());
    }
}
