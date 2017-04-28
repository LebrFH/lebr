package lebr;

import nav.NavData;

import java.util.ArrayList;
import java.util.List;

public class Crossing {

    private final int id;
    private final NavData navData;

    public Crossing(final NavData navData, final int latitude, final int longitude) {
        this.navData = navData;
        this.id = navData.getNearestCrossing(latitude, longitude);
    }

    public List<Link> getLinks(){
        final List<Link> links = new ArrayList<>();
        final int[] linksForCrossing = navData.getLinksForCrossing(id);
        for (final int linkId : linksForCrossing) {
            links.add(new Link(linkId, navData));
        }
        return links;
    }

    public int getLatitude() {
        return navData.getCrossingLatE6(id);
    }

    public int getLongitude() {
        return navData.getCrossingLongE6(id);
    }
}
