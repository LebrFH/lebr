package lebr;

public class Params {

    /**
     * Parameter:           Testaufruf:
     * 0: <dbaccesstr>      geosrv.informatik.fh-nuernberg.de/5432/dbuser/dbuser/deproDB20
     * 1: <cacfile>         CAR_CACHE_de_noCC.CAC
     * 2: <latitude>        49.46591000
     * 3: <longitude>       11.15800500
     * 4: <minutes>         15
     * 5: <fromLSI>         20505600
     * 6: <toLSI>           20505699
     */
    public static final Params TEST_PARAMS = new Params(new String[]{
            "geosrv.informatik.fh-nuernberg.de/5432/dbuser/dbuser/deproDB20",
            "CAR_CACHE_mittelfranken_noCC.CAC",
            "49.46591000",
            "11.15800500",
            "20",
            "20505600",
            "20505699"
    });

    private final String dbAccess;
    private final String cacFile;
    private final double latitude;
    private final double longitude;
    private final int seconds;
    private final int fromLSI;
    private final int toLSI;

    public Params(final String[] args) {
        if (args.length != 7) {
            throw new IllegalArgumentException(args.length + " Argumente übergeben; 7 erwartet!");
        }
        dbAccess = args[0];
        cacFile = args[1];
        latitude = Double.parseDouble(args[2]) * 1000000; //TODO di
        longitude = Double.parseDouble(args[3]) * 1000000; //TODO di
        seconds = Integer.parseInt(args[4]) * 60; // Uebergeben werden Minuten
        fromLSI = Integer.parseInt(args[5]);
        toLSI = Integer.parseInt(args[6]);
    }

    public String getDbAccess() {
        return dbAccess;
    }

    public String getCacFile() {
        return cacFile;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getSeconds() {
        return seconds;
    }

    public int getFromLSI() {
        return fromLSI;
    }

    public int getToLSI() {
        return toLSI;
    }
}
