package at.a9yards.wifieye;

import android.app.Application;
import android.support.test.espresso.Espresso;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;

import android.support.test.runner.AndroidJUnit4;
import android.test.ApplicationTestCase;
import android.widget.ImageView;


import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.realm.Realm;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.isSelected;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayWithSize;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ApplicationTest {
    @Rule
    public ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<MainActivity>(MainActivity.class);

    @Before
    public void setRealmState(){

        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.deleteAll();
        realm.copyToRealm(MockData.createNetworkItemWithPassword());
        realm.copyToRealm(MockData.createNetworkItemNoPassword());
        realm.commitTransaction();
    }
    @Test
    public void initialVisiblityTest() {
        activityTestRule.getActivity();
        onView(withText("WiFiEye")).check(matches(isDisplayed()));
        onView(withText("WiFi here")).check(matches(isDisplayed())).check(matches(isSelected()));
        onView(withText("History")).check(matches(isDisplayed()));

        //onView(allOf(withText(MockData.ssid1)),hasSibling()).check(matches(isDisplayed()));
        //onData(arrayWithSize(2)).atPosition(0).check(matches(isDisplayed()));
        onView(allOf(withText(MockData.ssid1),isDisplayed())).check(matches(hasSibling(withId(R.id.avail_net_strength))));
        onView(allOf(withText(MockData.ssid1),isDisplayed())).check(matches(hasSibling(withId(R.id.avail_net_pass_avail))));
        onView(allOf(withText(MockData.ssid2),isDisplayed())).check(matches(hasSibling(withId(R.id.avail_net_strength))));
        onView(allOf(withText(MockData.ssid2),isDisplayed())).check(matches(hasSibling(withId(R.id.avail_net_pass_avail))));
    }

    @Test
    public void historyVisiblityTest(){
        activityTestRule.getActivity();
        onView(withId(R.id.tabs_viewpager)).perform(swipeLeft());
        onView(allOf(withText(MockData.ssid1),isDisplayed())).check(matches(hasSibling(withId(R.id.history_net_delete))));
    }

    @Test
    public void selectHistoryTabClickTest() {
        activityTestRule.getActivity();
        onView(withText("History")).perform(click()).check(matches(isSelected()));
        onView(withText("WiFiEye")).check(matches(isDisplayed()));
        onView(withText("History")).check(matches(isDisplayed()));
    }

    @Test
    public void swipeTest(){
        activityTestRule.getActivity();
        onView(withId(R.id.tabs_viewpager)).perform(swipeLeft());
        onView(withText("History")).check(matches(isSelected()));
        onView(withId(R.id.tabs_viewpager)).perform(swipeRight());
        onView(withText("WiFi here")).check(matches(isSelected()));
    }


}