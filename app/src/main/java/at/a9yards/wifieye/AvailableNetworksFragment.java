package at.a9yards.wifieye;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Log.d(LOG_TAG,"List view is: "+ listView);
        Realm realm = Realm.getDefaultInstance();

        if (mAdapter == null) {
            mAdapter = new AvailableNetworksAdapter(getActivity(), realm.where(NetworkItem.class).findAll());
            setListAdapter(mAdapter);
            //Log.d(LOG_TAG, "adapter set ");
        }
        if (wifiReceiver == null) {

            wifiReceiver = new WifiReceiver();
            manageWifi = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
            intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            //intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
            getActivity().registerReceiver(wifiReceiver, intentFilter);
            //Log.d(LOG_TAG, "registered");
        }

        setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initiateRefresh();
            }
        });
        initiateRefresh();
        //Log.d(LOG_TAG,"onCreate done")
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        NetworkItem item = mAdapter.getItem(position);
        if(item.isPasswordAvailable()){
            password = item.getPassword();
            ssid = item.getSSID();

            newConnection = true;
            tryNewWifiConnection();

        }else {
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
        cancelAllConnections();
        WifiConfiguration conf = new WifiConfiguration();

//           TODO: Add Scan result to REALM
//             Realm realm = Realm.getDefaultInstance();
//            final NetworkItem savedNetwork = realm.where(NetworkItem.class).equalTo(NetworkItem.FIELDNAME_SSID, data.getExtras().getString(AvailableNetworksFragment.SSID_FOR_SCAN)).findFirst();
//
        manageWifi = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        conf.SSID = "\"" + ssid + "\"";
        conf.preSharedKey = "\"" + password + "\"";

        if(!manageWifi.isWifiEnabled())
            manageWifi.setWifiEnabled(true);
        //Log.d(LOG_TAG, "wifi manager disconnect");

        int networkId = manageWifi.addNetwork(conf);
        Log.d(LOG_TAG, "wifi manager add conf "+networkId);
        if(networkId == -1){
            networkId = manageWifi.updateNetwork(conf);
            Log.d(LOG_TAG, "wifi manager updated conf "+networkId);
        }
        if(!manageWifi.enableNetwork(networkId, true)) {

            Log.d(LOG_TAG, "failed to enable network");
        }
        if(!manageWifi.reconnect())
            Log.d(LOG_TAG, "failed to reconnect");
        //Log.d(LOG_TAG, "wifi manager reconnect");
    }

    private void cancelAllConnections() {
        for (WifiConfiguration configuration : manageWifi.getConfiguredNetworks()) {
            manageWifi.disableNetwork(configuration.networkId);
        }

        manageWifi.disconnect();

    }

    private void connectionEstablished (){
        if(snackbar != null) {
            snackbar.dismiss();
            snackbar = Snackbar.make(getView(), "connection established \"" + ssid + "\"", Snackbar.LENGTH_SHORT);
            snackbar.show();
            TextView text =
                    (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
            text.setTextColor(getResources().getColor(R.color.icons));
            mAdapter.setListEnabled(true);
        }

        Realm realm = Realm.getDefaultInstance();
        NetworkItem persistHere = realm.where(NetworkItem.class).equalTo(NetworkItem.FIELDNAME_SSID, ssid.toString()).findFirst();
        if(persistHere !=null) {
            realm.beginTransaction();
            persistHere.setPassword(password.toString());
            realm.commitTransaction();
            mAdapter.notifyDataSetChanged();
        }


    }


    @Override
    public void onPause() {
        //Log.d(LOG_TAG, "unregister");
        getActivity().unregisterReceiver(wifiReceiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        //Log.d(LOG_TAG, "register");
        super.onResume();
        getActivity().registerReceiver(wifiReceiver, intentFilter);
    }

    private void initiateRefresh() {
        manageWifi.startScan();
    }

    boolean tryingToConnect =false;

    public class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d(LOG_TAG, "recieving" );
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {

                List<ScanResult> availableNetworkList = manageWifi.getScanResults();
                //Log.d(LOG_TAG, availableNetworkList.size() + " results recieved");

                Realm realm = Realm.getDefaultInstance();

                for (ScanResult network : availableNetworkList) {
                    if (network.level < NetworkItem.MIN_SIGNAL_LEVEL)
                        continue;
                    final NetworkItem savedNetwork = realm.where(NetworkItem.class).equalTo(NetworkItem.FIELDNAME_SSID, network.SSID).findFirst();
                    if (savedNetwork == null) {
                        createNetworkItemFromBroadcast(realm, network);

                    } else {
                        //update level
                        realm.beginTransaction();
                        savedNetwork.setLevel(network.level);
                        realm.commitTransaction();
                    }

                }
                mAdapter.updateData(realm.where(NetworkItem.class).findAll().sort(NetworkItem.FIELDNAME_LEVEL, Sort.DESCENDING));
                mAdapter.notifyDataSetChanged();
                setRefreshing(false);

            } else if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {

                /**
                 * FOR DEBUGGING: full supplication stats changes shown
                 */

                Log.d(LOG_TAG, ">>>>SUPPLICANT_STATE_CHANGED_ACTION<<<<<<");
                SupplicantState state = (intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE));


                if (newConnection) {

                    snackbar = Snackbar.make(getView(), "Connecting to \"" + ssid + "\" with password: " + password, Snackbar.LENGTH_INDEFINITE)
                            .setAction("CANCEL", new  View.OnClickListener() {
                        @Override
                        public void onClick (View v){
                            mAdapter.setListEnabled(true);
                            cancelAllConnections();
                        }
                    });
                    TextView text =
                            (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                    text.setTextColor(getResources().getColor(R.color.icons));

                    Log.d(LOG_TAG,((TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text)).getText().toString());
                    snackbar.show();
                    newConnection = false;
                    tryingToConnect = true;
                    mAdapter.setListEnabled(false);
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
                        if(tryingToConnect) {
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
                        //Something went wrong:
                        if(snackbar != null && snackbar.isShown()) {
                            snackbar.dismiss();
                            snackbar = Snackbar.make(getView(), "failed to enable network \"" + ssid + "\"", Snackbar.LENGTH_LONG);
                            TextView text =
                                    (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                            text.setTextColor(getResources().getColor(R.color.icons));
                            snackbar.show();
                            mAdapter.setListEnabled(true);
                            tryingToConnect = false;
                        }
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
                if (error != -1) {
                    if(snackbar!= null)
                        snackbar.dismiss();
                    mAdapter.setListEnabled(true);
                    ConnectionFailedDialogFragment dialog = new ConnectionFailedDialogFragment();
                    Bundle args = new Bundle();
                    args.putString(ConnectionFailedDialogFragment.SSID_ARGUMENT, ssid.toString());
                    args.putString(ConnectionFailedDialogFragment.PASSWORD_ARGUMENT, password.toString());
                    dialog.setArguments(args);
                    dialog.setTargetFragment(AvailableNetworksFragment.this, AvailableNetworksFragment.SCAN_REQUEST_CODE);
                    dialog.show(getActivity().getSupportFragmentManager(), "dialog");
                    tryingToConnect=false;

                }

                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                    Snackbar.make(getView(), "Connected to " + ssid, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    TextView text =
                            (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                    text.setTextColor(getResources().getColor(R.color.icons));
                    tryingToConnect=false;
                }
            }

        }
    }


    private void createNetworkItemFromBroadcast(Realm realm, ScanResult network) {
        NetworkItem item = new NetworkItem();
        item.setLevel(network.level);
        item.setSSID(network.SSID);
        item.setPassword("");


        realm.beginTransaction();
        realm.copyToRealm(item); // Persist unmanaged objects
        realm.commitTransaction();
    }
}
