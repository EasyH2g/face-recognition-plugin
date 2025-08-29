import 'face_recognition_platform_interface.dart';

class FaceRecognition {
  Future<String?> getPlatformVersion() {
    return FaceRecognitionPlatform.instance.getPlatformVersion();
  }

  Future<List<String>?> startFaceRecognition() {
    return FaceRecognitionPlatform.instance.startFaceRecognition();
  }
}
