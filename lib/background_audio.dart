import 'dart:async';
import 'package:flutter/services.dart';

const MethodChannel _methodChannel = const MethodChannel('method_channel');
const EventChannel _eventChannel = const EventChannel('event_channel');

class BackgroundAudioPlaylist {
  List<Map<String, dynamic>> songs = [];
  Map<String, dynamic> metadata;

  BackgroundAudioPlaylist({this.songs, this.metadata});

  factory BackgroundAudioPlaylist.fromJson(Map data) {
    return BackgroundAudioPlaylist(
      songs: data['songs'].map<Map<String, dynamic>>((song) {
        Map<String, dynamic> map = Map.from(song);
        return map;
      }).toList(),
      metadata: Map.from(data['metadata'])
    );
  }

  Map<String, dynamic> toMap() {
    return {"songs": this.songs, "metadata": this.metadata};
  }
}

class BackgroundAudio {
  static bool playing = false;
  static int duration = 0;
  static int position = 0;
  static Map<String, Function> _listeners = new Map();
  static BackgroundAudioPlaylist playlist;
  static int index;
  static bool repeat = false;
  static bool shuffle = false;

  static Map<String, dynamic> get song {
    if (playlist == null || playlist.songs.length == 0) {
      return null;
    }
    return playlist.songs[index];
  }

  static init() async {
    _methodChannel.invokeMethod('getOptions').then((options) {
      print(options);
      repeat = options['repeat'];
      shuffle = options['shuffle'];
    });

    Map data = await _methodChannel.invokeMethod('getPlaylist');
   
    if (data != null) {
      index = data["index"] as int;
      playing = data["playing"] as bool;
      playlist = BackgroundAudioPlaylist.fromJson(data);
    }

    _eventChannel.receiveBroadcastStream().listen(_onEvent);

    Timer.periodic(new Duration(milliseconds: 500), (Timer timer) async {
      if (playlist == null || !playing) {
        return;
      }
      position = await _methodChannel.invokeMethod('getPosition');
      _callEvent('position', data: position);

      if (duration <= 0) {
        _callEvent('duration', data: position);
        _updateDuration();
      }
    });
  }

  static _updateDuration() {
    _methodChannel.invokeMethod('getDuration').then((sec) {
      duration = sec;
      _callEvent('duration', data: sec);
    });
  }

  static setPlaylist(BackgroundAudioPlaylist p) async {
    if (BackgroundAudio.playlist != p) {
      BackgroundAudio.playlist = p;
      await _methodChannel.invokeMethod('setPlaylist', {"playlist": p.toMap()});
    }
  }

  static toggleRepeat() async {
    repeat = !repeat;
    await _methodChannel.invokeMethod('toggleRepeat');
  }

  static toggleShuffle() async {
    shuffle = !shuffle;
    await _methodChannel.invokeMethod('toggleShuffle');
  }

  static getOptions() async {
    var options = await _methodChannel.invokeMethod('getOptions');
    return options;
  }

  static setCustomOption(name, value) async {
    await _methodChannel.invokeMethod('setCustomOption', {"option": {"name": name, "value": value}});
  }

  static getCustomOption(name) async {
    var value = await _methodChannel.invokeMethod('getCustomOption', {"name": name});
    return value;
  }

  static play(int i) async {
    index = i;
    await _methodChannel.invokeMethod('play', {"index": i});

    _updateDuration();
  }

  static toggle() {
    playing = false;
    _methodChannel.invokeMethod('toggle');
  }

  static stop() {
    playing = false;
    _methodChannel.invokeMethod('stop');
  }

  static seekTo(int sec) {
    position = sec;
    _methodChannel.invokeMethod('seekTo', {"sec": sec});
    _callEvent('position', data: sec);
  }

  static next() {
    _methodChannel.invokeMethod('next');
  }

  static prev() {
    _methodChannel.invokeMethod('prev');
  }

  static onDuration(Function(int duration) callback) {
    print('BackgroundAudio: Added listener "onDuration"\n');
    _listeners.addAll({"duration": callback});
  }

  static onPosition(Function(int duration) callback) {
    print('BackgroundAudio: Added listener "onPosition"\n');
    _listeners.addAll({"position": callback});
  }

  static onTogglePlayback(Function(bool playing) callback) {
    print('BackgroundAudio: Added listener "onTogglePlayback"\n');
    _listeners.addAll({"toggle_playback": callback});
  }

  static onStop(Function callback) {
    print('BackgroundAudio: Added listener "onStop"\n');
    playing = false;
    _listeners.addAll({"stop": callback});
  }

  static onSelect(Function callback) {
    print('BackgroundAudio: Added listener "onSelect"\n');
    _listeners.addAll({"select": callback});
  }
  
  static onNext(Function callback) {
    print('BackgroundAudio: Added listener "onNext"\n');
    _listeners.addAll({"next": callback});
  }
  
  static onPrev(Function callback) {
    print('BackgroundAudio: Added listener "onPrev"\n');
    _listeners.addAll({"prev": callback});
  }

  static _onEvent(dynamic name) async {
    dynamic data;
    print('BackgroundAudio: Event "$name"\n');

    if (name == "next" || name == "prev") {
      position = 0;
      duration = 0;
      playing = true;
    }

    if (name == "next" || name == "prev") {
      index = await _methodChannel.invokeMethod('getIndex');
    }

    if (name == "play") {
      playing = true;
      data = playing;
      name = "toggle_playback";
    }

    if (name == "pause") {
      playing = false;
      data = playing;
      name = "toggle_playback";
    }

    _callEvent(name, data: data);
  }

  static _callEvent(dynamic name, {dynamic data}) {
    _listeners.forEach((event, callback) {
      if (name == event) {
        if (name != 'position' && name != 'duration') {
          print('BackgroundAudio: Calling listeners for "$name"\n');
        }

        if (data == null) {
          callback();
        } else {
          callback(data);
        }
      }
    });
  }
}
