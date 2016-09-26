package at.a9yards.wifieye;


import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001;
    private static final int PERMISSIONS_REQUEST_CAMERA = 1002;

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
        snackbar = Snackbar.make(getView(), R.string.snackbar_dummy, Snackbar.LENGTH_SHORT);
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
        setUpWifiReciever();


        setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                wifiReceiver.initiateRefresh();
            }
        });

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
         ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            Log.d(LOG_TAG,"requesting WIFI permissions");

            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);

            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method

        } else {
            Log.d(LOG_TAG,"no Runtime Permissions");
            //setUpWifiReciever();
            wifiReceiver.initiateRefresh();
            //do something, permission was previously granted; or legacy device
        }



    }

    private void setUpWifiReciever(){
        if (wifiReceiver == null) {

            wifiReceiver = new WifiReceiver(this.getActivity());
            wifiReceiver.setOnWifiReceiverListener(this.wifiListener);

            intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
            getActivity().registerReceiver(wifiReceiver, intentFilter);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(LOG_TAG,"WIFI permissions granted");
                //setUpWifiReciever();
                wifiReceiver.initiateRefresh();
            }
            else Log.d(LOG_TAG, "Permission denied");
        }
        if (requestCode == PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent i = new Intent(getActivity(), ScanActivity.class);
                i.putExtra(SSID_FOR_SCAN, ssid);
                startActivityForResult(i, SCAN_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        NetworkItem item = mAdapter.getItem(position);
        if (item.isPasswordAvailable()) {
            password = item.getPassword();
            ssid = item.getSSID();
            this.wifiReceiver.tryNewWifiConnection("" + ssid, "" + password);

        } else {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ) {


                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA},
                        PERMISSIONS_REQUEST_CAMERA);
            }else {
                Intent i = new Intent(getActivity(), ScanActivity.class);
                i.putExtra(SSID_FOR_SCAN, mAdapter.getItem(position).getSSID());
                startActivityForResult(i, SCAN_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == SCAN_REQUEST_CODE) && (resultCode == getActivity().RESULT_OK)) {

            password = data.getExtras().getString(PASSWORD_SCAN_RESULT);
            ssid = data.getExtras().getString(SSID_FOR_SCAN);

            this.wifiReceiver.tryNewWifiConnection("" + ssid, "" + password);
        }
    }

    private void connectionEstablished() {
        if (snackbar != null) {
            snackbar.dismiss();
            snackbar = Snackbar.make(getView(), String.format(getResources().getString(R.string.snackbar_connection_established), ssid), Snackbar.LENGTH_SHORT);
            snackbar.show();
            TextView text =
                    (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
            text.setTextColor(getResources().getColor(R.color.icons));
            mAdapter.setListEnabled(true);
        }

        mAdapter.persistPassword("" + ssid, "" + password);

    }

    private WifiReceiver.OnWifiReceiverListener wifiListener = new WifiReceiver.OnWifiReceiverListener() {
        @Override
        public void onScansAvailable(List<ScanResult> scanResults) {
            mAdapter.synchronizeData(scanResults);
            setRefreshing(false);
        }

        @Override
        public void onNewWifiConnectionCreated() {
            snackbar = Snackbar.make(getView(), String.format(getResources().getString(R.string.snackbar_connecting), ssid, password), Snackbar.LENGTH_INDEFINITE)
                    .setAction(getResources().getString(R.string.button_text_cancel), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mAdapter.setListEnabled(true);
                            wifiReceiver.stopConnection();
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
            snackbar = Snackbar.make(getView(), String.format(getResources().getString(R.string.snackbar_connection_failure), ssid), Snackbar.LENGTH_INDEFINITE)
                    .setAction(getResources().getString(R.string.button_text_retry), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AvailableNetworksFragment.this.wifiReceiver.tryNewWifiConnection("" + ssid, "" + password);
                        }
                    });
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
