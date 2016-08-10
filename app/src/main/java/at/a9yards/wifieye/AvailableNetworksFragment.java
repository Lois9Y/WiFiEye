package at.a9yards.wifieye;

import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import at.a9yards.wifieye.data.Cheeses;

import com.example.android.swiperefreshlistfragment.SwipeRefreshListFragment;

import java.util.List;

/**
 * Created by Lois-9Y on 10/08/2016.
 */
public class AvailableNetworksFragment extends SwipeRefreshListFragment {

    private static final int DISPLAY_ITEM_COUNT = 20;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListAdapter adapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                Cheeses.randomList (DISPLAY_ITEM_COUNT));
        setListAdapter(adapter);

        setOnRefreshListener(new 	SwipeRefreshLayout.OnRefreshListener(){
            @Override
            public void onRefresh() {
                initiateRefresh();
            }
        });
    }

    private void initiateRefresh() {
        new DummyBackgroundTask().execute();
    }

    private class DummyBackgroundTask extends AsyncTask<Void, Void, List<String>> {

        static final int TASK_DURATION = 3 * 1000; // 3 seconds

        @Override
        protected List<String> doInBackground(Void... params) {
            // Sleep for a small amount of time to simulate a background-task
            try {
                Thread.sleep(TASK_DURATION);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Return a new random list of cheeses
            return Cheeses.randomList(DISPLAY_ITEM_COUNT);
        }

        @Override
        protected void onPostExecute(List<String> result) {
            super.onPostExecute(result);

            // Tell the Fragment that the refresh has completed
            onRefreshComplete(result);
        }
        private void onRefreshComplete(List<String> result) {

            // Remove all items from the ListAdapter, and then replace them with the new items
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) getListAdapter();
            adapter.clear();
            for (String cheese : result) {
                adapter.add(cheese);
            }

            // Stop the refreshing indicator
            setRefreshing(false);
        }

    }
}
