package io.github.dajoh2062.traveldb.api.dto;

public record BaggageOptions(
        Boolean checkedBaggage,
        String ticketArrangement,
        String checkedThrough
) {
    public static BaggageOptions defaults() {
        return new BaggageOptions(true, "SINGLE_BOOKING", "YES");
    }
}
