package com.backgroundaudio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;

import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by dmitry on 02.10.18.
 * Edited by jagrajsinghji on 28.12.19
 */

public class AudioPlayer extends Service implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, AudioManager.OnAudioFocusChangeListener {

    public static final String SERVICE_EVENT = "AudioPlayerSericeEvent";
    public static final String ACTION_PLAY = "action.PLAY";
    public static final String ACTION_TOGGLE = "action.TOGGLE";
    public static final String ACTION_STOP = "action.STOP";
    public static final String ACTION_SELECT = "action.SELECT";
    public static final String ACTION_NEXT = "action.NEXT";
    public static final String ACTION_PREV = "action.PREV";
    private static final String CHANNEL = "media_notification";

    private static NotificationManager mNM;
    private static MediaPlayer player;
    private static AudioManager audioManager;


    public static boolean play = false;
    public static boolean prepared = false;

    private static List<Map<String, String>> songs = new ArrayList<>();
    private static Map<String, String> metadata = new HashMap<>();
    public static int index = 0;

    public static List<Map> customOptions = new ArrayList<>();

    public static boolean repeat = false;
    public static boolean shuffle = false;

    @Override
    public void onCreate() {
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnErrorListener(this);
        player.setOnCompletionListener(this);

        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
        showNotification();
    }

    public static void setCustomOption(HashMap option) {
        for (Map map : AudioPlayer.customOptions) {
            if (map.get("name").equals(option.get("name"))) {
                map.clear();
                map.put("name", option.get("name"));
                map.put("value", option.get("value"));
                return;
            }
        }
        customOptions.add(option);
    }

    public static void setPlaylist(HashMap p) {
        if (p != null) {
            songs.clear();
            metadata.clear();
            songs = (List<Map<String, String>>) p.get("songs");
            metadata = (Map<String, String>) p.get("metadata");
        }
    }

    @Nullable
    public static Map getPlaylist() {
        if (player == null || songs.size() == 0) {
            return null;
        }

        Map playlist = new HashMap<String, String>();
        playlist.put("songs", songs);
        playlist.put("metadata", metadata);

        playlist.put("index", index);
        playlist.put("playing", play);

        return playlist;
    }

    public static Map<String, String> getSong() {
        return songs.isEmpty() || songs.size() <= 0 ? new HashMap() : songs.get(index);
    }

    public static int getPosition() {
        if (player != null && prepared) {
            return player.getCurrentPosition() / 1000;
        }
        return 0;
    }

    public static int getDuration() {
        if (player != null && prepared) {
            return player.getDuration() / 1000;
        }
        return 0;
    }

    public static void seekTo(int sec) {
        if (player != null) {
            player.seekTo(sec * 1000);
        }
    }

    private void callEvent(String name) {
        Intent intent = new Intent(SERVICE_EVENT);
        intent.putExtra("name", name);
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        assert action != null;
        switch (action) {
            case ACTION_PLAY:
                index = intent.getIntExtra("index", 0);
                play();
                callEvent("play");
                break;
            case ACTION_TOGGLE:
                toggle();
                break;
            case ACTION_SELECT:
                callEvent("select");
                break;
            case ACTION_NEXT:
                index = index == songs.size() - 1 ? 0 : index + 1;
                play();
                callEvent("next");
                break;
            case ACTION_PREV:
                index = index == 0 ? songs.size() - 1 : index - 1;
                play();
                callEvent("prev");
                break;
            case ACTION_STOP:
                mNM.cancel(1);
                player.stop();
                player.release();
                prepared = false;
                index = 0;
                callEvent("stop");
                stopSelf();
                break;
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        prepared = false;

        if (mp.getCurrentPosition() > 0) {
            if (!repeat) {
                if (shuffle) {
                    Random r = new Random();
                    int max = songs.size() - 1;
                    int min = 0;
                    int new_index = r.nextInt((max - min) + 1) + min;
                    if (new_index == index) {
                        new_index = new_index > 0 ? new_index - 1 : max;
                    }

                    index = new_index;
                } else {
                    index = index == songs.size() - 1 ? 0 : index + 1;
                }
                callEvent("next");
                play();
            } else {
                player.seekTo(0);
                player.start();
                callEvent("play");
            }
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        player.start();
        prepared = true;
    }

    private void play() {
        // request audio focus in case other app is playing audio
        requestAudioFocus();
        play = true;
        String source = getSong().get("source");
        if (source != null) {
            try {
                player.reset();
                player.setDataSource(source);
                player.prepareAsync();

            } catch (Exception ignored) {
            }

            showNotification();

    }
    else{
        android.util.Log.e("PLAY","NOSOURCEOFSNG");
        }}

    private void toggle() {
        play = !play;
        if (play) {
            player.start();
            callEvent("play");
        } else {
            player.pause();
            callEvent("pause");
        }
        showNotification();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL, CHANNEL, importance);
            mNM.createNotificationChannel(channel);
        }
    }

    private void showNotification() {
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_music_note)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setVibrate(new long[]{0L})
                .setSound(null);

        RemoteViews remoteView = new RemoteViews(this.getPackageName(), R.layout.notificationlayout);
        remoteView.setTextViewText(R.id.mediaTitle, getSong().get("title"));
        remoteView.setTextViewText(R.id.mediaSubtitle, getSong().get("author"));

        String icon = play ? "baseline_pause_black_48" : "baseline_play_arrow_black_48";
        remoteView.setImageViewResource(R.id.toggle, getResources().getIdentifier(icon, "drawable", this.getPackageName()));

        setNotificationListeners(remoteView);
        nBuilder.setContent(remoteView);

        Notification notification = nBuilder.build();
        startForeground(1, notification);
    }

    public void setNotificationListeners(RemoteViews view) {
        // Пауза/Воспроизведение :Play pause
        Intent intent = new Intent(this, NotificationReturnSlot.class).setAction(ACTION_TOGGLE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.toggle, pendingIntent);

        // Вперед:Forward
        Intent nextIntent = new Intent(this, NotificationReturnSlot.class).setAction(ACTION_NEXT);
        PendingIntent pendingNextIntent = PendingIntent.getBroadcast(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.next, pendingNextIntent);

        // Назад:Back
        Intent prevIntent = new Intent(this, NotificationReturnSlot.class).setAction(ACTION_PREV);
        PendingIntent pendingPrevIntent = PendingIntent.getBroadcast(this, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.prev, pendingPrevIntent);

        // Закрыть:Close
        Intent closeIntent = new Intent(this, NotificationReturnSlot.class).setAction(ACTION_STOP);
        PendingIntent pendingCloseIntent = PendingIntent.getBroadcast(this, 0, closeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.close, pendingCloseIntent);

        // Нажатие на уведомление :Click Notification
        Intent selectIntent = new Intent(this, NotificationReturnSlot.class).setAction(ACTION_SELECT);
        PendingIntent selectPendingIntent = PendingIntent.getBroadcast(this, 0, selectIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        view.setOnClickPendingIntent(R.id.layout, selectPendingIntent);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        //Invoked when the audio focus of the system is updated.
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (player.isPlaying()) player.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (player.isPlaying()) player.stop();
                player.release();
                mNM.cancel(1);
                prepared = false;
                index = 0;
                callEvent("stop");
                stopSelf();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (player.isPlaying()) {
                    play = false;
                    player.pause();
                    callEvent("pause");
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (player.isPlaying()) player.setVolume(0.1f, 0.1f);
                break;
        }
        showNotification();
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    public static class NotificationReturnSlot extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }

            if (action.equals(ACTION_SELECT)) {
                Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                context.sendBroadcast(closeDialog);
                String packageName = context.getPackageName();
                PackageManager pm = context.getPackageManager();
                Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
                context.startActivity(launchIntent);
            }

            intent = new Intent(context, AudioPlayer.class);
            intent.setAction(action);
            context.startService(intent);
        }
    }
}
