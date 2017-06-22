package lebr;

import nav.NavData;

public final class NavDataProvider {

    private static NavData navData;

    private NavDataProvider(){
        //Utility-Class
    }

    public static NavData getNavData() {
        return navData;
    }

    public static void setNavData(final NavData navData) {
        NavDataProvider.navData = navData;
    }
}
