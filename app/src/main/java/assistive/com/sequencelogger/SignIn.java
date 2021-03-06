package assistive.com.sequencelogger;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import assistive.com.sequencelogger.data.AppDetails;
import assistive.com.sequencelogger.data.ScreenDetails;
import assistive.com.sequencelogger.data.User;

public class SignIn extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private final String TAG = SignIn.class.getSimpleName();

    // Choose an arbitrary request code value
    private static final int RC_SIGN_IN = 1223;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());

                    SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                    String uid = sharedPref.getString(getString(R.string.uid), "");
                    if(!uid.equals(user.getUid())){
                        registerUser(user.getUid());
                    }
                    finish();

                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setProviders(Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                            new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                                    .build(),
                            RC_SIGN_IN);           }
                // ...
            }
        };
    }

    @Override
    public void onStart(){
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop(){
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // RC_SIGN_IN is the request code you passed into startActivityForResult(...) when starting the sign in flow.

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            // Successfully signed in
            if (resultCode == ResultCodes.OK) {
                registerUser(FirebaseAuth.getInstance().getCurrentUser().getUid());

                IdpResponse idpResponse = IdpResponse.fromResultIntent(data);
                /*startActivity(new Intent(this, Main2Activity.class)
                        .putExtra("my_token", idpResponse.getIdpToken()));*/

               // startActivity(new Intent(getApplicationContext(), SignIn.class));
               // finish();


                return;
            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    showSnackbar(R.string.sign_in_cancelled);
                    return;
                }

                if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                    showSnackbar(R.string.no_internet_connection);
                    return;
                }

                if (response.getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                    showSnackbar(R.string.unknown_error);
                    return;
                }
            }

            showSnackbar(R.string.unknown_sign_in_response);
        }
    }

    public void showSnackbar(int id){

    }

    public void registerUser(String uid) {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.uid), uid);
        editor.commit();

        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String product = Build.PRODUCT;
        int sdk = Build.VERSION.SDK_INT;
        ArrayList<AppDetails> apps = installedApplications();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        ScreenDetails screenDetails = new ScreenDetails(displaymetrics.widthPixels,displaymetrics.heightPixels,displaymetrics.density,displaymetrics.densityDpi, displaymetrics.scaledDensity);
        User userData = new User(uid,manufacturer,model, product,sdk,apps, screenDetails);
        FirebaseSingleton.registerUser(userData);

    }



    private ArrayList<AppDetails> installedApplications(){
        final PackageManager pm = getPackageManager();
        //get a list of installed apps.
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        ArrayList <AppDetails> apps = new ArrayList<AppDetails>();
        for (ApplicationInfo packageInfo : packages) {
            PackageInfo pInfo = null;
            try {
                pInfo = getPackageManager().getPackageInfo(packageInfo.packageName, 0);
                apps.add(new AppDetails(packageInfo.packageName,packageInfo.sourceDir,pInfo.versionName,pInfo.versionCode ));

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return apps;
    }

}
