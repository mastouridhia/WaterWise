package com.waterwise.app.network.model;

import java.util.List;

/** JSON answer of the geocoding search: { "results": [ ... ] } */
public class GeocodingResponse {

    /** Null when nothing matches. */
    public List<Place> results;

    public static class Place {
        public String name;
        public String country;
        /** Region, e.g. "Tunis Governorate". */
        public String admin1;
        public double latitude;
        public double longitude;

        /** "Tunis, Tunis Governorate, Tunisia" */
        public String label() {
            StringBuilder sb = new StringBuilder(name);
            if (admin1 != null && !admin1.equals(name)) sb.append(", ").append(admin1);
            if (country != null) sb.append(", ").append(country);
            return sb.toString();
        }
    }
}
