package lebr;

import nav.NavData;

import java.util.HashMap;
import java.util.Map;

public class Link {

    private static final Map<Integer, Link> cache = new HashMap<>();

    private final int id;

    public static Link getLink(int id) {
        final Link link = cache.get(id);
        if (link != null) {
            return link;
        }
        final Link newLink = new Link(id);
        cache.put(id, newLink);
        return newLink;
    }

    private Link(final int id) {
        this.id = id;
    }

    public Crossing getFrom() {
        final int crossingIDFrom = NavDataProvider.getNavData().getCrossingIDFrom(id);
        return Crossing.getCrossing(crossingIDFrom);
    }

    public Crossing getTo() {
        final int crossingIDTo = NavDataProvider.getNavData().getCrossingIDTo(id);
        return Crossing.getCrossing(crossingIDTo);
    }

    public double[][] getPoints() {
        final NavData navData = NavDataProvider.getNavData();
        final int domainID = navData.getDomainID(id);
        final double[] domainLongsE6 = toDoubleArray(navData.getDomainLongsE6(domainID));
        final double[] domainLatsE6 = toDoubleArray(navData.getDomainLatsE6(domainID));
        return new double[][]{domainLongsE6, domainLatsE6};
    }

    private double[] toDoubleArray(final int[] intArray) {
        final double[] doubleArray = new double[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            doubleArray[i] = intArray[i] / 1000000.0; //TODO di
        }
        return doubleArray;
    }

    public double getCost() {
        final int length = NavDataProvider.getNavData().getLengthMeters(id);
        double maxSpeed = getSpeed();
        if (maxSpeed == 0) {
            //TODO geschw anhand lsiclass
            maxSpeed = 100;
        }
        return (length / maxSpeed) * 1.33;
    }

    public double getSpeed() {
        return NavDataProvider.getNavData().getMaxSpeedKMperHours(id) / 3.6; // Meter pro Sekunde
    }

    public Link getReverseLink() {
        return getLink(NavDataProvider.getNavData().getReverseLink(id));
    }
}
