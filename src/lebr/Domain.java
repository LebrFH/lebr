package lebr;

import com.vividsolutions.jts.geom.Geometry;

public class Domain {

    private final String name;
    private Geometry geometry;

    public Domain(final String name, final Geometry geometry) {
        this.name = name;
        this.geometry = geometry;
    }

    public Domain(final String name, int latitude, int longitude) {
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

    private int latitude;
    private int longitude;

    public int getLatitude() {
        return latitude;
    }

    public int getLongitude() {
        return longitude;
    }

    public void setLatitude(int latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(int longitude) {
        this.longitude = longitude;
    }

}
