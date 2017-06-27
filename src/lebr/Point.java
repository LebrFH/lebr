package lebr;

/**
 * Einfache Implementierung einer geographischen Koordinate
 */
public class Point implements Coordinate {

    private final double latitude;
    private final double longitude;

    public Point(final double latitude, final double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public double getLatitude() {
        return latitude;
    }

    @Override
    public double getLongitude() {
        return longitude;
    }

}
