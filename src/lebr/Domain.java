package lebr;

public class Domain implements Coordinate {

    private final String name;
    private final double latitude;
    private final double longitude;

    public Domain(final String name, final double latitude, final double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
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
