import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';

class FlutterDownload {
  static const MethodChannel _channel =
      const MethodChannel('plugin.morrisware/download');

  static Future<bool> download(String url, [String tag]) async {
    final bool isNewDownload =
        await _channel.invokeMethod("download", {"url": url, "tag": tag});
    return isNewDownload;
  }

  static cancelDownload(String url, [String tag]) async {
    _channel.invokeMethod("cancelDownload", {"url": url, "tag": tag});
  }

  static Future<String> getDownloadFile(String url, [String tag]) async {
    final String path = await _channel
        .invokeMethod("getDownloadFile", {"url": url, "tag": tag});
    return path;
  }

  static Future<DownloadStatus> getDownloadStatus(String url,
      [String tag]) async {
    final Int32List array = await _channel
        .invokeMethod("getDownloadStatus", {"url": url, "tag": tag});
    final int status = array[0];
    final int progress = array[1];
    return DownloadStatus(status, progress);
  }

  static clearCache(Duration duration) async {
    await _channel.invokeMethod("clearCache", {
      "interval": duration.inMilliseconds,
    });
  }
}

class DownloadStatus {
  static const int STATUS_PENDING = 1;
  static const int STATUS_RUNNING = 1 << 1;
  static const int STATUS_SUCCESSFUL = 1 << 3;
  static const int STATUS_FAILED = 1 << 4;

  final int status;
  final int progress;

  DownloadStatus(this.status, this.progress);

  @override
  String toString() {
    return 'DownloadStatus{status: $status, progress: $progress}';
  }
}
