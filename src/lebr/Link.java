package lebr;

import nav.NavData;

import java.util.HashMap;
import java.util.Map;

public class Link {

    private static final Map<Integer, Link> cache = new HashMap<>();

    private final int id;
    private final NavData navData;

    public static Link newLink(int id, NavData navData){
        final Link link= cache.get(id);
        if(link != null){
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

    public Crossing getFrom(){
        final int crossingIDFrom = navData.getCrossingIDFrom(id);
        return Crossing.newCrossing(crossingIDFrom, navData);
    }

    public Crossing getTo(){
        final int crossingIDTo = navData.getCrossingIDTo(id);
        return Crossing.newCrossing(crossingIDTo, navData);
    }

    public double getKosten(){
        final int laenge = navData.getLengthMeters(id);
        double maxGeschwindigkeit = navData.getMaxSpeedKMperHours(id) / 3.6; // Meter pro Sekunde
        if(maxGeschwindigkeit == 0){
            maxGeschwindigkeit = 100;
        }
        return (laenge / maxGeschwindigkeit) * 1.33;
    }
}
