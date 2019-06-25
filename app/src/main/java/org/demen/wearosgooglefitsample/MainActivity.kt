package org.demen.wearosgooglefitsample

import android.content.Intent
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessActivities
import com.google.android.gms.fitness.data.Session
import com.google.android.gms.fitness.request.SessionInsertRequest
import com.google.android.gms.fitness.request.SessionReadRequest
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : WearableActivity(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    private var mGoogleApiClient: GoogleApiClient? = null
    private var mGoogleSignInAccount: GoogleSignInAccount? = null
    private var mSignInButton: SignInButton? = null
    private var mSignOutButton: Button? = null
    private var mSendFitButton: Button? = null
    private var mReadFitButton: Button? = null
    private var mUserIdToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupGoogleApiClient()

        // Enables Always-on
        setAmbientEnabled()

        mSignInButton = findViewById(R.id.sign_in_button)
        mSignOutButton = findViewById(R.id.sign_out_button)
        mSendFitButton = findViewById(R.id.send_fit)
        mReadFitButton = findViewById(R.id.read_fit)

        mSignInButton?.setOnClickListener { signIn() }
        mSignOutButton?.setOnClickListener { signOut() }
        mSendFitButton?.setOnClickListener {
            if (!GoogleSignIn.hasPermissions(mGoogleSignInAccount, Fitness.SCOPE_ACTIVITY_READ_WRITE)) {
                GoogleSignIn.requestPermissions(
                    this, // your activity
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    mGoogleSignInAccount,
                    Fitness.SCOPE_ACTIVITY_READ_WRITE
                )
            } else {
                sendFit(mGoogleSignInAccount!!)
            }
        }
        mReadFitButton?.setOnClickListener {
            if (!GoogleSignIn.hasPermissions(mGoogleSignInAccount, Fitness.SCOPE_ACTIVITY_READ_WRITE)) {
                GoogleSignIn.requestPermissions(
                    this, // your activity
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    mGoogleSignInAccount,
                    Fitness.SCOPE_ACTIVITY_READ_WRITE
                )
            } else {
                readFit()
            }
        }
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
        } else if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            Toast.makeText(this, "Request Google Fit Permission successful!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Configures the GoogleApiClient used for sign in. Requests scopes profile and email.
     */
    private fun setupGoogleApiClient() {
        val gso = GoogleSignInOptions.Builder()
            .requestEmail()
            .requestScopes(Fitness.SCOPE_ACTIVITY_READ_WRITE)
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
                mSendFitButton?.visibility = View.VISIBLE
                mReadFitButton?.visibility = View.VISIBLE
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

        val signInOptions = GoogleSignInOptions.Builder()
            .requestScopes(Fitness.SCOPE_ACTIVITY_READ_WRITE)
            .build()
        GoogleSignIn.getClient(this, signInOptions).revokeAccess()
            .addOnSuccessListener {
                Log.d(TAG, "disableFit onSuccess")
                mSendFitButton?.visibility = View.GONE
                mReadFitButton?.visibility = View.GONE
                Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(this)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "disableFit onFailure", e)
                mSendFitButton?.visibility = View.GONE
                mReadFitButton?.visibility = View.GONE
                Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(this)
            }
    }

    private fun sendFit(account: GoogleSignInAccount) {
        Log.d(TAG, "sendFit")

        val cal = Calendar.getInstance()
        cal.time = Date()
        val endTime = cal.timeInMillis
        cal.add(Calendar.DATE, -1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        // Create the sleep session
        val session = Session.Builder()
            .setIdentifier("id_$startTime")
            .setName("session_name")
            .setDescription("session for test")
            .setStartTime(startTime, TimeUnit.MILLISECONDS)
            .setEndTime(endTime, TimeUnit.MILLISECONDS)
            .setActivity(FitnessActivities.SLEEP)
            .build()

        // Build the request to insert the session.
        val request = SessionInsertRequest.Builder()
            .setSession(session)
            .build()

        // Insert the session into Fit platform
        Log.i(TAG, "Inserting the session in the Sessions API")
        Fitness.getSessionsClient(this, account)
            .insertSession(request)
            .addOnSuccessListener {
                Log.i(TAG, "Session insert was successful!")
            }
            .addOnFailureListener { e ->
                Log.i(TAG, "There was a problem inserting the session: ${e.localizedMessage}")
            }
    }

    private fun readFit() {
        Log.d(TAG, "readFit")

        val cal = Calendar.getInstance()
        cal.time = Date()
        val endTime = cal.timeInMillis
        cal.add(Calendar.DATE, -1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis


        // Build a session read request
        val readRequest = SessionReadRequest.Builder()
            .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
            .readSessionsFromAllApps()
            .build()

        Fitness.getSessionsClient(this, mGoogleSignInAccount!!)
            .readSession(readRequest)
            .addOnSuccessListener { sessionReadResponse ->
                Log.d(TAG, "readSession onSuccess: ${sessionReadResponse.sessions}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "readSession onFailure", e)
            }
            .addOnCompleteListener {
                Log.d(TAG, "readSession onComplete")
            }

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
        const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 8002
    }
}
