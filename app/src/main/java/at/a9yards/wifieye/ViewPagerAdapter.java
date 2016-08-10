package at.a9yards.wifieye;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by Lois-9Y on 10/08/2016.
 */
public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    CharSequence titles[];


    public ViewPagerAdapter(FragmentManager fm, CharSequence titles[]){
        super(fm);
        this.titles = titles;

    }

    @Override
    public Fragment getItem(int position) {
        return position == 0 ? new AvailableNetworksFragment(): new ScannedNetworksFragment();

    }

    @Override
    public CharSequence getPageTitle(int position) {
        return titles[position];
    }

    @Override
    public int getCount() {
        return titles.length;
    }
}
