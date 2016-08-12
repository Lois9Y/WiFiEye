package at.a9yards.wifieye.data;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Created by Lois-9Y on 11/08/2016.
 */
public class NetworkItem extends RealmObject {

    public static final String FIELDNAME_ID = "id";
    public static final String FIELDNAME_SSID = "SSID";
    public static final String FIELDNAME_LEVEL = "level";
    public static final String FIELDNAME_PASSWORD = "password";
    public static final String FIELDNAME_SCAN_DATE = "scanDate";

    public static final int MIN_SIGNAL_LEVEL= -90;
    public static final int BAR0_SIGNAL_LEVEL= -80;
    public static final int BAR1_SIGNAL_LEVEL= -70;
    public static final int BAR2_SIGNAL_LEVEL= -67;
    public static final int BAR3_SIGNAL_LEVEL= -55;
    public static final int BAR4_SIGNAL_LEVEL= -42;

    private static long uniqueKey = 0;

    @PrimaryKey
    private long id;
    @Required
    private String SSID;
    private int level;

    private String password;
    private Date scanDate;

    public NetworkItem() {
        this.id = ++uniqueKey;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!NetworkItem.class.isAssignableFrom(o.getClass())) {
            return false;
        }
        final NetworkItem other = (NetworkItem) o;
        if ((this.SSID == null) ? (other.SSID != null) : !this.SSID.equals(other.SSID)) {
            return false;
        }
        return true;
    }

    public boolean isPasswordAvailable(){


        if(password == null || password.equals(""))
            return false;

        return true;
    }

    public long getId() {
        return id;
    }

    public String getSSID() {
        return SSID;
    }

    public int getLevel() {
        return level;
    }

    public String getPassword() {
        return password;
    }

    public Date getScanDate() {
        return scanDate;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setSSID(String SSID) {
        this.SSID = SSID;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setScanDate(Date scanDate) {
        this.scanDate = scanDate;
    }
}
