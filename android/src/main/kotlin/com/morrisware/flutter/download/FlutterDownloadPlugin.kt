package com.morrisware.flutter.download

import android.content.Context
import androidx.annotation.NonNull
import com.morrisware.flutter.net.FileDownloader
import com.morrisware.flutter.net.core.Response
import com.morrisware.flutter.net.request.FileRequest

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.File

/** FlutterDownloadPlugin */
class FlutterDownloadPlugin : FlutterPlugin, MethodCallHandler {

    private lateinit var channel: MethodChannel
    private lateinit var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding
    private lateinit var context: Context

    companion object {
        const val CHANNEL_NAME = "plugin.morrisware/download"
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        this.flutterPluginBinding = flutterPluginBinding
        this.context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, CHANNEL_NAME)
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (call.method == "download") {
            val url = call.argument<String>("url")
            val tag = call.argument<String>("tag")
            val isNewDownload = FileDownloader.downloadUrl(context, url, tag, object : FileRequest.FileResponseCallback() {
                override fun onSuccess(response: Response<File>?) {
                    channel.invokeMethod("downloadResponse", mapOf(
                            Pair("url", url),
                            Pair("tag", tag),
                            Pair("type", "onSuccess"),
                            Pair("path", response?.data?.absolutePath)
                    ))
                }

                override fun onFailure(response: Response<File>?) {
                    channel.invokeMethod("downloadResponse", mapOf(
                            Pair("url", url),
                            Pair("tag", tag),
                            Pair("type", "onFailure"),
                            Pair("error", response?.error?.message)
                    ))
                }

                override fun onFileDownloadProgress(length: Long, contentLength: Long) {
                }
            })
            result.success(isNewDownload)
        } else if (call.method == "cancelDownload") {
            val url = call.argument<String>("url")
            val tag = call.argument<String>("tag")
            FileDownloader.cancelDownload(context, url, tag)
            result.success(null);
        } else if (call.method == "getDownloadFile") {
            val url = call.argument<String>("url")
            val tag = call.argument<String>("tag")
            val file = FileDownloader.getDownloadFile(context, url, tag)
            result.success(file?.absolutePath)
        } else if (call.method == "getDownloadStatus") {
            val url = call.argument<String>("url")
            val tag = call.argument<String>("tag")
            val array = FileDownloader.getDownloadStatus(context, url, tag)
            result.success(array)
        } else if (call.method == "clearCache") {
            val interval = call.argument<Long>("interval")
            if (interval != null) {
                FileDownloader.clearCache(context, interval)
            }
            result.success(null)
        } else if (call.method == "openFile") {
            val path = call.argument<String>("path")
            if (!path.isNullOrEmpty()) {
                DownloadHelper.tryOpenFile(flutterPluginBinding.applicationContext, File(path))
            }
        } else if (call.method == "jumpBrowser") {
            val url = call.argument<String>("url")
            if (!url.isNullOrEmpty()) {
                DownloadHelper.jumpBrowser(flutterPluginBinding.applicationContext, url)
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
