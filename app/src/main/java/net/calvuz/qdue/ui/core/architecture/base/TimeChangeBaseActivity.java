package net.calvuz.qdue.ui.core.architecture.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import net.calvuz.qdue.ui.core.common.interfaces.NotifyUpdatesInterface;
import net.calvuz.qdue.ui.core.common.utils.Library;
import net.calvuz.qdue.ui.core.common.utils.Log;
import net.calvuz.qdue.ui.core.common.utils.TimeChangeReceiver;

import java.util.List;

public abstract class TimeChangeBaseActivity
        extends AppCompatActivity
        implements TimeChangeReceiver.TimeChangeListener
{
    // TAG
    private static final String TAG = "TimeChangeBaseActivity";

    // Time Change Receiver
    protected TimeChangeReceiver mTimeChangeReceiver;
    protected boolean mReceiverRegistered = false;

    // ===========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

        // Initialize time change receiver
        mTimeChangeReceiver = new TimeChangeReceiver( this );
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerTimeChangeReceiver();
        notifyUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterTimeChangeReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterTimeChangeReceiver();
    }

    // ===========================================================

    /**
     * Notify  update data.
     */
    public void notifyUpdates() {
        Log.d( TAG, "✅ notifyUpdates" );
        try {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            for (Fragment fragment : fragments) {

                // Check for fragment VISIBILITY
                if (fragment instanceof NotifyUpdatesInterface && fragment.isVisible()) {
                    ((NotifyUpdatesInterface) fragment).notifyUpdates();
                    Log.d( TAG, "✅ Fragment notified for changes" );
                } else {
                    Log.w( TAG, "❌ Fragment not notifiable" );
                }
            }
        } catch (Exception e) {
            Log.e( TAG, "❌ Error during updates notification", e );
        }
    }

    // ===========================================================

    /**
     * Register TimeChangeReceiver
     */
    @SuppressLint ("UnspecifiedRegisterReceiverFlag")
    private void registerTimeChangeReceiver() {
        if (!mReceiverRegistered && mTimeChangeReceiver != null) {
            try {
                IntentFilter filter = TimeChangeReceiver.createOnlyCriticalIntentFilter();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver( mTimeChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                } else {
                    registerReceiver( mTimeChangeReceiver, filter);
                }
                mReceiverRegistered = true;
                Log.d( TAG, "✅ TimeChangeReceiver registered" );
            } catch (Exception e) {
                Log.e( TAG, "❌ Error registering TimeChangeReceiver", e );
            }
        }
    }

    /**
     * Unregister TimeChangeReceiver
     */
    private void unregisterTimeChangeReceiver() {
        if (mReceiverRegistered && mTimeChangeReceiver != null) {
            try {
                unregisterReceiver( mTimeChangeReceiver );
                mReceiverRegistered = false;
                Log.d( TAG, "✅ TimeChangeReceiver unregistered" );
            } catch (Exception e) {
                Log.e( TAG, "❌ Error unregistering TimeChangeReceiver", e );
            }
        }
    }

    // ===========================================================

    /**
     * TimeChangeReceiver.TimeChangeListener implementation
     */
    @Override
    public void onTimeChanged() {
        Log.d( TAG, "✅ onTimeChanged" );
        runOnUiThread( () -> {
            //notifyUpdates();
            Library.showSuccess( this, "System Time Updated" );
        } );
    }

    /**
     * TimeChangeReceiver.TimeChangeListener implementation
     */
    @Override
    public void onDateChanged() {
        Log.d( TAG, "✅ onDateChanged" );
        runOnUiThread( () -> {
            //notifyUpdates();
            Library.showSuccess( this, "System Date Updated" );
        } );
    }

    /**
     * TimeChangeReceiver.TimeChangeListener implementation
     */
    @Override
    public void onTimezoneChanged() {
        Log.d( TAG, "✅ onTimezoneChanged" );
        runOnUiThread( () -> {
            //notifyUpdates();
            Library.showSuccess( this, "System Timezone Updated" );
        } );
    }
}
