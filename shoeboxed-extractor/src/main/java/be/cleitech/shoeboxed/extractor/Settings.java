package be.cleitech.shoeboxed.extractor;

/**
 * Created by Pierrick on 23-12-16.
 */
public  class Settings {

    private static Settings instance;

    private Settings() {

    }

    public synchronized static Settings getInstance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }

}
