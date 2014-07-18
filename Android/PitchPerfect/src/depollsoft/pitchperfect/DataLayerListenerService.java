package depollsoft.pitchperfect;

import android.content.Intent;
import android.net.Uri;

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;

public class DataLayerListenerService extends WearableListenerService {
  public static final Uri LOCATION_DATA_URI = Uri.parse("/location_data");

  public static DataMap currentData;

  @Override
  public void onDataChanged(DataEventBuffer dataEvents) {
    super.onDataChanged(dataEvents);
  }

  @Override
  public void onMessageReceived(MessageEvent messageEvent) {
    super.onMessageReceived(messageEvent);
    Uri uri = Uri.parse(messageEvent.getPath());
    Intent serviceIntent = new Intent(this, PitchPerfectService.class);
    serviceIntent.putExtra("noteName", uri.getQueryParameter("noteName"));
    serviceIntent.putExtra("accidental", uri.getQueryParameter("accidental"));
    if (uri.getQueryParameter("octave") != null) {
      serviceIntent.putExtra("octave", Integer.parseInt(uri.getQueryParameter("octave")));
    }
    serviceIntent.putExtra("play", Boolean.parseBoolean(uri.getQueryParameter("play")));
    startService(serviceIntent);
  }

  @Override
  public void onPeerConnected(Node peer) {
    super.onPeerConnected(peer);
  }

  @Override
  public void onPeerDisconnected(Node peer) {
    super.onPeerDisconnected(peer);
  }
}