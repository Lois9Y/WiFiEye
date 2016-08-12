package at.a9yards.wifieye;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
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
    private static final int DISPLAY_ITEM_COUNT = 20;


    private WifiManager manageWifi;
    private WifiReceiver wifiReceiver;
    private AvailableNetworksAdapter mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Log.d(LOG_TAG,"List view is: "+ listView);
        Realm realm = Realm.getDefaultInstance();

        if(mAdapter == null) {
            mAdapter = new AvailableNetworksAdapter(getActivity(), realm.where(NetworkItem.class).findAll());
            setListAdapter(mAdapter);
            Log.d(LOG_TAG, "adapter set ");
        }
        if(wifiReceiver == null) {

            wifiReceiver = new WifiReceiver();
            manageWifi = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
            getActivity().registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
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

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\""+mAdapter.getItem(position).getSSID()+"\"";
        conf.preSharedKey ="\""+mAdapter.getItem(position).getPassword()+"\"";

        manageWifi.addNetwork(conf);
        List<WifiConfiguration> list = manageWifi.getConfiguredNetworks();

        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + mAdapter.getItem(position).getSSID() + "\"")) {
                manageWifi.disconnect();
                manageWifi.enableNetwork(i.networkId, true);
                manageWifi.reconnect();

                break;
            }
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
        getActivity().registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
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
                        if(network.SSID.contains("9yards") && !network.SSID.contains("Guest"))
                            savedNetwork.setPassword("\"9Ymedia#\"");
                        realm.commitTransaction();
                    }

                }
                mAdapter.updateData(realm.where(NetworkItem.class).findAll().sort(NetworkItem.FIELDNAME_LEVEL, Sort.DESCENDING));
                mAdapter.notifyDataSetChanged();
                setRefreshing(false);
            }
        }

    }

    private void createNetworkItemFromBroadcast(Realm realm, ScanResult network) {
        NetworkItem item = new NetworkItem();
        item.setLevel(network.level);
        item.setSSID(network.SSID);
        item.setPassword("");

        /**
         * DUMMY IMPL FOR PASSWORD BELOW
         */
        if(network.SSID.contains("9yards") && !network.SSID.contains("Guest"))
            item.setPassword("\"9Ymedia#\"");



        realm.beginTransaction();
        realm.copyToRealm(item); // Persist unmanaged objects
        realm.commitTransaction();
    }
}
