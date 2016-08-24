package at.a9yards.wifieye;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.android.swiperefreshlistfragment.SwipeRefreshListFragment;

import at.a9yards.wifieye.data.NetworkItem;
import io.realm.Realm;
import io.realm.Sort;

/**
 * Created by Lois-9Y on 10/08/2016.
 */
public class ScannedNetworksFragment extends SwipeRefreshListFragment {
    ScannedNetworksAdapter mAdapter;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        Realm realm = Realm.getDefaultInstance();

        if (mAdapter == null) {
            mAdapter = new ScannedNetworksAdapter(getActivity(), realm.where(NetworkItem.class).findAll());
            setListAdapter(mAdapter);
            mAdapter.updateData(realm.where(NetworkItem.class)
                    .isNotNull(NetworkItem.FIELDNAME_PASSWORD)
                    .notEqualTo(NetworkItem.FIELDNAME_PASSWORD, "")
                    .findAllSorted(NetworkItem.FIELDNAME_SSID, Sort.ASCENDING));
        }


        setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initiateRefresh();
            }
        });


        //Log.d(LOG_TAG,"onCreate done")
    }
    private void initiateRefresh() {
        //Lets see what TODO here
        setRefreshing(false);
    }

}
