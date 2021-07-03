package asr.carinae.eta.haand;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import java.util.Iterator;
import java.util.Vector;

/**
 * Created by moses gichangA on 10/6/2016.
 */
public class SmsReceiver extends BroadcastReceiver {
    Vector<SmsReceivedListener> newMessagesListeners = new Vector<SmsReceivedListener>();

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println(intent.getAction());
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            for (SmsMessage newSms : Telephony.Sms.Intents.getMessagesFromIntent(intent))
                this.fireNewMessageReceived(newSms);
        }
    }

    private void fireNewMessageReceived(SmsMessage newSms) {
        for (SmsReceivedListener newMessagesListeners : this.newMessagesListeners)
            newMessagesListeners.smsReceived(newSms);
    }

    protected void addSMSReceivedListener(SmsReceivedListener smsReceivedListener) {
        this.newMessagesListeners.add(smsReceivedListener);
    }
}

interface SmsReceivedListener {
    void smsReceived(SmsMessage smsMessage);
}