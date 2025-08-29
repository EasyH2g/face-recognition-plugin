package com.h2g.face.plugin.face_recognition

import android.app.Activity
import android.content.Intent
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry

/** FaceRecognitionPlugin */
class FaceRecognitionPlugin: FlutterPlugin, MethodCallHandler, ActivityAware,
  PluginRegistry.ActivityResultListener {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private var activityBinding: ActivityPluginBinding? = null
  private var pendingResult: Result? = null

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "face_recognition")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    val activity = activityBinding?.activity
    if (activity == null) {
      result.error("activity_not_prepare", "Activity not prepare", null)
      return
    }
    if (call.method == "getPlatformVersion") {
      result.success("Android ${android.os.Build.VERSION.RELEASE}")
    } else if (call.method == "startFaceRecognition") {
      if (pendingResult != null) {
        pendingResult!!.error("multiple_requests", "Cancelled by a second request.", null)
        pendingResult = null
      }
      pendingResult = result
      val intent = Intent(activity, FaceRecognitionActivity::class.java)
      activity.startActivityForResult(intent, CommonConstant.FACE_DETECTION_CODE)
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activityBinding = binding
    binding.addActivityResultListener(this)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activityBinding?.removeActivityResultListener(this)
    activityBinding = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activityBinding = binding
    binding.addActivityResultListener(this)
  }

  override fun onDetachedFromActivity() {
    activityBinding?.removeActivityResultListener(this)
    activityBinding = null
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    if (resultCode == Activity.RESULT_OK && data != null && requestCode == CommonConstant.FACE_DETECTION_CODE) {
      val first = data.getStringExtra(CommonConstant.FACE_FIRST_URL)
      val second = data.getStringExtra(CommonConstant.FACE_SECOND_URL)
      val third = data.getStringExtra(CommonConstant.FACE_THIRD_URL)
      if (first != null && second != null && third != null) {
        pendingResult?.success(listOf<String>(first, second, third))
        pendingResult = null
        return true
      } else {
        pendingResult?.success(listOf<String>())
        pendingResult = null
        return true
      }
    }
    pendingResult?.error("face_detection_failed", "Face detection failed", null)
    pendingResult = null
    return true
  }
}
