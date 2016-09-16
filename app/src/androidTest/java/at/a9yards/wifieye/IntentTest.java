package at.a9yards.wifieye;

import android.app.Instrumentation;
import android.content.Intent;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.realm.Realm;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.BundleMatchers.hasKey;
import static android.support.test.espresso.intent.matcher.ComponentNameMatchers.hasClassName;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/**
 * Created by Lois-9Y on 15/09/2016.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class IntentTest {
    @Rule
    public IntentsTestRule<MainActivity> activityTestRule = new IntentsTestRule<MainActivity>(MainActivity.class);


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
    public void networkWithoutPasswordClickedPositiveTest(){

        Matcher<Intent> expectedIntent = hasExtra(AvailableNetworksFragment.SSID_FOR_SCAN,MockData.ssid2);

        Intent newPassword = new Intent();
        newPassword.putExtra(AvailableNetworksFragment.PASSWORD_SCAN_RESULT,MockData.password2);
        newPassword.putExtra(AvailableNetworksFragment.SSID_FOR_SCAN,MockData.ssid2);
        intending(expectedIntent).respondWith(new Instrumentation.ActivityResult(activityTestRule.getActivity().RESULT_OK,newPassword));

        //scan new password
        onView(allOf(withChild(withText(MockData.ssid2)),isDisplayed())).perform(click());
        intended(expectedIntent);
        //snackbar
        onView(allOf(withId(android.support.design.R.id.snackbar_text),withText(containsString("Cannot edit WiFi connection: " + MockData.ssid2 )))).check(matches(isDisplayed()));
    }

    @Test
    public void networkWithoutPasswordClickedNegativeTest(){

        Matcher<Intent> expectedIntent = hasExtra(AvailableNetworksFragment.SSID_FOR_SCAN,MockData.ssid2);
        intending(expectedIntent).respondWith(new Instrumentation.ActivityResult(activityTestRule.getActivity().RESULT_CANCELED,new Intent()));

        //scan new password
        onView(allOf(withChild(withText(MockData.ssid2)),isDisplayed())).perform(click());
        intended(expectedIntent);
        //no snackbar
        onView(allOf(withId(android.support.design.R.id.snackbar_text))).check(doesNotExist());

    }
}
