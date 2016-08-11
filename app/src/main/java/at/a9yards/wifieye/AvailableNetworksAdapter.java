package at.a9yards.wifieye;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import io.realm.OrderedRealmCollection;
import io.realm.RealmBaseAdapter;


import at.a9yards.wifieye.data.NetworkItem;

/**
 * Created by Lois-9Y on 11/08/2016.
 */
public class AvailableNetworksAdapter extends RealmBaseAdapter<NetworkItem> implements ListAdapter {

    private String LOG_TAG = AvailableNetworksAdapter.class.getSimpleName();

    public AvailableNetworksAdapter(@NonNull Context context, @Nullable OrderedRealmCollection<NetworkItem> data) {
        super(context, data);
    }

    public static class ViewHolder {

        final TextView  ssidTextView;
        final TextView strength;
        final TextView passAvail;

        public ViewHolder(View container) {
            this.ssidTextView = (TextView) container.findViewById(R.id.avail_net_ssid);
            this.strength = (TextView) container.findViewById(R.id.avail_net_strength);
            this.passAvail = (TextView) container.findViewById(R.id.avail_net_pass_avail);

        }
    }



    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Log.d(LOG_TAG,"getView called");

        ViewHolder viewHolder;
        if (convertView == null){
            Log.d(LOG_TAG,"init viewholder ");
            convertView = inflater.inflate(R.layout.avail_list_item,parent,false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }

        NetworkItem item = adapterData.get(position);
        viewHolder.ssidTextView.setText( item.getSSID());
        viewHolder.strength.setText( ""+item.getLevel());
        viewHolder.passAvail.setText( ""+item.isPasswordAvailable());

        return convertView;
    }

}
