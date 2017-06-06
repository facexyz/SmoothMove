package xyz.facex.imitaterunning;

/**
 * Created by zhu on 17-6-1.
 */

public class LngLat {
    private double lng;
    private double lat;

    public LngLat(double lng, double lat) {
        this.lng = lng;
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public double getLat() {
        return lat;
    }

    @Override
    public String toString() {
        return "LngLat{" +
                "lng=" + lng +
                ", lat=" + lat +
                '}';
    }
}
