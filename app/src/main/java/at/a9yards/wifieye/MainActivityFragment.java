package at.a9yards.wifieye;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SlidingPaneLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.samples.apps.iosched.ui.widget.SlidingTabLayout;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    CharSequence tabNames [];

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tabNames = new CharSequence[]{getResources().getString(R.string.title_wifi_tab), getResources().getString(R.string.title_history_tab)};

        ViewPagerAdapter adapter = new ViewPagerAdapter(getActivity().getSupportFragmentManager(),tabNames);

        ViewPager pager = (ViewPager) getActivity().findViewById(R.id.tabs_viewpager);
        pager.setAdapter(adapter);

        SlidingTabLayout tabs = (SlidingTabLayout) getActivity().findViewById(R.id.tabs);
        tabs.setDistributeEvenly(true);
        tabs.setViewPager(pager);

        tabs.setCustomTabColorizer( new SlidingTabLayout.TabColorizer(){
            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.accent);
            }

        });

    }
}
