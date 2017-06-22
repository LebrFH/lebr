package lebr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Crossing implements Coordinate {

    private static final Map<Integer, Crossing> cache = new HashMap<>();

    private final int id;
    private boolean closed = false;
    private double costFromStart = Integer.MAX_VALUE;

    public static Crossing getCrossing(int id) {
        final Crossing crossing = cache.get(id);
        if (crossing != null) {
            return crossing;
        }
        final Crossing newCrossing = new Crossing(id);
        cache.put(id, newCrossing);
        return newCrossing;
    }

    public Crossing(final double latitude, final double longitude) {
        this.id = NavDataProvider.getNavData().getNearestCrossing((int)(latitude * 1000000), (int) (longitude* 1000000));
        cache.put(this.id, this);
    }

    private Crossing(final int id) {
        this.id = id;
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
            if (link.getFrom().equals(neighbour)) {
                return link.getReverseLink();
            }
        }
        throw new RuntimeException("Kein Link von Crossing " + id + " zu Crossing " + neighbour.id + " vorhanden!");
    }

    @Override
    public double getLatitude() {
        return NavDataProvider.getNavData().getCrossingLatE6(id) / 1000000.0;
    }

    @Override
    public double getLongitude() {
        return NavDataProvider.getNavData().getCrossingLongE6(id) / 1000000.0;
    }

    //TODO Einbahnstraßen?
    public List<Crossing> getNeighbours() {
        final List<Crossing> neighbours = new ArrayList<>();
        final int[] linkIds = NavDataProvider.getNavData().getLinksForCrossing(id);
        for (final int linkId : linkIds) {
            final Link link = Link.getLink(linkId);
            if (!link.getFrom().equals(this)) {
                neighbours.add(link.getFrom());
            } else if (!link.getTo().equals(this)) {
                neighbours.add(link.getTo());
            }
        }
        return neighbours;
    }

    public double getCostFromStart() {
        return costFromStart;
    }

    public void setCostFromStart(final double costFromStart) {
        this.costFromStart = costFromStart;
    }

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

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(final boolean closed) {
        this.closed = closed;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Crossing crossing = (Crossing) o;
        return id == crossing.id;
    }
}
