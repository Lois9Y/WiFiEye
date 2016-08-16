package at.a9yards.wifieye;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import at.a9yards.wifieye.data.Cheeses;
import at.a9yards.wifieye.data.NetworkItem;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;

import com.example.android.swiperefreshlistfragment.SwipeRefreshListFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lois-9Y on 10/08/2016.
 */
public class AvailableNetworksFragment extends SwipeRefreshListFragment {

    private String LOG_TAG = AvailableNetworksFragment.class.getSimpleName();
    private static final int SCAN_REQUEST_CODE = 101;
    public static final String SSID_FOR_SCAN = "ssid_for_scan";

    private WifiManager manageWifi;
    private WifiReceiver wifiReceiver;
    private IntentFilter intentFilter;
    private AvailableNetworksAdapter mAdapter;

    private CharSequence password = "";
    private CharSequence ssid = "";

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
            Log.d(LOG_TAG, "adapter set ");
        }
        if (wifiReceiver == null) {

            wifiReceiver = new WifiReceiver();
            manageWifi = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
            intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            //intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
            getActivity().registerReceiver(wifiReceiver, intentFilter);
            Log.d(LOG_TAG, "registered");
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

//

        Intent i = new Intent(getActivity(), ScanActivity.class);
        i.putExtra(SSID_FOR_SCAN, mAdapter.getItem(position).getSSID());
        startActivityForResult(i, SCAN_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == SCAN_REQUEST_CODE) && (resultCode == getActivity().RESULT_OK)) {

            password = data.getExtras().getString(ScanActivityFragment.PASSWORD_SCAN_RESULT);
            ssid = data.getExtras().getString(SSID_FOR_SCAN);


            WifiConfiguration conf = new WifiConfiguration();

//           TODO: Add Scan result to REALM
//             Realm realm = Realm.getDefaultInstance();
//            final NetworkItem savedNetwork = realm.where(NetworkItem.class).equalTo(NetworkItem.FIELDNAME_SSID, data.getExtras().getString(AvailableNetworksFragment.SSID_FOR_SCAN)).findFirst();
//

            conf.SSID = "\"" + ssid + "\"";
            conf.preSharedKey = "\"" + password + "\"";


            manageWifi.disconnect();
            int networkId = manageWifi.addNetwork(conf);
            manageWifi.enableNetwork(networkId, true);
            manageWifi.reconnect();


        }
    }


    @Override
    public void onPause() {
        Log.d(LOG_TAG, "unregister");
        getActivity().unregisterReceiver(wifiReceiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d(LOG_TAG, "register");
        super.onResume();
        getActivity().registerReceiver(wifiReceiver, intentFilter);
    }

    private void initiateRefresh() {
        manageWifi.startScan();
    }

    public class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d(LOG_TAG, "recieving" );
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {

                List<ScanResult> availableNetworkList = manageWifi.getScanResults();
                Log.d(LOG_TAG, availableNetworkList.size() + " results recieved");

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
                        /**
                         * DUMMY IMPL FOR PASSWORD BELOW
                         */
                        if (network.SSID.contains("9yards") && !network.SSID.contains("Guest"))
                            savedNetwork.setPassword("\"9Ymedia#\"");
                        realm.commitTransaction();
                    }

                }
                mAdapter.updateData(realm.where(NetworkItem.class).findAll().sort(NetworkItem.FIELDNAME_LEVEL, Sort.DESCENDING));
                mAdapter.notifyDataSetChanged();
                setRefreshing(false);
//            } else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
//
//                NetworkInfo networkInfo =
//                        intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
//                int duration = Toast.LENGTH_SHORT;
//                Toast toast;
//                switch (networkInfo.getState()) {
//                    case CONNECTING:
//
//                        toast = Toast.makeText(context, "connecting", duration);
//                        toast.show();
//                        break;
//                    case CONNECTED:
//
//                        toast = Toast.makeText(context, "connected", duration);
//                        toast.show();
//                        break;
//
//                    case DISCONNECTED:
//                        if (networkInfo.getDetailedState() == NetworkInfo.DetailedState.FAILED) {
//                            PasswordDialogFragment dialog = new PasswordDialogFragment();
//                            Bundle args = new Bundle();
//                            args.putString(PasswordDialogFragment.SSID_ARGUMENT, ssid.toString());
//                            args.putString(PasswordDialogFragment.PASSWORD_ARGUMENT, password.toString());
//                            dialog.setArguments(args);
//                            dialog.show(getActivity().getSupportFragmentManager(), "dialog");
//
//                        }
//                        break;
//                }
            } else if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {

                /**
                 * FOR DEBUGGING: full supplication stats changes shown
                 */

                Log.d("WifiReceiver", ">>>>SUPPLICANT_STATE_CHANGED_ACTION<<<<<<");
                SupplicantState state = (intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE));
                int duration = Toast.LENGTH_SHORT;
                Toast toast;
                toast = Toast.makeText(context, "connecting", duration);
                toast.show();

                switch (state) {
                    case ASSOCIATED:
                        Log.i("SupplicantState", "ASSOCIATED");
                        break;
                    case ASSOCIATING:
                        Log.i("SupplicantState", "ASSOCIATING");
                        break;

                    case AUTHENTICATING:
                        Log.i("SupplicantState", "Authenticating...");
                        break;
                    case COMPLETED:
                        Log.i("SupplicantState", "Connected");

                        toast = Toast.makeText(context, "connected", duration);
                        toast.show();
                        break;
                    case DISCONNECTED:
                        Log.i("SupplicantState", "Disconnected");
                        break;
                    case DORMANT:
                        Log.i("SupplicantState", "DORMANT");
                        break;
                    case FOUR_WAY_HANDSHAKE:
                        Log.i("SupplicantState", "FOUR_WAY_HANDSHAKE");
                        break;
                    case GROUP_HANDSHAKE:
                        Log.i("SupplicantState", "GROUP_HANDSHAKE");
                        break;
                    case INACTIVE:
                        Log.i("SupplicantState", "INACTIVE");
                        break;
                    case INTERFACE_DISABLED:
                        Log.i("SupplicantState", "INTERFACE_DISABLED");
                        break;
                    case INVALID:
                        Log.i("SupplicantState", "INVALID");
                        break;
                    case SCANNING:
                        Log.i("SupplicantState", "SCANNING");
                        break;
                    case UNINITIALIZED:
                        Log.i("SupplicantState", "UNINITIALIZED");
                        break;
                    default:
                        Log.i("SupplicantState", "Unknown");
                        break;

                }
                int error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                if (error == WifiManager.ERROR_AUTHENTICATING) {
                    PasswordDialogFragment dialog = new PasswordDialogFragment();
                    Bundle args = new Bundle();
                    args.putString(PasswordDialogFragment.SSID_ARGUMENT, ssid.toString());
                    args.putString(PasswordDialogFragment.PASSWORD_ARGUMENT, password.toString());
                    dialog.setArguments(args);
                    dialog.show(getActivity().getSupportFragmentManager(), "dialog");
                }
            }

        }
    }


    private void updateRealmItem(NetworkItem saved, ScanResult scanned) {
        //TODO: Persist Passwords.
    }

    private void createNetworkItemFromBroadcast(Realm realm, ScanResult network) {
        NetworkItem item = new NetworkItem();
        item.setLevel(network.level);
        item.setSSID(network.SSID);
        item.setPassword("");

        /**
         * DUMMY IMPL FOR PASSWORD BELOW
         */
        if (network.SSID.contains("9yards") && !network.SSID.contains("Guest"))
            item.setPassword("\"9Ymedia#\"");


        realm.beginTransaction();
        realm.copyToRealm(item); // Persist unmanaged objects
        realm.commitTransaction();
    }
}
