package lebr;

import com.vividsolutions.jts.geom.Geometry;

public class Domain {

    private final String name;
    private Geometry geometry;

    public Domain(final String name, final Geometry geometry) {
        this.name = name;
        this.geometry = geometry;
    }

    public Domain(final String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    private double latitude;
    private double longitude;

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

}
