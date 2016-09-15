package at.a9yards.wifieye;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import at.a9yards.wifieye.data.NetworkItem;
import io.realm.Realm;
import io.realm.Sort;

import com.example.android.swiperefreshlistfragment.SwipeRefreshListFragment;

import org.w3c.dom.Text;

import java.util.List;

/**
 * Created by Lois-9Y on 10/08/2016.
 */
public class AvailableNetworksFragment extends SwipeRefreshListFragment {


    private String LOG_TAG = AvailableNetworksFragment.class.getSimpleName();
    public static final int SCAN_REQUEST_CODE = 101;
    public static final String SSID_FOR_SCAN = "ssid_for_scan";
    public static final String PASSWORD_SCAN_RESULT = "password_scan_result";

    private WifiManager manageWifi;
    private WifiReceiver wifiReceiver;
    private IntentFilter intentFilter;
    private AvailableNetworksAdapter mAdapter;

    private CharSequence password = "";
    private CharSequence ssid = "";

    private boolean newConnection = false;

    private Snackbar snackbar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public void onPause() {
        getActivity().unregisterReceiver(wifiReceiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(wifiReceiver, intentFilter);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Realm realm = Realm.getDefaultInstance();

        if (mAdapter == null) {
            mAdapter = new AvailableNetworksAdapter(getActivity(), realm.where(NetworkItem.class).findAll());
            setListAdapter(mAdapter);
        }
        if (wifiReceiver == null) {

            wifiReceiver = new WifiReceiver();
            manageWifi = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
            intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
            getActivity().registerReceiver(wifiReceiver, intentFilter);
        }

        setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initiateRefresh();
            }
        });
        initiateRefresh();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        NetworkItem item = mAdapter.getItem(position);
        if (item.isPasswordAvailable()) {
            password = item.getPassword();
            ssid = item.getSSID();

            newConnection = true;
            tryNewWifiConnection();

        } else {
            Intent i = new Intent(getActivity(), ScanActivity.class);
            i.putExtra(SSID_FOR_SCAN, mAdapter.getItem(position).getSSID());
            startActivityForResult(i, SCAN_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == SCAN_REQUEST_CODE) && (resultCode == getActivity().RESULT_OK)) {

            password = data.getExtras().getString(PASSWORD_SCAN_RESULT);
            ssid = data.getExtras().getString(SSID_FOR_SCAN);

            newConnection = true;
            tryNewWifiConnection();
        }
    }

    private void tryNewWifiConnection() {
        //cancelAllConnections();
        WifiConfiguration found = findNetworkInExistingConfig("\"" + ssid + "\"");

        manageWifi.disconnect();
        int networkId = -1;
        if (found == null) {
            Log.d(LOG_TAG, " network not found");
            found = getNewWifiConfig();
            networkId = manageWifi.addNetwork(found);
        } else {
            //statusView.setText(R.string.wifi_modifying_network);
            Log.d(LOG_TAG, "found network " + found.networkId);
            WifiConfiguration sparse = new WifiConfiguration();
            sparse.networkId = found.networkId;
            sparse.preSharedKey = "\""+this.password+"\"";
            //Log.d(LOG_TAG,sparse.toString());
            networkId = manageWifi.updateNetwork(sparse);
            if(networkId <0) {
                snackbar = Snackbar.make(getView(), "Cannot edit WiFi connection " + ssid + "- locked by device owner", Snackbar.LENGTH_LONG);
                snackbar.show();
                TextView text =
                        (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                text.setTextColor(getResources().getColor(R.color.accent));
                Log.d(LOG_TAG, "unable to update " + networkId);
                newConnection = false;
                manageWifi.reassociate();
                return;
            }
        }

        Log.d(LOG_TAG, "Inserted/Modified network " + networkId);
        if (networkId < 0)
            Log.d(LOG_TAG, "FAILURE_ADDING_NETWORK_CONFIG");




        newConnection = true;
        if(!manageWifi.saveConfiguration())
            Log.d(LOG_TAG, "FAILURE_SAVING_NETWORK_CONFIGURATION");

        // Try to disable the current network and start a new one.
        if (!manageWifi.enableNetwork(networkId, true))
            Log.d(LOG_TAG, "FAILURE_STARTING_NEW_NETWORK");
        manageWifi.reassociate();
    }

    private WifiConfiguration findNetworkInExistingConfig(String ssid) {
        List<WifiConfiguration> existingConfigs = manageWifi.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals(ssid)) {
                return existingConfig;
            }
        }
        return null;
    }


    private void connectionEstablished() {
        if (snackbar != null) {
            snackbar.dismiss();
            snackbar = Snackbar.make(getView(), "connection established \"" + ssid + "\"", Snackbar.LENGTH_SHORT);
            snackbar.show();
            TextView text =
                    (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
            text.setTextColor(getResources().getColor(R.color.icons));
            mAdapter.setListEnabled(true);
        }

        mAdapter.persistPassword(""+ssid,""+password);

    }



    private void initiateRefresh() {
        manageWifi.startScan();
    }

    boolean tryingToConnect = false;

    public class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d(LOG_TAG, "recieving" );
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {

                List<ScanResult> availableNetworkList = manageWifi.getScanResults();
                //Log.d(LOG_TAG, availableNetworkList.size() + " results recieved");

                mAdapter.synchronizeData(availableNetworkList);
                setRefreshing(false);

            } else if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {

                if(!manageWifi.getConnectionInfo().getSSID().equals("\""+AvailableNetworksFragment.this.ssid+"\"")){

                    return;
                }

                /**
                 * FOR DEBUGGING: full supplication stats changes shown
                 */

                Log.d(LOG_TAG, ">>>>SUPPLICANT_STATE_CHANGED_ACTION<<<<<<");
                SupplicantState state = (intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE));


                if (newConnection && !ssid.equals("")) {

                    snackbar = Snackbar.make(getView(), "Connecting to \"" + ssid + "\" with password: " + password, Snackbar.LENGTH_INDEFINITE)
                            .setAction("CANCEL", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mAdapter.setListEnabled(true);

                                }
                            });
                    TextView text =
                            (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                    text.setTextColor(getResources().getColor(R.color.icons));

                    Log.d(LOG_TAG, ((TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text)).getText().toString());
                    snackbar.show();
                    newConnection = false;
                    tryingToConnect = true;
                    mAdapter.setListEnabled(false);

                    return;
                }

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
                            AvailableNetworksFragment.this.connectionEstablished();
                            tryingToConnect = false;
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
                int error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                if (error != -1 && !ssid.equals("")) {
                    if (snackbar != null)
                        snackbar.dismiss();
                    mAdapter.setListEnabled(true);
                    ConnectionFailedDialogFragment dialog = new ConnectionFailedDialogFragment();
                    Bundle args = new Bundle();
                    args.putString(ConnectionFailedDialogFragment.SSID_ARGUMENT, ssid.toString());
                    args.putString(ConnectionFailedDialogFragment.PASSWORD_ARGUMENT, password.toString());
                    dialog.setArguments(args);
                    dialog.setTargetFragment(AvailableNetworksFragment.this, AvailableNetworksFragment.SCAN_REQUEST_CODE);
                    dialog.show(getActivity().getSupportFragmentManager(), "dialog");

                    tryingToConnect = false;

                }

                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                    Snackbar.make(getView(), "Connected to " + ssid, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    TextView text =
                            (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                    text.setTextColor(getResources().getColor(R.color.icons));
                    tryingToConnect = false;
                }
            }

        }
    }




    private WifiConfiguration getNewWifiConfig() {
        WifiConfiguration wifiConfig = new WifiConfiguration();

        wifiConfig.allowedAuthAlgorithms.clear();
        wifiConfig.allowedGroupCiphers.clear();
        wifiConfig.allowedKeyManagement.clear();
        wifiConfig.allowedPairwiseCiphers.clear();
        wifiConfig.allowedProtocols.clear();


        wifiConfig.preSharedKey = "\""+this.password+"\"";
        wifiConfig.SSID = "\""+this.ssid+"\"";

        return wifiConfig;
    }
}
