package lebr;

import nav.NavData;

public class Link {

    private final int id;
    private final NavData navData;

    public Link(final int id, final NavData navData) {
        this.id = id;
        this.navData = navData;
    }

    public Crossing getFrom(){
        final int crossingIDFrom = navData.getCrossingIDFrom(id);
        return new Crossing(crossingIDFrom, navData);
    }

    public Crossing getTo(){
        final int crossingIDTo = navData.getCrossingIDTo(id);
        return new Crossing(crossingIDTo, navData);
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
