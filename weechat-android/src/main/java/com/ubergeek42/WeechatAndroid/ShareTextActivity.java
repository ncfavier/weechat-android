package com.ubergeek42.WeechatAndroid;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.ubergeek42.WeechatAndroid.adapters.BufferListAdapter;
import com.ubergeek42.WeechatAndroid.relay.Buffer;
import com.ubergeek42.WeechatAndroid.service.Events;
import com.ubergeek42.WeechatAndroid.service.RelayService;

import org.greenrobot.eventbus.EventBus;

import static com.ubergeek42.WeechatAndroid.utils.Constants.*;

public class ShareTextActivity extends AppCompatActivity implements DialogInterface.OnClickListener,
        DialogInterface.OnDismissListener, DialogInterface.OnShowListener {

    BufferListAdapter bufferlistAdapter;
    AlertDialog dialog;

    @Override
    protected void onStart() {
        super.onStart();

        if (!EventBus.getDefault().getStickyEvent(Events.StateChangedEvent.class).state.contains(RelayService.STATE.LISTED)) {
            Toast.makeText(getApplicationContext(), getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Intent intent = getIntent();
        if ((Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType()))) {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.bufferlist);
            dialog.show();
            //bufferlistAdapter = new BufferListAdapter(this);
            //AlertDialog.Builder builder = new AlertDialog.Builder(this)
            //        .setAdapter(bufferlistAdapter, this)
            //        .setTitle(getString(R.string.share_text_title));
            //dialog = builder.create();
            //dialog.setOnShowListener(this);
            //dialog.setOnDismissListener(this);
            //dialog.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (dialog != null) {
            dialog.setOnDismissListener(null);      // prevents closing the activity on rotate
            dialog.dismiss();                       // prevents window leaks
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        //Buffer buffer = bufferlistAdapter.(which);
        //if (buffer != null) {
        //    final String text = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        //    Intent intent = new Intent(getApplicationContext(), WeechatActivity.class);
        //    intent.putExtra(NOTIFICATION_EXTRA_BUFFER_FULL_NAME, buffer.fullName);
        //    intent.putExtra(NOTIFICATION_EXTRA_BUFFER_INPUT_TEXT, text);
        //    startActivity(intent);
        //}
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }

    @Override
    public void onShow(DialogInterface dialog) {
        bufferlistAdapter.onBuffersChanged();
    }
}
