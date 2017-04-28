package lebr;

public class Params {

    private final String dbAccess;
    private final String cacFile;
    private final double latitude;
    private final double longitude;
    private final int minutes;
    private final int fromLSI;
    private final int toLSI;

    // Parameter:           Testaufruf:
    // 0: <dbaccesstr>      geosrv.informatik.fh-nuernberg.de/5432/dbuser/dbuser/deproDB20
    // 1: <cacfile>         CAR_CACHE_de_noCC.CAC
    // 2: <latitude>        49.46591000
    // 3: <longitude>       11.15800500
    // 4: <minutes>         15
    // 5: <fromLSI>         20505600
    // 6: <toLSI>           20505699
    public Params(final String[] args) {
        if(args.length != 7){
            throw new IllegalArgumentException(args.length + " Argumente übergeben; 7 erwartet!");
        }
        dbAccess = args[0];
        cacFile = args[1];
        latitude = Double.parseDouble(args[2]);
        longitude = Double.parseDouble(args[3]);
        minutes = Integer.parseInt(args[4]);
        fromLSI = Integer.parseInt(args[5]);
        toLSI = Integer.parseInt(args[6]);
    }
}
