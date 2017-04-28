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

    public int getKosten(){
        final int laenge = navData.getLengthMeters(id);
        final int geschwindigkeit = navData.getMaxSpeedKMperHours(id) * 1000; // Meter pro Stunde
        final int kostenInStunden = laenge / geschwindigkeit;
        return kostenInStunden * 3600;
    }
}
