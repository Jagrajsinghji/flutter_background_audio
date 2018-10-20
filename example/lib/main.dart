import 'package:flutter/material.dart';
import 'package:background_audio/background_audio.dart';

void main() => runApp(new MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String status = 'stopped';
  
  List<BackgroundAudioPlaylist> playlists = [
    BackgroundAudioPlaylist(
        songs: [{
          "id": "1",
          "title": "First song",
          "author": "Unknown",
          "url": "https://youvix.org/songs/stream?song_id=671&s=867ad6332d7bc8f197e67cd34fbc7528",
        }, {
          "id": "2",
          "title": "say it",
          "author": "Blue oktober",
          "url": "https://youvix.org/songs/stream?song_id=2049&s=eca61993bdd45569ebd385cd3cc58971",
        }],
      metadata: {"id": "1", "title": "Playlist one"}
    ),
    BackgroundAudioPlaylist(
        songs: [{
          "id": "3",
          "title": "First song(2 playlist)",
          "author": "Unknown",
          "url": "https://youvix.org/songs/stream?song_id=671&s=867ad6332d7bc8f197e67cd34fbc7528",
        }, {
          "id": "4",
          "title": "New Order(2 playlist)",
          "author": "Blue Monday 88",
          "url": "https://youvix.org/songs/stream?song_id=2049&s=eca61993bdd45569ebd385cd3cc58971",
        }],
        metadata: {"id": "2", "title": "Playlist two"}
    )
  ];
  
  @override
  void initState() {
    super.initState();

    BackgroundAudio.init().then((e) {
      if (BackgroundAudio.playlist != null) {
        if (BackgroundAudio.playing) {
          setState(() => status = 'play');
        } else {
          setState(() => status = 'pause');
        }
      }
    });

    BackgroundAudio.onTogglePlayback((bool playing) {
      setState(() => status = playing ? 'play' : 'pause');
    });

    BackgroundAudio.onDuration((int duration) {
      setState(() {});
    });

    BackgroundAudio.onPosition((int position) {
      setState(() {});
    });
    
    BackgroundAudio.onNext(() {
      
    });

    BackgroundAudio.onPrev(() {
      
    });

    BackgroundAudio.onSelect(() {
      print('notification selected');
    });

    BackgroundAudio.onStop(() {
      setState(() => status = 'stopped');
    });
  }

  stop() {
    BackgroundAudio.stop();
  }

  toggle() {
    BackgroundAudio.toggle();
  }

  play(BackgroundAudioPlaylist playlist, int index) async {
    await BackgroundAudio.setPlaylist(playlist);
    BackgroundAudio.play(index);
  }

  Widget _buildPlaylist(BackgroundAudioPlaylist playlist) {
    return Container(
      width: 180.0,
      margin: EdgeInsets.only(top: 20.0),
      child: Column(
        children: <Widget>[
          Text(playlist.metadata["title"], style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16.0)),
          ListView.builder(
            shrinkWrap: true,
            itemBuilder: (context, i) => ListTile(
              title: Text(playlist.songs[i]["title"], textAlign: TextAlign.center, style: TextStyle(fontSize: 14.0),),
              onTap: () {
                play(playlist, i);
              },
            ),
            itemCount: playlist.songs.length,
          )
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {

    var player = BackgroundAudio.song == null ? Container() : Container(
      height: 240.0,
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: <Widget>[
              FlatButton(child: Icon(Icons.close), onPressed: stop),
            ],
          ),
          Divider(),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              BackgroundAudio.song == null ? Container() :
              Flexible(
                child: Column(children: <Widget>[
                  Text(BackgroundAudio.song["title"], style: TextStyle(fontSize: 16.0)),
                  Text(BackgroundAudio.song["author"]),
                  SizedBox(height: 12.0),
                  Text((BackgroundAudio.playlist.metadata["title"] ?? ""), 
                    style: TextStyle(fontSize: 16.0, color: Colors.grey),
                  ),
                ]),
              ),
            ]
          ),
          Text(BackgroundAudio.position.toString()+'/'+BackgroundAudio.duration.toString()),
           Slider(
             onChanged: (val) {
               BackgroundAudio.seekTo(val.toInt());
             },
             value: BackgroundAudio.position.toDouble(),
             max: BackgroundAudio.duration.toDouble(),
           ),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              FlatButton(child: Icon(Icons.skip_previous, size: 50.0), onPressed: BackgroundAudio.prev),
              FlatButton(child: Icon(
                status == 'pause' ? Icons.play_circle_outline : Icons.pause_circle_outline,
                size: 50.0,
              ), onPressed: toggle),
              FlatButton(child: Icon(Icons.skip_next, size: 50.0), onPressed: BackgroundAudio.next),
            ]
          ),
        ]
      ),
    );

    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: Text('Plugin example app'),
        ),
        body: Column(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: <Widget>[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: <Widget>[
                _buildPlaylist(playlists[0]),
                _buildPlaylist(playlists[1]),
              ],
            ),
            status == 'stopped' ? Container() : player,
          ],
        )
      ),
    );
  }
}
