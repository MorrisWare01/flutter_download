import 'dart:async';

import 'package:flutter/services.dart';

typedef DownloadSuccessCallback = void Function(String path);
typedef DownloadFailureCallback = void Function(String error);

class _DownloadCallback {
  DownloadSuccessCallback onSuccess;
  DownloadFailureCallback onFailure;

  _DownloadCallback(this.onSuccess, this.onFailure);
}

class FlutterDownload {
  factory FlutterDownload() => _getInstance();

  static FlutterDownload get instance => _getInstance();

  static FlutterDownload _instance;

  static _getInstance() {
    if (_instance == null) {
      _instance = FlutterDownload._internal();
    }
    return _instance;
  }

  FlutterDownload._internal() {
    _channel.setMethodCallHandler((call) {
      switch (call.method) {
        case "onDownloadResponse":
          if (call.arguments is Map) {
            Map map = call.arguments as Map;
            final url = map["url"];
            final tag = map["tag"];
            final type = map["type"];
            final path = map["path"];
            final error = map["error"];
            final callback =
                _downloadCallbackMap.remove(_downloadKey(url, tag));
            if (callback != null) {
              if (type == "onSuccess") {
                callback.onSuccess?.call(path);
              } else if (type == "onFailure") {
                callback.onSuccess?.call(error);
              }
            }
          }
          break;
        default:
          throw MissingPluginException();
      }
      return null;
    });
  }

  final MethodChannel _channel =
      const MethodChannel('plugin.morrisware/download');
  final Map<String, _DownloadCallback> _downloadCallbackMap = Map();

  String _downloadKey(String url, String tag) {
    return url + "-" + tag;
  }

  void removeDownloadCallback(String url, String tag) {
    _downloadCallbackMap.remove(_downloadKey(url, tag));
  }

  Future<bool> download(
    String url, {
    String tag,
    DownloadSuccessCallback onSuccess,
    DownloadFailureCallback onFailure,
  }) async {
    final bool isNewDownload =
        await _channel.invokeMethod("download", {"url": url, "tag": tag});
    if (onSuccess != null || onFailure != null) {
      _downloadCallbackMap[_downloadKey(url, tag)] =
          _DownloadCallback(onSuccess, onFailure);
    }
    return isNewDownload;
  }

  cancelDownload(String url, [String tag]) async {
    _channel.invokeMethod("cancelDownload", {"url": url, "tag": tag});
  }

  Future<String> getDownloadFile(String url, [String tag]) async {
    final String path = await _channel
        .invokeMethod("getDownloadFile", {"url": url, "tag": tag});
    return path;
  }

  Future<DownloadStatus> getDownloadStatus(String url, [String tag]) async {
    final array = await _channel
        .invokeMethod("getDownloadStatus", {"url": url, "tag": tag});
    final int status = array[0];
    final int progress = array[1];
    return DownloadStatus(status, progress);
  }

  clearCache(Duration duration) async {
    await _channel.invokeMethod("clearCache", {
      "interval": duration.inMilliseconds,
    });
  }
}

class DownloadStatus {
  static const int STATUS_UNKNOWN = -1;
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
