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
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dmitry on 02.10.18.
 */

public class AudioPlayer extends Service implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

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

    public static boolean play = false;

    private static List<Map<String, String>> songs = new ArrayList<Map<String, String>>();
    private static Map<String, String> metadata = new HashMap<String, String>();
    public static int index = 0;

    @Override
    public void onCreate() {
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnErrorListener(this);
        player.setOnCompletionListener(this);

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
        showNotification();
    }

    public static void setPlaylist(HashMap p) {
        if (p != null) {
            songs = (List<Map<String, String>>)p.get("songs");
            metadata = (Map<String, String>)p.get("metadata");
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
        return songs.get(index);
    }

    public static int getPosition() {
        if (player != null || songs.size() == 0) {
            return player.getCurrentPosition() / 1000;
        }
        return 999;
    }

    public static int getDuration() {
        if (player != null || songs.size() == 0) {
            return player.getDuration() / 1000;
        }
        return 999;
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
    public IBinder onBind(Intent intent) {return null;}

    @Override
    public boolean onUnbind(Intent intent) {return false;}

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
                index = index == songs.size()-1 ? 0 : index+1;
                play();
                callEvent("next");
                break;
            case ACTION_PREV:
                index = index == 0 ? songs.size()-1 : index-1;
                play();
                callEvent("prev");
                break;
            case ACTION_STOP:
                mNM.cancel(1);
                player.stop();
                player.release();
                songs = new ArrayList<Map<String, String>>();
                metadata = new HashMap<String, String>();
                index = 0;
                callEvent("stop");
                stopSelf();
                break;
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mp.getCurrentPosition() > 0) {
            index = index == songs.size()-1 ? 0 : index+1;
            play();
            callEvent("next");
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        player.start();
    }

    private void play() {
        play = true;

        try{
            player.reset();
            player.setDataSource(getSong().get("url"));
            player.prepareAsync();
        }
        catch (Exception ignored) {}

        showNotification();
    }

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
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setVibrate(new long[]{0L})
                .setSound(null);

        RemoteViews remoteView = new RemoteViews(this.getPackageName(), R.layout.notificationlayout);
        remoteView.setTextViewText(R.id.title, getSong().get("title"));
        remoteView.setTextViewText(R.id.author, getSong().get("author"));

        String icon = play ? "baseline_pause_black_48" : "baseline_play_arrow_black_48";
        remoteView.setImageViewResource(R.id.toggle, getResources().getIdentifier(icon,"drawable", this.getPackageName()));

        setNotificationListeners(remoteView);
        nBuilder.setContent(remoteView);

        Notification notification = nBuilder.build();
        startForeground(1, notification);
    }

    public void setNotificationListeners(RemoteViews view){
        // Пауза/Воспроизведение
        Intent intent = new Intent(this, NotificationReturnSlot.class).setAction(ACTION_TOGGLE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.toggle, pendingIntent);

        // Вперед
        Intent nextIntent = new Intent(this, NotificationReturnSlot.class).setAction(ACTION_NEXT);
        PendingIntent pendingNextIntent = PendingIntent.getBroadcast(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.next, pendingNextIntent);

        // Назад
        Intent prevIntent = new Intent(this, NotificationReturnSlot.class).setAction(ACTION_PREV);
        PendingIntent pendingPrevIntent = PendingIntent.getBroadcast(this, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.prev, pendingPrevIntent);

        // Закрыть
        Intent closeIntent = new Intent(this, NotificationReturnSlot.class).setAction(ACTION_STOP);
        PendingIntent pendingCloseIntent = PendingIntent.getBroadcast(this, 0, closeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.close, pendingCloseIntent);

        // Нажатие на уведомление
        Intent selectIntent = new Intent(this, NotificationReturnSlot.class).setAction(ACTION_SELECT);
        PendingIntent selectPendingIntent = PendingIntent.getBroadcast(this, 0, selectIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        view.setOnClickPendingIntent(R.id.layout, selectPendingIntent);
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