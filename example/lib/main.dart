import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_download/flutter_download.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  Timer _timer;
  final String url =
      "https://moguxingqiu.oss-cn-hangzhou.aliyuncs.com/cpa/file/jrapp_jr2688.apk";

  @override
  void initState() {
    super.initState();
    FlutterDownload.download(url);
    _timer = Timer.periodic(Duration(seconds: 1), (timer) async {
      DownloadStatus status = await FlutterDownload.getDownloadStatus(url);
      print(status);
      if (status.status == DownloadStatus.STATUS_FAILED ||
          status.status == DownloadStatus.STATUS_SUCCESSFUL) {
        _timer.cancel();
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Text('Running on: $_platformVersion\n'),
        ),
      ),
    );
  }
}
