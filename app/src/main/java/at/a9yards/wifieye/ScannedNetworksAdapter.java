package at.a9yards.wifieye;

import android.content.Context;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import at.a9yards.wifieye.data.NetworkItem;
import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmBaseAdapter;

/**
 * Created by Lois-9Y on 24/08/2016.
 */
public class ScannedNetworksAdapter extends RealmBaseAdapter<NetworkItem> implements ListAdapter {

    private final String LOG_TAG = ScannedNetworksAdapter.class.getSimpleName();

    public static class ViewHolder {

        final TextView ssidTextView;
        final ImageButton deleteButton;

        public ViewHolder(View container) {
            this.ssidTextView = (TextView) container.findViewById(R.id.history_net_ssid);
            this.deleteButton = (ImageButton) container.findViewById(R.id.history_net_delete);
        }
    }

    public ScannedNetworksAdapter(@NonNull Context context, @Nullable OrderedRealmCollection<NetworkItem> data) {
        super(context, data);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null){
            Log.d(LOG_TAG,"init viewholder ");
            convertView = inflater.inflate(R.layout.history_list_item,parent,false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }

        NetworkItem item = adapterData.get(position);
        viewHolder.ssidTextView.setText( item.getSSID());

        viewHolder.deleteButton.setImageResource(R.drawable.ic_clear_black_24dp);
        viewHolder.deleteButton.getDrawable().setColorFilter(convertView.getResources().getColor(R.color.accent), PorterDuff.Mode.SRC_ATOP);

        viewHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Realm realm = Realm.getDefaultInstance();
                NetworkItem item = realm.where(NetworkItem.class).equalTo(NetworkItem.FIELDNAME_SSID,adapterData.get(position).getSSID()).findFirst();
                realm.beginTransaction();
                item.setPassword(null);
                realm.commitTransaction();
                ScannedNetworksAdapter.this.notifyDataSetChanged();
            }
        });

        return convertView;
    }
}
