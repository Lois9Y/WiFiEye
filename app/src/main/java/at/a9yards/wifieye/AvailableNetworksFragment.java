package at.a9yards.wifieye;


import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;


import at.a9yards.wifieye.data.NetworkItem;
import io.realm.Realm;


import com.example.android.swiperefreshlistfragment.SwipeRefreshListFragment;



import java.util.List;

/**
 * Created by Lois-9Y on 10/08/2016.
 */
public class AvailableNetworksFragment extends SwipeRefreshListFragment {


    private String LOG_TAG = AvailableNetworksFragment.class.getSimpleName();
    public static final int SCAN_REQUEST_CODE = 101;
    public static final String SSID_FOR_SCAN = "ssid_for_scan";
    public static final String PASSWORD_SCAN_RESULT = "password_scan_result";

    private WifiReceiver wifiReceiver;
    private IntentFilter intentFilter;
    private AvailableNetworksAdapter mAdapter;

    private CharSequence password = "";
    private CharSequence ssid = "";

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
        snackbar = Snackbar.make(getView(),"dummy",Snackbar.LENGTH_SHORT);
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

            wifiReceiver = new WifiReceiver(this.getActivity());
            wifiReceiver.setOnWifiReceiverListener(this.wifiListener);

            intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
            getActivity().registerReceiver(wifiReceiver, intentFilter);
        }

        setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                wifiReceiver.initiateRefresh();
            }
        });
        wifiReceiver.initiateRefresh();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        NetworkItem item = mAdapter.getItem(position);
        if (item.isPasswordAvailable()) {
            password = item.getPassword();
            ssid = item.getSSID();
            this.wifiReceiver.tryNewWifiConnection(""+ssid,""+password);

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

            this.wifiReceiver.tryNewWifiConnection(""+ssid,""+password);
        }
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

        mAdapter.persistPassword("" + ssid, "" + password);

    }

    private WifiReceiver.OnWifiReceiverListener wifiListener = new WifiReceiver.OnWifiReceiverListener(){
        @Override
        public void onScansAvailable(List<ScanResult> scanResults) {
            mAdapter.synchronizeData(scanResults);
            setRefreshing(false);
        }

        @Override
        public void onNewWifiConnectionCreated() {
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
            mAdapter.setListEnabled(false);
        }

        @Override
        public void onNewWifiConnectionFailed() {
            snackbar = Snackbar.make(getView(), "Cannot edit WiFi connection " + ssid + "- locked by device Wifi Manager", Snackbar.LENGTH_LONG);
            snackbar.show();
            TextView text =
                    (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
            text.setTextColor(getResources().getColor(R.color.accent));

        }

        @Override
        public void onConnectionEstablished() {
            AvailableNetworksFragment.this.connectionEstablished();
        }

        @Override
        public void onConnectionFailed() {
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
        }
    };
}
