package com.chibatching.nearbysample

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.messages.Message
import com.google.android.gms.nearby.messages.MessageListener
import com.google.android.gms.nearby.messages.Strategy
import kotlinx.android.synthetic.activity_main.*
import kotlinx.android.synthetic.item_message_list.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

public class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = "MainActivity"
        val REQUEST_RESOLVE_ERROR = 1
        val ENCODE = "UTF-8"
        val NAMESPACE = "nearby-sample"
    }

    // Nearby connection callback
    val callbacks = NearbyConnectionCallbacks()
    // Nearby connection failed listener
    val failedListener = NearbyConnectionFailedListener()
    // Google Api Client for Nearby Message
    val googleApiClient: GoogleApiClient by Delegates.lazy {
        GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(callbacks)
                .addOnConnectionFailedListener(failedListener)
                .build()
    }
    // Nearby Message strategy
    val strategy = Strategy.Builder()
            .setDiscoveryMode(Strategy.DISCOVERY_MODE_DEFAULT)
            .setDistanceType(Strategy.DISCOVERY_MODE_DEFAULT)
            .setTtlSeconds(Strategy.TTL_SECONDS_DEFAULT)
            .build()
    // Listener on message received
    val messageListener = object : MessageListener() {
        override fun onFound(message: Message?) {
            Log.d(TAG, "onFound: ${message?.toString()}")
            if (message != null) {
                addNewMessage(ChatMessage.fromJson(String(message.getContent(), ENCODE)))
            }
        }
    }

    val messageAdapter: ArrayAdapter<ChatMessage> by Delegates.lazy {
        MessageListAdapter(this, R.layout.item_message_list, messageList)
    }

    val messageList = ArrayList<ChatMessage>()

    val simpleDateFormat = SimpleDateFormat("HH:mm")

    var mResolvingError = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sendButton.setOnClickListener {
            // Make sending message when send button clicked
            val chatMessage = ChatMessage(editText.getText().toString(), System.currentTimeMillis())
            editText.setText("")
            textInputLayout.setHint(getString(R.string.hint_sending))
            if (getCurrentFocus() != null) {
                // Hide ime
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0)
            }
            // Publish message
            val message = Message(chatMessage.toString().toByteArray(ENCODE), ChatMessage.TYPE_USER_CHAT)
            Nearby.Messages.publish(googleApiClient, message, strategy)
                    .setResultCallback(NearbyResultCallback("send", {
                        // When send succeeded, show my message
                        textInputLayout.setHint(getString(R.string.hint_input))
                        addNewMessage(chatMessage)
                    }))
            Log.d(TAG, "Send message: ${message.toString()}")
            Log.d(TAG, "Send chat: ${String(message.getContent(), ENCODE)}")
        }

        listView.setAdapter(messageAdapter)
        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_NORMAL)
        listView.setStackFromBottom(true)
    }

    override fun onStart() {
        super.onStart()
        if (!googleApiClient.isConnected()) {
            googleApiClient.connect()
        }
    }

    override fun onStop() {
        if (googleApiClient.isConnected()) {
            Nearby.Messages.unsubscribe(googleApiClient, messageListener)
        }
        googleApiClient.disconnect()
        super.onStop()
    }

    // This is called in response to a button tap in the Nearby permission dialog.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Start subscribe")
                Nearby.Messages.subscribe(googleApiClient, messageListener)
            } else {
                Log.d(TAG, "Failed to resolve error with code $resultCode")
            }
        }
    }

    // Add new message to message list and update list view
    private fun addNewMessage(message: ChatMessage) {
        if (!messageList.contains(message)) {
            messageList.add(message)
            Collections.sort(messageList)
            this@MainActivity.runOnUiThread {
                messageAdapter.notifyDataSetChanged()
            }
        }
    }

    // Callbacks for Nearby Connection
    private inner class NearbyConnectionCallbacks : GoogleApiClient.ConnectionCallbacks {
        override fun onConnected(connectionHint: Bundle?) {
            Log.d(TAG, "onConnected: $connectionHint")
            Nearby.Messages.getPermissionStatus(googleApiClient)
                    .setResultCallback(NearbyResultCallback("getPermissionStatus", {
                        Log.d(TAG, "Start subscribe")
                        Nearby.Messages.subscribe(googleApiClient, messageListener)
                    }))
        }

        override fun onConnectionSuspended(p0: Int) {
        }
    }

    // Listener when Nearby Connection failed
    private class NearbyConnectionFailedListener : GoogleApiClient.OnConnectionFailedListener {
        override fun onConnectionFailed(p0: ConnectionResult?) {
            Log.d(TAG, "onConnectionFailed")
        }
    }

    // Message sending/receiving callback
    private inner class NearbyResultCallback(
            val method: String, val runOnSuccess: () -> Unit) : ResultCallback<Status> {

        override fun onResult(status: Status) {
            if (status.isSuccess()) {
                Log.d(TAG, "$method succeeded.")
                this@MainActivity.runOnUiThread { runOnSuccess() }
            } else {
                // Currently, the only resolvable error is that the device is not opted
                // in to Nearby. Starting the resolution displays an opt-in dialog.
                if (status.hasResolution()) {
                    if (!mResolvingError) {
                        try {
                            status.startResolutionForResult(this@MainActivity, REQUEST_RESOLVE_ERROR)
                            mResolvingError = true
                        } catch (e: IntentSender.SendIntentException) {
                            Toast.makeText(this@MainActivity, "$method failed with exception: " + e, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // This will be encountered on initial startup because we do
                        // both publish and subscribe together.  So having a toast while
                        // resolving dialog is in progress is confusing, so just log it.
                        Log.d(TAG, "$method failed with status: $status while resolving error.")
                    }
                } else {
                    Log.d(TAG, "$method failed with: $status resolving error: $mResolvingError")
                }
            }
        }
    }

    private inner class MessageListAdapter(context: Context, val resId: Int, array: ArrayList<ChatMessage>) :
            ArrayAdapter<ChatMessage>(context, resId, array) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            var view: View? = convertView
            if (view == null) {
                view = getLayoutInflater().inflate(resId, null, false)
            }

            val message = getItem(position)
            view?.messageText?.setText(message.text)
            view?.timestampText?.setText(simpleDateFormat.format(Date(message.timestamp)))
            return view
        }
    }
}
