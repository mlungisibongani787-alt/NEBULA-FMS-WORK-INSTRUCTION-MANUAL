package com.rfidsystem.bridge;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.pm.PackageManager;
import android.widget.*;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.*;
import okhttp3.*;
import com.zebra.rfid.api3.*;

/**
 * Production hardware adapter for the Zebra RFD8500.
 * The Zebra API3 AARs are intentionally not bundled with this repository.
 */
public class MainActivity extends Activity implements Readers.RFIDReaderEventHandler, RfidEventsListener {
    private Readers readers;
    private RFIDReader reader;
    private OkHttpClient http = new OkHttpClient();
    private WebSocket socket;
    private TextView status;
    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private String bridgeUrl = "ws://192.168.1.100:8787";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        requestBluetoothPermissions();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 28, 28, 28);
        TextView title = new TextView(this); title.setText("RFID SYSTEM — RFD8500 Bridge"); title.setTextSize(20);
        status = new TextView(this); status.setText("Starting…");
        Button connect = new Button(this); connect.setText("Connect RFD8500");
        Button inventory = new Button(this); inventory.setText("Start RFID Inventory");
        Button stop = new Button(this); stop.setText("Stop Inventory");
        root.addView(title); root.addView(status); root.addView(connect); root.addView(inventory); root.addView(stop);
        setContentView(root);
        connect.setOnClickListener(v -> connectReader());
        inventory.setOnClickListener(v -> startInventory());
        stop.setOnClickListener(v -> stopInventory());
        connectBridge();
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, 20);
        }
    }

    private void connectBridge() {
        Request request = new Request.Builder().url(bridgeUrl).build();
        socket = http.newWebSocket(request, new WebSocketListener() {
            @Override public void onOpen(WebSocket webSocket, Response response) {
                send(new JSONObject().put("type", "status").put("state", "online").put("message", "Android RFID bridge connected").toString());
            }
            @Override public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                runOnUiThread(() -> status.setText("Bridge error: " + t.getMessage()));
            }
            @Override public void onMessage(WebSocket webSocket, String text) { handleCommand(text); }
        });
    }

    private void send(String message) { if (socket != null) socket.send(message); }

    private void connectReader() {
        bg.execute(() -> {
            try {
                if (readers == null) {
                    readers = new Readers(this, ENUM_TRANSPORT.BLUETOOTH);
                    Readers.attach(this);
                }
                ArrayList<ReaderDevice> list = readers.GetAvailableRFIDReaderList();
                if (list == null || list.isEmpty()) {
                    runOnUiThread(() -> status.setText("No paired Zebra RFID reader found."));
                    send(new JSONObject().put("type","status").put("state","error").put("message","No paired RFD8500 reader found").toString());
                    return;
                }
                ReaderDevice device = list.get(0);
                reader = device.getRFIDReader();
                reader.Events.addEventsListener(this);
                reader.connect();
                if (!reader.isConnected()) throw new IllegalStateException("Reader did not report connected");
                runOnUiThread(() -> status.setText("Connected: " + device.getName()));
                publishReader(device);
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Reader error: " + e.getMessage()));
                send(new JSONObject().put("type","status").put("state","error").put("message","Reader connection failed: " + safe(e.getMessage())).toString());
            }
        });
    }

    private void publishReader(ReaderDevice device) {
        try {
            JSONObject r = new JSONObject().put("name", safe(device.getName())).put("model", "RFD8500").put("connected", true);
            int[] power = reader.ReaderCapabilities.getTransmitPowerLevelValues();
            r.put("powerLevels", power == null ? new org.json.JSONArray() : new org.json.JSONArray(power));
            r.put("tagLocationing", reader.ReaderCapabilities.isTagLocationingSupported());
            r.put("antennas", reader.ReaderCapabilities.getNumAntennaSupported());
            send(new JSONObject().put("type","reader-status").put("readers", new org.json.JSONArray().put(r)).toString());
        } catch (Exception e) {
            send(new JSONObject().put("type","reader-status").put("readers", new org.json.JSONArray().put(new JSONObject().put("name",safe(device.getName())).put("model","RFD8500").put("connected",true))).toString());
        }
    }

    private void startInventory() {
        bg.execute(() -> {
            try {
                ensureConnected();
                reader.Events.setTagReadEvent(true);
                reader.Events.setAttachTagDataWithReadEvent(false);
                reader.Actions.Inventory.perform();
                send(new JSONObject().put("type","status").put("state","online").put("message","RFD8500 inventory started").toString());
            } catch (Exception e) { send(new JSONObject().put("type","status").put("state","error").put("message","Inventory start failed: "+safe(e.getMessage())).toString()); }
        });
    }

    private void stopInventory() {
        bg.execute(() -> {
            try { if (reader != null && reader.isConnected()) reader.Actions.Inventory.stop(); }
            catch (Exception ignored) {}
            send(new JSONObject().put("type","status").put("state","online").put("message","RFD8500 inventory stopped").toString());
        });
    }

    private void locate(String epc) {
        bg.execute(() -> {
            try {
                ensureConnected();
                if (!reader.ReaderCapabilities.isTagLocationingSupported()) throw new IllegalStateException("RFD8500 reports tag locationing unsupported");
                reader.Actions.TagLocationing.Perform(epc, null);
                send(new JSONObject().put("type","status").put("state","online").put("message","Tag locationing started for "+epc).toString());
            } catch (Exception e) { send(new JSONObject().put("type","status").put("state","error").put("message","Locate failed: "+safe(e.getMessage())).toString()); }
        });
    }

    private void stopLocate() {
        bg.execute(() -> { try { if (reader != null && reader.isConnected()) reader.Actions.TagLocationing.Stop(); } catch (Exception ignored) {} });
    }

    private void setPower(int requestedDbm100) {
        bg.execute(() -> {
            try {
                ensureConnected();
                int[] supported = reader.ReaderCapabilities.getTransmitPowerLevelValues();
                if (supported == null || supported.length == 0) throw new IllegalStateException("Reader did not return supported power levels");
                int chosen = supported[0];
                int best = Integer.MAX_VALUE;
                for (int value : supported) { int diff=Math.abs(value-requestedDbm100); if(diff<best){best=diff;chosen=value;} }
                Antennas.AntennaRfConfig cfg=reader.Config.Antennas.getAntennaRfConfig(1);
                cfg.setTransmitPowerIndex(chosen);
                reader.Config.Antennas.setAntennaRfConfig(1,cfg);
                send(new JSONObject().put("type","status").put("state","online").put("message","RF power set to supported level "+chosen).toString());
            } catch(Exception e) { send(new JSONObject().put("type","status").put("state","error").put("message","RF power update failed: "+safe(e.getMessage())).toString()); }
        });
    }

    private void setBeeper(String level) {
        bg.execute(() -> { try {
            ensureConnected(); BEEPER_VOLUME volume;
            switch(level.toUpperCase(Locale.US)) { case "HIGH": volume=BEEPER_VOLUME.HIGH_BEEP; break; case "LOW": volume=BEEPER_VOLUME.LOW_BEEP; break; case "QUIET": volume=BEEPER_VOLUME.QUIET_BEEP; break; default: volume=BEEPER_VOLUME.MEDIUM_BEEP; }
            reader.Config.setBeeperVolume(volume);
            send(new JSONObject().put("type","status").put("state","online").put("message","Beeper set to "+level).toString());
        } catch(Exception e) { send(new JSONObject().put("type","status").put("state","error").put("message","Beeper update failed: "+safe(e.getMessage())).toString()); } });
    }

    private void handleCommand(String text) {
        try {
            JSONObject root = new JSONObject(text); if (!"command".equals(root.optString("type"))) return;
            String command=root.optString("command"); JSONObject p=root.optJSONObject("payload");
            if ("inventoryStart".equals(command)) startInventory();
            else if ("inventoryStop".equals(command)) stopInventory();
            else if ("locate".equals(command) && p!=null) locate(p.optString("epc"));
            else if ("stopLocate".equals(command)) stopLocate();
            else if ("setPower".equals(command) && p!=null) setPower(p.optInt("power",2200));
            else if ("setBeeper".equals(command) && p!=null) setBeeper(p.optString("level","MEDIUM"));
            else if ("listReaders".equals(command)) connectReader();
        } catch(Exception e) { send(new JSONObject().put("type","status").put("state","error").put("message","Command error: "+safe(e.getMessage())).toString()); }
    }

    private void ensureConnected() throws Exception { if(reader==null || !reader.isConnected()) { connectReader(); throw new IllegalStateException("Reader is not connected yet"); } }

    @Override public void eventReadNotify(RfidReadEvents e) {
        try {
            TagData[] tags=reader.Actions.getReadTags(100); if(tags==null)return;
            for(TagData t:tags){
                JSONObject tag=new JSONObject().put("epc",safe(t.getTagID())).put("rssi",t.getPeakRSSI()).put("reads",t.getTagSeenCount());
                JSONObject message=new JSONObject().put("type","tag").put("reader","RFD8500").put("tag",tag);
                if(t.isContainsLocationInfo()) message.put("location",true).put("relativeDistance",t.LocationInfo.getRelativeDistance());
                send(message.toString());
            }
        } catch(Exception ignored) {}
    }

    @Override public void eventStatusNotify(RfidStatusEvents e) { send(new JSONObject().put("type","status").put("state","online").put("message","RFID status event").toString()); }
    @Override public void readerAvailable(String readerName) { send(new JSONObject().put("type","status").put("state","online").put("message","Reader available: "+safe(readerName)).toString()); }
    @Override public void readerDisappeared(String readerName) { send(new JSONObject().put("type","status").put("state","offline").put("message","Reader disappeared: "+safe(readerName)).toString()); }

    private String safe(String s){return s==null?"":s.replace("\"","");}
    @Override protected void onDestroy(){try{if(reader!=null&&reader.isConnected())reader.disconnect();}catch(Exception ignored){}try{if(readers!=null)readers.Dispose();}catch(Exception ignored){}bg.shutdownNow();http.dispatcher().executorService().shutdown();super.onDestroy();}
}
