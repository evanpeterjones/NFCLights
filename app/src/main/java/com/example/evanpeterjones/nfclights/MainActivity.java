package com.example.evanpeterjones.nfclights;

import android.app.PendingIntent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.nfc.NfcAdapter;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Parcelable;
import android.nfc.*;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import org.json.JSONException;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private NfcAdapter nfc;
    private light lit = new light("192.168.1.101");
    private Tag tagg;
    private boolean writeMode = false;
    EditText Txt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nfc = NfcAdapter.getDefaultAdapter(this);
        Txt = findViewById(R.id.IP);

        if (nfc != null && nfc.isEnabled()) {
            Toast.makeText(this, "NFC Working", Toast.LENGTH_LONG).show();
        } if (!nfc.isEnabled()) {
            Toast.makeText(this, "NFC Not Working", Toast.LENGTH_SHORT).show();
        }
        handleIntent(getIntent());
    }

    private void handleIntent(Intent intent){
        String action = intent.getAction();
        String type = intent.getType();
        tagg = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);

        if (writeMode) {

        }
        if (intent.hasExtra(nfc.EXTRA_TAG)){
            Toast.makeText(this, "Tag Read", Toast.LENGTH_SHORT).show();
            Tag tag = intent.getParcelableExtra(nfc.toString());
            NdefMessage mess = createNDefMessage(lit.getIp());
            writeNDefMessage(tag, mess);
        }

        Parcelable[] rawMessages =
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        readTextFromMessage((NdefMessage) rawMessages[0]);

    }

    private void readTextFromMessage (NdefMessage message) {

        NdefRecord[] record = message.getRecords();

        if (record != null) {
            NdefRecord ndef = record[0];
            String content = getTextFromNdefRecord(ndef);
            Txt.setText(content);
        }
        else
        {
            Toast.makeText(this, "Could Not Read Text", Toast.LENGTH_SHORT);
        }

    }
    public String getTextFromNdefRecord(NdefRecord ndefRecord)
    {
        String tagContent = null;
        try {
            byte[] payload = ndefRecord.getPayload();
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
            int languageSize = payload[0] & 0063;
            tagContent = new String(payload, languageSize + 1,
                    payload.length - languageSize - 1, textEncoding);
        } catch (Exception e) {
            Log.e("getTextFromNdefRecord", e.getMessage());
        }
        return tagContent;
    }

    private void formatTag(Tag tag, NdefMessage message) {
        try {
            NdefFormatable format = NdefFormatable.get(tag);

            if (format == null) {
                Toast.makeText(this, "Tag Not Formatable", Toast.LENGTH_SHORT).show();
            }

            format.connect();
            format.format(message);
            format.close();
        } catch (Exception e) {
            Log.e("format error", e.getMessage());
        }
    }

    private void writeNDefMessage(Tag tag, NdefMessage mess) {
        try {

            if (tag == null) {
                Toast.makeText(this, "TAG EMPTY", Toast.LENGTH_SHORT);
            }
            Ndef ndef = Ndef.get(tag);
            if (mess == null) {
                Toast.makeText(this, "MESSAGE EMPTY", Toast.LENGTH_SHORT);

                formatTag(tag, mess);
            } else {
                ndef.connect();
                if (!ndef.isWritable()) {
                    Toast.makeText(this, "Not Writable", Toast.LENGTH_SHORT).show();
                    ndef.close();
                    Toast.makeText(this, "Tag Written", Toast.LENGTH_SHORT).show();
                }
                ndef.writeNdefMessage(mess);
                ndef.close();
                Toast.makeText(this, "Tag Written", Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e) {
            Log.e("Message not Written", e.getMessage());
        }
    }

    private NdefMessage createNDefMessage(String content) {
        NdefRecord record = NdefRecord.createTextRecord("UTF-8", content);

        NdefMessage message = new NdefMessage(new NdefRecord[]{record});

        return message;
    }

    public void writeTag(View view) throws IOException, JSONException {
        try {
            writeMode = true;
            EditText IP = findViewById(R.id.IP);
            lit.setIp(IP.toString());
            writeNDefMessage(tagg, createNDefMessage(IP.toString()));
            Toast.makeText(this, "Writing: Hover over tag", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e) {
            Log.e("Error", e.getMessage());
        }
    }

    public class Network extends AsyncTask {
        protected Bitmap doInBackground(int a) throws JSONException, IOException{
            if (a == 0) {
                lit.switchOff();
            } else {
                lit.switchOn();
            }
        }
    }

    public void toggle(View view) throws IOException, JSONException{
        lit.setIp(Txt.getText().toString());
        //Toast.makeText(this, lit.getIp().toString(), Toast.LENGTH_LONG).show();
        try {
            if (!lit.isOn()) { new Network().execute(0); }
            else new Network().execute(1);
            Toast.makeText(this, "light at: " +lit.getIp()+ " Toggled",
                    Toast.LENGTH_SHORT).show();
        }
        catch (Exception e) {
            Toast.makeText(this, "Light Connection Failure: " + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void enableForegroundDispatchSystem() {
        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                intent, 0);

        IntentFilter[] intentFilters = new IntentFilter[]{};

        nfc.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
    }

    private void disableForegroundDispatchSystem() {
        nfc.disableForegroundDispatch(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableForegroundDispatchSystem();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableForegroundDispatchSystem();
    }
}