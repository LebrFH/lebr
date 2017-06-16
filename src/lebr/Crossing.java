package lebr;

import nav.NavData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Crossing {

    private static final Map<Integer, Crossing> cache = new HashMap<>();

    boolean closed = false;

    private final int id;
    private final NavData navData;
    private double kostenVonStart = Integer.MAX_VALUE;

    public static Crossing newCrossing(int id, NavData navData){
        final Crossing crossing = cache.get(id);
        if(crossing != null){
            return crossing;
        }
        final Crossing newCrossing = new Crossing(id, navData);
        cache.put(id, newCrossing);
        return newCrossing;
    }

    public Crossing(final NavData navData, final int latitude, final int longitude) {
        this.navData = navData;
        this.id = navData.getNearestCrossing(latitude, longitude);
        cache.put(this.id, this);
    }

    private Crossing(final int id, final NavData navData) {
        this.id = id;
        this.navData = navData;
    }

    public List<Link> getLinks() {
        final List<Link> links = new ArrayList<>();
        final int[] linksForCrossing = navData.getLinksForCrossing(id);
        for (final int linkId : linksForCrossing) {
            links.add(Link.newLink(linkId, navData));
        }
        return links;
    }

    public int getLatitude() {
        return navData.getCrossingLatE6(id);
    }

    public int getLongitude() {
        return navData.getCrossingLongE6(id);
    }

    //TODO Einbahnstraßen?
    public List<Crossing> getNachbarn() {
        final List<Crossing> nachbarn = new ArrayList<>();

        final int[] linkIds = navData.getLinksForCrossing(id);
        for (int linkId : linkIds) {
            final Link link = Link.newLink(linkId, navData);
            if (!link.getFrom().equals(this)) {
                nachbarn.add(link.getFrom());
            } else if (!link.getTo().equals(this)) {
                nachbarn.add(link.getTo());
            }
        }
        return nachbarn;
    }

    public double getKostenVonStart() {
        return kostenVonStart;
    }

    public void setKostenVonStart(final double kostenVonStart) {
        this.kostenVonStart = kostenVonStart;
    }

    public double getKostenZuNachbar(final Crossing nachbar) {
        final int[] linksForCrossing = navData.getLinksForCrossing(id);
        for (int linkId : linksForCrossing) {
            final Link link = Link.newLink(linkId, navData);
            if (link.getFrom().equals(nachbar) || link.getTo().equals(nachbar)) {
                return link.getKosten();
            }
        }
        return 0; //TODO
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Crossing crossing = (Crossing) o;
        return id == crossing.id;
    }
}
