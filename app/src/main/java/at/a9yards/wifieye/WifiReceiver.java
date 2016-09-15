package at.a9yards.wifieye;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

/**
 * Created by Lois-9Y on 15/09/2016.
 */
public class WifiReceiver extends BroadcastReceiver {

    private String LOG_TAG = this.getClass().getSimpleName();
    private WifiManager mWifiManager;
    private String ssid;
    private String password;
    private boolean newConnection;
    private boolean tryingToConnect = false;


    public interface OnWifiReceiverListener{
        void onScansAvailable(List<ScanResult> scanResults);
        void onNewWifiConnectionCreated();
        void onNewWifiConnectionFailed();
        void onConnectionEstablished();
        void onConnectionFailed();

    }

    private OnWifiReceiverListener listener = new OnWifiReceiverListener(){
        @Override
        public void onScansAvailable(List<ScanResult> scanResults) {

        }

        @Override
        public void onNewWifiConnectionCreated() {

        }

        @Override
        public void onNewWifiConnectionFailed() {

        }

        @Override
        public void onConnectionEstablished() {

        }

        @Override
        public void onConnectionFailed() {

        }
    };

    public WifiReceiver(Context context) {
        super();
        mWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    }

    public void setOnWifiReceiverListener(OnWifiReceiverListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //Log.d(LOG_TAG, "recieving" );
        if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {

            List<ScanResult> availableNetworkList = mWifiManager.getScanResults();
            //Log.d(LOG_TAG, availableNetworkList.size() + " results recieved");

            listener.onScansAvailable(availableNetworkList);

        } else if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {

            if (!mWifiManager.getConnectionInfo().getSSID().equals("\"" + this.ssid + "\"")) {

                return;
            }

            int error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
            if (error != -1 && !ssid.equals("")) {

                tryingToConnect = false;
                listener.onConnectionFailed();

            }


            Log.d(LOG_TAG, ">>>>SUPPLICANT_STATE_CHANGED_ACTION<<<<<<");
            SupplicantState state = (intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE));


            if (newConnection && !ssid.equals("")) {
                newConnection = false;
                tryingToConnect = true;

                listener.onNewWifiConnectionCreated();
                return;
            }
            /**
             * FOR DEBUGGING: full supplication stats changes shown
             */
            switch (state) {
                case ASSOCIATED:
                    Log.i(LOG_TAG, "ASSOCIATED");
                    break;
                case ASSOCIATING:
                    Log.i(LOG_TAG, "ASSOCIATING");
                    break;

                case AUTHENTICATING:
                    Log.i(LOG_TAG, "Authenticating...");
                    break;
                case COMPLETED:
                    Log.i(LOG_TAG, "Connected");
                    if (tryingToConnect) {
                        tryingToConnect = false;
                        listener.onConnectionEstablished();
                        return;
                    }
                    break;
                case DISCONNECTED:
                    Log.i(LOG_TAG, "Disconnected");
                    break;
                case DORMANT:
                    Log.i(LOG_TAG, "DORMANT");
                    break;
                case FOUR_WAY_HANDSHAKE:
                    Log.i(LOG_TAG, "FOUR_WAY_HANDSHAKE");
                    break;
                case GROUP_HANDSHAKE:
                    Log.i(LOG_TAG, "GROUP_HANDSHAKE");
                    break;
                case INACTIVE:
                    Log.i(LOG_TAG, "INACTIVE");
                    break;
                case INTERFACE_DISABLED:
                    Log.i(LOG_TAG, "INTERFACE_DISABLED");
                    break;
                case INVALID:
                    Log.i(LOG_TAG, "INVALID");
                    break;
                case SCANNING:
                    Log.i(LOG_TAG, "SCANNING");
                    break;
                case UNINITIALIZED:
                    Log.i(LOG_TAG, "UNINITIALIZED");
                    break;
                default:
                    Log.i(LOG_TAG, "Unknown");
                    break;

            }


//            if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
//                Snackbar.make(getView(), "Connected to " + ssid, Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//                TextView text =
//                        (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
//                text.setTextColor(getResources().getColor(R.color.icons));
//                listener.onConnectionEstablished();
//                tryingToConnect = false;
//            }
        }

    }

    public void tryNewWifiConnection(String ssid, String password) {
        this.ssid = ssid;
        this.password = password;
        //cancelAllConnections();
        WifiConfiguration found = findNetworkInExistingConfig("\"" + ssid + "\"");

        mWifiManager.disconnect();
        int networkId = -1;
        if (found == null) {
            Log.d(LOG_TAG, " network not found");
            found = getNewWifiConfig();
            networkId = mWifiManager.addNetwork(found);
        } else {
            //statusView.setText(R.string.wifi_modifying_network);
            Log.d(LOG_TAG, "found network " + found.networkId);
            WifiConfiguration sparse = new WifiConfiguration();
            sparse.networkId = found.networkId;
            sparse.preSharedKey = "\"" + this.password + "\"";
            //Log.d(LOG_TAG,sparse.toString());
            networkId = mWifiManager.updateNetwork(sparse);
            if (networkId < 0) {
                Log.d(LOG_TAG, "unable to update " + networkId);
                listener.onNewWifiConnectionFailed();
                newConnection = false;
                mWifiManager.reassociate();
                return;
            }
        }

        Log.d(LOG_TAG, "Inserted/Modified network " + networkId);
        if (networkId < 0) {
            Log.d(LOG_TAG, "FAILURE_ADDING_NETWORK_CONFIG");
            listener.onNewWifiConnectionFailed();
            return;
        }


        if (!mWifiManager.saveConfiguration()) {
            Log.d(LOG_TAG, "FAILURE_SAVING_NETWORK_CONFIGURATION");
            listener.onNewWifiConnectionFailed();
            return;
        }

        // Try to disable the current network and start a new one.
        if (!mWifiManager.enableNetwork(networkId, true)) {
            Log.d(LOG_TAG, "FAILURE_STARTING_NEW_NETWORK");
            listener.onNewWifiConnectionFailed();
            return;
        }
        newConnection = true;
        mWifiManager.reassociate();
    }

    public WifiConfiguration findNetworkInExistingConfig(String ssid) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals(ssid)) {
                return existingConfig;
            }
        }
        return null;
    }



    public void initiateRefresh() {
        mWifiManager.startScan();
    }


    private WifiConfiguration getNewWifiConfig() {
        WifiConfiguration wifiConfig = new WifiConfiguration();

        wifiConfig.allowedAuthAlgorithms.clear();
        wifiConfig.allowedGroupCiphers.clear();
        wifiConfig.allowedKeyManagement.clear();
        wifiConfig.allowedPairwiseCiphers.clear();
        wifiConfig.allowedProtocols.clear();


        wifiConfig.preSharedKey = "\"" + this.password + "\"";
        wifiConfig.SSID = "\"" + this.ssid + "\"";

        return wifiConfig;
    }
}

