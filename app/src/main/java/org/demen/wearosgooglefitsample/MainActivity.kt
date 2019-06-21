package org.demen.wearosgooglefitsample

import android.content.Intent
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status

class MainActivity : WearableActivity(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    private var mGoogleApiClient: GoogleApiClient? = null
    private var mGoogleSignInAccount: GoogleSignInAccount? = null
    private var mSignInButton: SignInButton? = null
    private var mSignOutButton: Button? = null
    private var mUserIdToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupGoogleApiClient()

        // Enables Always-on
        setAmbientEnabled()

        mSignInButton = findViewById(R.id.sign_in_button)
        mSignOutButton = findViewById(R.id.sign_out_button)

        mSignInButton?.setOnClickListener { signIn() }
        mSignOutButton?.setOnClickListener { signOut() }
    }

    override fun onResume() {
        super.onResume()
        if (mGoogleApiClient?.isConnected == false) mGoogleApiClient?.connect()
    }

    override fun onPause() {
        if (mGoogleApiClient?.isConnected == true) mGoogleApiClient?.disconnect()
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(TAG, "Activity request code: $requestCode")
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            handleSignInResult(result)
        }
    }

    /**
     * Configures the GoogleApiClient used for sign in. Requests scopes profile and email.
     */
    private fun setupGoogleApiClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestProfile()
            .requestEmail()
            .requestIdToken(getString(R.string.server_client_id))
            .build()
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addOnConnectionFailedListener(this)
            .addConnectionCallbacks(this)
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
            .build()
    }

    private fun handleSignInResult(result: GoogleSignInResult?) {
        if (result == null) {
            Log.d(TAG, "Google Sign-In result is null")
            mSignInButton?.isEnabled = true
        }

        if (result!!.isSuccess) {
            mGoogleSignInAccount = result.signInAccount
            if (mGoogleSignInAccount != null) {
                Toast.makeText(this, "Google Sign-In successful!", Toast.LENGTH_SHORT).show()

                mUserIdToken = mGoogleSignInAccount!!.idToken
                Log.d(TAG, "Google Sign-In success $mUserIdToken")

                mSignInButton?.visibility = View.GONE
                mSignOutButton?.visibility = View.VISIBLE

            }
        } else {
            Log.d(TAG, "Google Sign-In failure: " + result.status)
            mSignInButton?.isEnabled = true
        }
    }

    /**
     * Try to silently retrieve sign-in information for a user who is already signed into the app.
     */
    private fun refreshSignIn() {
        val pendingResult = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient)
        if (pendingResult.isDone) {
            handleSignInResult(pendingResult.get())
        } else {
            pendingResult.setResultCallback { result -> handleSignInResult(result) }
        }
    }

    /**
     * Starts Google sign in activity, response handled in onActivityResult.
     */
    private fun signIn() {
        if (mGoogleApiClient == null || mGoogleApiClient?.isConnected == false) {
            Log.e(TAG, "Google API client not initialized or not connected.")
            return
        }
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    /**
     * Signs the user out and resets the sign-in button to visible.
     */
    private fun signOut() {
        if (mGoogleApiClient == null || mGoogleApiClient?.isConnected == false) {
            Log.e(TAG, "Google API client not initialized or not connected")
            return
        }
        mGoogleApiClient!!.connect()
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(this)
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.d(TAG, "Connection failed.")
        Toast.makeText(this, "Connection failed.", Toast.LENGTH_SHORT).show()
    }

    override fun onConnected(bundle: Bundle?) {
        Log.d(TAG, "onConnected()")
        refreshSignIn()
    }

    override fun onConnectionSuspended(i: Int) {
        Log.d(TAG, "onConnectionSuspended(): connection to location client suspended: $i")
    }

    override fun onResult(status: Status) {
        if (status.isSuccess) {
            Log.d(TAG, "Successfully signed out")
            Toast.makeText(this, "Successfully signed out!", Toast.LENGTH_SHORT).show()
            mSignOutButton?.visibility = View.GONE
            mSignInButton?.visibility = View.VISIBLE
        } else {
            Log.d(TAG, "Sign out not successful.")
        }
    }

    companion object {
        const val TAG = "MainActivity"
        const val RC_SIGN_IN = 8001
    }
}
