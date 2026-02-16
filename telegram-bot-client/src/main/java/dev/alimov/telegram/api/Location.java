package dev.alimov.telegram.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Location(
        Double longitude,
        Double latitude,
        Double horizontalAccuracy,
        Integer livePeriod,
        Integer heading,
        Integer proximityAlertRadius
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Double longitude;
        private Double latitude;
        private Double horizontalAccuracy;
        private Integer livePeriod;
        private Integer heading;
        private Integer proximityAlertRadius;

        public Builder longitude(Double longitude) { this.longitude = longitude; return this; }
        public Builder latitude(Double latitude) { this.latitude = latitude; return this; }
        public Builder horizontalAccuracy(Double horizontalAccuracy) { this.horizontalAccuracy = horizontalAccuracy; return this; }
        public Builder livePeriod(Integer livePeriod) { this.livePeriod = livePeriod; return this; }
        public Builder heading(Integer heading) { this.heading = heading; return this; }
        public Builder proximityAlertRadius(Integer proximityAlertRadius) { this.proximityAlertRadius = proximityAlertRadius; return this; }

        public Location build() { return new Location(longitude, latitude, horizontalAccuracy, livePeriod, heading, proximityAlertRadius); }
    }
}
