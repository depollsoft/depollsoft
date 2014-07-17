package depollsoft.pitchperfect;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

import bolts.Capture;
import bolts.Continuation;
import bolts.Task;
import depollsoft.lib.activity.RichApplication;
import depollsoft.pitchperfect.lib.Accidental;
import depollsoft.pitchperfect.lib.Note;

public class PitchPerfectApplication extends RichApplication {
  @Override
  public void onCreate() {
    super.onCreate();
    Note.setPlayer(new Note.NotePlayer() {
      @Override
      public void play(final Note n) {
        getApiClientAsync(PitchPerfectApplication.this)
            .onSuccessTask(new Continuation<GoogleApiClient, Task<Void>>() {
              @Override
              public Task<Void> then(Task<GoogleApiClient> googleApiClientTask) throws Exception {
                return sendMessageAsync(PitchPerfectApplication.this,
                    getUriForNote(n, true).toString());
              }
            });
      }

      @Override
      public void stop(final Note n) {
        getApiClientAsync(PitchPerfectApplication.this)
            .onSuccessTask(new Continuation<GoogleApiClient, Task<Void>>() {
              @Override
              public Task<Void> then(Task<GoogleApiClient> googleApiClientTask) throws Exception {
                return sendMessageAsync(PitchPerfectApplication.this,
                    getUriForNote(n, false).toString());
              }
            });
      }
    });
  }

  private static Uri getUriForNote(Note n, boolean play) {
    Uri.Builder builder = new Uri.Builder()
        .scheme("pitchperfect")
        .authority("playNote")
        .appendQueryParameter("noteName", n.getFriendlyName());
    switch (n.getAccidental()) {
      case Flat:
        builder = builder.appendQueryParameter("accidental", "b");
        break;
      case Sharp:
        builder = builder.appendQueryParameter("accidental", "#");
        break;
    }
    builder = builder.appendQueryParameter("octave", "" + n.getOctave())
        .appendQueryParameter("play", "" + play);
    return builder.build();
  }

  private static Task<GoogleApiClient> apiClientTask;

  private static Task<GoogleApiClient> getApiClientAsync(final Context context) {
    if (apiClientTask == null || apiClientTask.isFaulted()) {
      final Task<GoogleApiClient>.TaskCompletionSource apiClientTcs = Task.create();
      apiClientTask = apiClientTcs.getTask();

      final Capture<GoogleApiClient> apiClient = new Capture<GoogleApiClient>();

      apiClient.set(new GoogleApiClient.Builder(context, new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
          apiClientTcs.trySetResult(apiClient.get());
        }

        @Override
        public void onConnectionSuspended(int i) {
          apiClientTask = null;
        }
      }, new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
          apiClientTcs.setError(new Exception("Failed to connect: " + connectionResult));
        }
      }).addApi(Wearable.API).build());
      apiClient.get().connect();
    }
    return apiClientTask;
  }

  public static Task<Void> sendMessageAsync(Context context, final String path) {
    return getNodesAsync(context).onSuccessTask(new Continuation<List<Node>, Task<Void>>() {
      @Override
      public Task<Void> then(Task<List<Node>> listTask) throws Exception {
        for (Node n : listTask.getResult()) {
          Wearable.MessageApi.sendMessage(apiClientTask.getResult(), n.getId(), path, null);
        }
        return null;
      }
    });
  }

  public static Task<List<Node>> getNodesAsync(Context context) {
    return getApiClientAsync(context).onSuccessTask(new Continuation<GoogleApiClient, Task<NodeApi.GetConnectedNodesResult>>() {
      @Override
      public Task<NodeApi.GetConnectedNodesResult> then(Task<GoogleApiClient> googleApiClientTask) throws Exception {
        return TaskUtils.fromPendingResult(Wearable.NodeApi.getConnectedNodes(googleApiClientTask.getResult()));
      }
    }).onSuccess(new Continuation<NodeApi.GetConnectedNodesResult, List<Node>>() {
      @Override
      public List<Node> then(Task<NodeApi.GetConnectedNodesResult> getConnectedNodesResultTask) throws Exception {
        return getConnectedNodesResultTask.getResult().getNodes();
      }
    });
  }
}
