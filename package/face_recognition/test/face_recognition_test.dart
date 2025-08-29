import 'package:face_recognition/face_recognition.dart';
import 'package:face_recognition/face_recognition_method_channel.dart';
import 'package:face_recognition/face_recognition_platform_interface.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFaceRecognitionPlatform
    with MockPlatformInterfaceMixin
    implements FaceRecognitionPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<List<String>?> startFaceRecognition() => Future.value(['1', '2', '3']);
}

void main() {
  final FaceRecognitionPlatform initialPlatform =
      FaceRecognitionPlatform.instance;

  test('$MethodChannelFaceRecognition is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFaceRecognition>());
  });

  test('getPlatformVersion', () async {
    FaceRecognition faceRecognitionPlugin = FaceRecognition();
    MockFaceRecognitionPlatform fakePlatform = MockFaceRecognitionPlatform();
    FaceRecognitionPlatform.instance = fakePlatform;

    expect(await faceRecognitionPlugin.getPlatformVersion(), '42');
  });
}
