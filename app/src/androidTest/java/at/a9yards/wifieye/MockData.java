package at.a9yards.wifieye;

import at.a9yards.wifieye.data.NetworkItem;

/**
 * Created by Lois-9Y on 14/09/2016.
 */
public class MockData {

    public static String ssid1 = "someSSID";
    public static String ssid2 = "someOtherSSID";
    public static int strongLevel = NetworkItem.BAR3_SIGNAL_LEVEL +1;
    public static int weakLevel = NetworkItem.BAR1_SIGNAL_LEVEL -1;
    public static String password = "somepassword";

    public static NetworkItem createNetworkItemWithPassword(){
        NetworkItem withPass = new NetworkItem();
        withPass.setSSID(ssid1);
        withPass.setLevel(strongLevel);
        withPass.setPassword(password);
        return withPass;
    }
    public static NetworkItem createNetworkItemNoPassword(){
        NetworkItem withoutPass = new NetworkItem();
        withoutPass.setSSID(ssid2);
        withoutPass.setLevel(weakLevel);
        withoutPass.setPassword("");
        return withoutPass;
    }
}
