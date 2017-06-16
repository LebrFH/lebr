package lebr;

import nav.NavData;

import java.util.HashMap;
import java.util.Map;

public class Link {

    private static final Map<Integer, Link> cache = new HashMap<>();

    private final int id;
    private final NavData navData;

    public static Link newLink(int id, NavData navData) {
        final Link link = cache.get(id);
        if (link != null) {
            return link;
        }
        final Link newLink = new Link(id, navData);
        cache.put(id, newLink);
        return newLink;
    }

    private Link(final int id, final NavData navData) {
        this.id = id;
        this.navData = navData;
    }

    public Crossing getFrom() {
        final int crossingIDFrom = navData.getCrossingIDFrom(id);
        return Crossing.newCrossing(crossingIDFrom, navData);
    }

    public Crossing getTo() {
        final int crossingIDTo = navData.getCrossingIDTo(id);
        return Crossing.newCrossing(crossingIDTo, navData);
    }

    public double[][] getGeometriepunkte() {
        final int domainID = navData.getDomainID(id);
        final double[] domainLongsE6 = toDoubleArray(navData.getDomainLongsE6(domainID));
        final double[] domainLatsE6 = toDoubleArray(navData.getDomainLatsE6(domainID));
        return new double[][]{domainLongsE6, domainLatsE6};
    }

    private double[] toDoubleArray(final int[] intArray) {
        final double[] doubleArray = new double[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            doubleArray[i] = intArray[i] / 1000000.0;
        }
        return doubleArray;
    }

    public double getKosten() {
        final int laenge = navData.getLengthMeters(id);
        double maxGeschwindigkeit = getSpeed();
        if (maxGeschwindigkeit == 0) {
            //TODO geschw anhand lsiclass
            maxGeschwindigkeit = 100;
        }
        return (laenge / maxGeschwindigkeit) * 1.33;
    }

    public double getSpeed() {
        return navData.getMaxSpeedKMperHours(id) / 3.6; // Meter pro Sekunde
    }

    public Link getReverseLink() {
        return newLink(navData.getReverseLink(id), navData);
    }
}
