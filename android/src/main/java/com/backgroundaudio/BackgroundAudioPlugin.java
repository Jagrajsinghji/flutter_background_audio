package com.backgroundaudio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static com.backgroundaudio.AudioPlayer.SERVICE_EVENT;

/** BackgroundAudioPlugin */
public class BackgroundAudioPlugin implements MethodCallHandler, StreamHandler {
    private static String TAG = "DEBUG";
    private Registrar registrar;
    private BroadcastReceiver eventReceiver;

    private BackgroundAudioPlugin(Registrar r) {
        registrar = r;
    }

    public static void registerWith(Registrar registrar) {
        BackgroundAudioPlugin plugin = new BackgroundAudioPlugin(registrar);

        final MethodChannel methodChannel = new MethodChannel(registrar.messenger(), "method_channel");
        methodChannel.setMethodCallHandler(plugin);

        final EventChannel eventChannel = new EventChannel(registrar.messenger(), "event_channel");
        eventChannel.setStreamHandler(plugin);
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        eventReceiver = createEventReceiver(events);
        registrar.context().registerReceiver(eventReceiver, new IntentFilter(SERVICE_EVENT));
    }

    @Override
    public void onCancel(Object arguments) {
        registrar.context().unregisterReceiver(eventReceiver);
        eventReceiver = null;
    }

    private BroadcastReceiver createEventReceiver(final EventChannel.EventSink events) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                events.success(intent.getStringExtra("name"));
            }
        };
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        Intent intent;

        switch (call.method) {
            case "play":
                result.success(true);
                int index = call.argument("index");

                intent = new Intent(registrar.context(), AudioPlayer.class);
                intent.setAction(AudioPlayer.ACTION_PLAY);
                intent.putExtra("index", index);
                registrar.context().startService(intent);
                break;
            case "next":
                intent = new Intent(registrar.context(), AudioPlayer.class);
                intent.setAction(AudioPlayer.ACTION_NEXT);
                registrar.context().startService(intent);
                break;
            case "prev":
                intent = new Intent(registrar.context(), AudioPlayer.class);
                intent.setAction(AudioPlayer.ACTION_PREV);
                registrar.context().startService(intent);
                break;
            case "toggle":
                intent = new Intent(registrar.context(), AudioPlayer.class);
                intent.setAction(AudioPlayer.ACTION_TOGGLE);
                registrar.context().startService(intent);
                break;
            case "stop":
                intent = new Intent(registrar.context(), AudioPlayer.class);
                intent.setAction(AudioPlayer.ACTION_STOP);
                registrar.context().startService(intent);
                break;
            case "seekTo":
              Integer sec = call.argument("sec");
              AudioPlayer.seekTo(sec);
                break;
            case "getPosition":
                result.success(AudioPlayer.getPosition());
                break;
            case "getDuration":
                result.success(AudioPlayer.getDuration());
                break;
            case "getIndex":
                result.success(AudioPlayer.index);
                break;
            case "getPlaylist":
                result.success(AudioPlayer.getPlaylist());
                break;
            case "setPlaylist":
                Object playlist = call.argument("playlist");
                AudioPlayer.setPlaylist((HashMap)playlist);
                result.success(true);
                break;
            case "toggleRepeat":
                AudioPlayer.repeat = !AudioPlayer.repeat;
                result.success(true);
                break;
            case "toggleShuffle":
                AudioPlayer.shuffle = !AudioPlayer.shuffle;
                result.success(true);
                break;
            case "getOptions":
                Map options = new HashMap<String, String>();
                options.put("repeat", AudioPlayer.repeat);
                options.put("shuffle", AudioPlayer.shuffle);
                options.put("custom", AudioPlayer.customOptions);
                result.success(options);
                break;
            case "setCustomOption":
                Object option = call.argument("option");
                AudioPlayer.setCustomOption((HashMap)option);
                result.success(true);
                break;
            case "getCustomOption":
                String name = call.argument("name");
                Object value = null;
                for (Map map: AudioPlayer.customOptions) {
                    if(map.get("name").equals(name)) {
                        value = map.get("value");
                        break;
                    }
                }
                result.success(value);
                break;
            default:
                result.notImplemented();
                break;
        }
    }
}