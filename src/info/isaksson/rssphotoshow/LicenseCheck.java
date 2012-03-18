package info.isaksson.rssphotoshow;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.widget.Toast;
import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.ServerManagedPolicy;

public class LicenseCheck extends Activity {
    private class MyLicenseCheckerCallback implements LicenseCheckerCallback {
        @Override
        public void allow(int reason) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            // Should allow user access.
            startMainActivity();

        }

        @Override
        public void applicationError(int errorCode) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            // This is a polite way of saying the developer made a mistake
            // while setting up or calling the license checker library.
            // Please examine the error code and fix the error.
            toast("Error when validating license: " + errorCode);
            startMainActivity();

        }

        @Override
        public void dontAllow(int reason) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }

            // Should not allow access. In most cases, the app should assume
            // the user has access unless it encounters this. If it does,
            // the app should inform the user of their unlicensed ways
            // and then either shut down the app or limit the user to a
            // restricted set of features.
            // In this example, we show a dialog that takes the user to Market.
            showDialog(reason);
        }
    }

    private static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhe6Ao9TzwOWOfAHcnYJVGTM6JnjXOGGomhbAzekJfv2H7vqohgL3yzbpn429f5X/LSKTWysXrcLQvCcRpvLPFSctNe7fkne8j7vfvXDclGoOBoZD9a39eQTqD+yBJj6pDQAsCgbdwSR6BEXF6T2kA75A/algx+W96sTbvD7ZNUBo8cLO1PcFt6XmsV689S8TYAqRKlcoMXo2nhNCy0ZV0E6X6tn9DV+TYcP8ndDS4WR4PIhQLBpxAGDUaYSe/7MXOQz3CSIklk2k85obRwHlXo44nFdg5wQXS/21V79j1qXR7xGru3aYLrjAgs1wWnxCxZ1Bbkr0/lTjhHrlVgHgHQIDAQAB";

    private static final byte[] SALT = new byte[]{12, 124, -34, 45, 87, -67, 126, 4, -98, -23, -66, 89, 45, 3, -98, -103, -111, 7, 9, 73};
    private LicenseChecker mChecker;

    // A handler on the UI thread.

    private LicenseCheckerCallback mLicenseCheckerCallback;

    private void doCheck() {

        mChecker.checkAccess(mLicenseCheckerCallback);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Try to use more data here. ANDROID_ID is a single point of attack.
        String deviceId = Secure.getString(getContentResolver(),
                Secure.ANDROID_ID);

        // Library calls this when it's done.
        mLicenseCheckerCallback = new MyLicenseCheckerCallback();
        // Construct the LicenseChecker with a policy.
        mChecker = new LicenseChecker(this, new ServerManagedPolicy(this,
                new AESObfuscator(SALT, getPackageName(), deviceId)),
                BASE64_PUBLIC_KEY);
        doCheck();

    }

    @Override
    protected Dialog onCreateDialog(int id) {
        // We have only one dialog.
        return new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.notlicensed_title))
                .setCancelable(false)
                .setMessage(
                        getResources().getString(R.string.notlicensed_description))
                .setPositiveButton(getResources().getString(R.string.notlicensed_buy),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                Intent marketIntent = new Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id="
                                                + getPackageName()));
                                startActivity(marketIntent);
                                finish();
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.notlicensed_exit),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                finish();
                            }
                        }).create();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mChecker.onDestroy();
    }

    private void startMainActivity() {
        startActivity(new Intent(this, RSSPhotoShowActivity.class));  //REPLACE MAIN.class WITH YOUR APPS ORIGINAL LAUNCH ACTIVITY
        finish();
    }

    public void toast(String string) {
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
    }

}