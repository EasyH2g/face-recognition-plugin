import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'face_recognition_method_channel.dart';

abstract class FaceRecognitionPlatform extends PlatformInterface {
  /// Constructs a FaceRecognitionPlatform.
  FaceRecognitionPlatform() : super(token: _token);

  static final Object _token = Object();

  static FaceRecognitionPlatform _instance = MethodChannelFaceRecognition();

  /// The default instance of [FaceRecognitionPlatform] to use.
  ///
  /// Defaults to [MethodChannelFaceRecognition].
  static FaceRecognitionPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FaceRecognitionPlatform] when
  /// they register themselves.
  static set instance(FaceRecognitionPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<List<String>?> startFaceRecognition() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
