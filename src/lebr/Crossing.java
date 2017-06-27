package lebr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Klasse fuer ein Crossing.
 */
public class Crossing implements Coordinate {

    /**
     * Crossing-Cache. <br>
     * Da wir nur eine ID haben, benoetigen wir die Referenzen auf bereits angelegte Crossings.
     */
    private static final Map<Integer, Crossing> cache = new HashMap<>();

    private final int id;
    private boolean closed = false;
    private double costFromStart = Double.MAX_VALUE;

    /**
     * Holt anhand einer ID ein vorhandenes Crossing aus dem Cache oder legt ein neues an, falls noch nicht gecached.
     *
     * @param id die Crossing-ID
     * @return das Crossing zur ID
     */
    public static Crossing getCrossing(int id) {
        final Crossing crossing = cache.get(id);
        if (crossing != null) {
            return crossing;
        }
        final Crossing newCrossing = new Crossing(id);
        cache.put(id, newCrossing);
        return newCrossing;
    }

    private Crossing(final int id) {
        this.id = id;
    }

    public Crossing(final double latitude, final double longitude) {
        this.id = NavDataProvider.getNavData().getNearestCrossing((int) (latitude * 1000000), (int) (longitude * 1000000));
        cache.put(this.id, this);
    }

    public List<Link> getLinks() {
        final List<Link> links = new ArrayList<>();
        final int[] linksForCrossing = NavDataProvider.getNavData().getLinksForCrossing(id);
        for (final int linkId : linksForCrossing) {
            links.add(Link.getLink(linkId));
        }
        return links;
    }

    public Link getLinkToNeighbour(final Crossing neighbour) {
        final List<Link> links = getLinks();
        for (final Link link : links) {
            if (link.getTo().equals(neighbour)) {
                return link;
            }
        }
        throw new RuntimeException("Kein Link von Crossing " + id + " zu Crossing " + neighbour.id + " vorhanden!");
    }

    /**
     * Ermittelt alle Nachbar-Crossings dieses Crossings, unter Beachtung von Einbahnstrassen.
     * @return eine Liste aller Nachbar-Crossings
     */
    public List<Crossing> getNeighbours() {
        final List<Crossing> neighbours = new ArrayList<>();
        final int[] linkIds = NavDataProvider.getNavData().getLinksForCrossing(id);
        for (final int linkId : linkIds) {
            final Link link = Link.getLink(linkId);
            // Nur wenn nicht entgegen einer Einbahnstrasse
            if (!link.goesCounterWay()) {
                neighbours.add(link.getTo());
            }
        }
        return neighbours;
    }

    /**
     * Ermittelt die Kosten von diesem Crossing zum uebergebenen Nachbar-Crossing.
     * @param neighbour das Nachbar-Crossing
     * @return die Kosten in Sekunden
     */
    public double getCostToNeighbour(final Crossing neighbour) {
        final int[] linksForCrossing = NavDataProvider.getNavData().getLinksForCrossing(id);
        for (final int linkId : linksForCrossing) {
            final Link link = Link.getLink(linkId);
            if (link.getFrom().equals(neighbour) || link.getTo().equals(neighbour)) {
                return link.getCost();
            }
        }
        throw new RuntimeException("Kein Link von Crossing " + id + " zu Crossing " + neighbour.id + " vorhanden!");
    }

    public static void clearCache() {
        cache.clear();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Crossing crossing = (Crossing) o;
        return id == crossing.id;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(final boolean closed) {
        this.closed = closed;
    }

    @Override
    public double getLatitude() {
        return NavDataProvider.getNavData().getCrossingLatE6(id) / 1000000.0;
    }

    @Override
    public double getLongitude() {
        return NavDataProvider.getNavData().getCrossingLongE6(id) / 1000000.0;
    }

    public double getCostFromStart() {
        return costFromStart;
    }

    public void setCostFromStart(final double costFromStart) {
        this.costFromStart = costFromStart;
    }
}
