package at.a9yards.wifieye;

import android.content.Context;
import android.graphics.PorterDuff;
import android.net.wifi.ScanResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.List;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmBaseAdapter;


import at.a9yards.wifieye.data.NetworkItem;
import io.realm.Sort;

/**
 * Created by Lois-9Y on 11/08/2016.
 */
public class AvailableNetworksAdapter extends RealmBaseAdapter<NetworkItem> implements ListAdapter {

    private String LOG_TAG = AvailableNetworksAdapter.class.getSimpleName();

    private boolean listEnabled = true;

    public AvailableNetworksAdapter(@NonNull Context context, @Nullable OrderedRealmCollection<NetworkItem> data) {
        super(context, data);
    }

    private static class ViewHolder {

        final TextView  ssidTextView;
        final ImageView strength;
        final ImageView passAvail;

        public ViewHolder(View container) {
            this.ssidTextView = (TextView) container.findViewById(R.id.avail_net_ssid);
            this.strength = (ImageView) container.findViewById(R.id.avail_net_strength);
            this.passAvail = (ImageView) container.findViewById(R.id.avail_net_pass_avail);

        }
    }

    @Override
    public boolean isEnabled(int position) {
        return listEnabled;
    }

    public void setListEnabled(boolean flag){
        listEnabled = flag;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //Log.d(LOG_TAG,"getView called at position "+ position );

        ViewHolder viewHolder;
        if (convertView == null){
            //Log.d(LOG_TAG,"init viewholder ");
            convertView = inflater.inflate(R.layout.avail_list_item,parent,false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }else{
            //Log.d(LOG_TAG,"init recycle viewholder ");
            viewHolder = (ViewHolder) convertView.getTag();
        }

        NetworkItem item = adapterData.get(position);
        viewHolder.ssidTextView.setText( item.getSSID());
        viewHolder.strength.setImageResource( levelToDrawable(item.getLevel()));
        viewHolder.strength.getDrawable().setColorFilter(convertView.getResources().getColor(R.color.accent),PorterDuff.Mode.SRC_ATOP);
        //viewHolder.passAvail.setText( ""+item.isPasswordAvailable());
        //viewHolder.passAvail.setText( ""+item.isPasswordAvailable());
        if(adapterData.get(position).isPasswordAvailable()){
            viewHolder.passAvail.setVisibility(View.VISIBLE);
            viewHolder.passAvail.setImageResource(R.drawable.ic_done_black_48dp);
            viewHolder.passAvail.getDrawable().setColorFilter(convertView.getResources().getColor(R.color.accent), PorterDuff.Mode.SRC_ATOP);
        }else{
            viewHolder.passAvail.setVisibility(View.GONE);
        }



        return convertView;
    }

    private int levelToDrawable(int level){
        if(level < NetworkItem.BAR0_SIGNAL_LEVEL)
            return R.drawable.ic_signal_wifi_0_bar_black_24dp;
        if(level < NetworkItem.BAR1_SIGNAL_LEVEL)
            return R.drawable.ic_signal_wifi_1_bar_black_24dp;
        if(level < NetworkItem.BAR2_SIGNAL_LEVEL)
            return R.drawable.ic_signal_wifi_2_bar_black_24dp;
        if(level < NetworkItem.BAR3_SIGNAL_LEVEL)
            return R.drawable.ic_signal_wifi_3_bar_black_24dp;

        return R.drawable.ic_signal_wifi_4_bar_black_24dp;
    }

    public void synchronizeData(List<ScanResult>availableNetworkList){
        Realm realm = Realm.getDefaultInstance();

        for (android.net.wifi.ScanResult network : availableNetworkList) {
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
        updateData(realm.where(NetworkItem.class).findAll().sort(NetworkItem.FIELDNAME_LEVEL, Sort.DESCENDING));
        notifyDataSetChanged();

    }

    private void createNetworkItemFromBroadcast(Realm realm, android.net.wifi.ScanResult network) {
        NetworkItem item = new NetworkItem();
        item.setLevel(network.level);
        item.setSSID(network.SSID);
        item.setPassword("");


        realm.beginTransaction();
        realm.copyToRealm(item); // Persist unmanaged objects
        realm.commitTransaction();

    }

    public void persistPassword (String ssid, String password){
        Realm realm = Realm.getDefaultInstance();
        NetworkItem persistHere = realm.where(NetworkItem.class).equalTo(NetworkItem.FIELDNAME_SSID, ssid.toString()).findFirst();
        if (persistHere != null) {
            realm.beginTransaction();
            persistHere.setPassword(password.toString());
            realm.commitTransaction();
            notifyDataSetChanged();
        }
    }
}
