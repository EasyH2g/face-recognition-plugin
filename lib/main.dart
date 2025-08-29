import 'dart:io';

import 'package:face_recognition/face_recognition.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';

import 'log_util.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // TRY THIS: Try running your application with "flutter run". You'll see
        // the application has a purple toolbar. Then, without quitting the app,
        // try changing the seedColor in the colorScheme below to Colors.green
        // and then invoke "hot reload" (save your changes or press the "hot
        // reload" button in a Flutter-supported IDE, or press "r" if you used
        // the command line to start the app).
        //
        // Notice that the counter didn't reset back to zero; the application
        // state is not lost during the reload. To reset the state, use hot
        // restart instead.
        //
        // This works for code too, not just values: Most code changes can be
        // tested with just a hot reload.
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
      ),
      home: const MyHomePage(title: 'Flutter Face Recognition'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  // This widget is the home page of your application. It is stateful, meaning
  // that it has a State object (defined below) that contains fields that affect
  // how it looks.

  // This class is the configuration for the state. It holds the values (in this
  // case the title) provided by the parent (in this case the App widget) and
  // used by the build method of the State. Fields in a Widget subclass are
  // always marked "final".

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  final faceDetection = FaceRecognition();
  String? _imagePath; // 新增状态变量

  @override
  Widget build(BuildContext context) {
    // This method is rerun every time setState is called, for instance as done
    // by the _incrementCounter method above.
    //
    // The Flutter framework has been optimized to make rerunning build methods
    // fast, so that you can just rebuild anything that needs updating rather
    // than having to individually change instances of widgets.
    return Scaffold(
      appBar: AppBar(
        // TRY THIS: Try changing the color here to a specific color (to
        // Colors.amber, perhaps?) and trigger a hot reload to see the AppBar
        // change color while the other colors stay the same.
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        // Here we take the value from the MyHomePage object that was created by
        // the App.build method, and use it to set our appbar title.
        title: Text(widget.title),
      ),
      body: SafeArea(
        child: Column(
          children: [
            ElevatedButton(
              onPressed: () {
                clickButton();
              },
              child: Text("Start Face Recognition"),
            ),
            if (_imagePath != null) _buildImagePreview(), // 自定义图片预览组件
          ],
        ),
      ),
    );
  }

  Future<void> clickButton() async {
    final cameraStatus = await Permission.camera.status;
    if (cameraStatus.isGranted) {
      startFaceRecognition();
    } else {
      final status = await Permission.camera.request();
      if (status.isGranted) {
        startFaceRecognition();
      } else if (status.isPermanentlyDenied) {
        openAppSettings();
      }
    }
  }

  Future<void> startFaceRecognition() async {
    try {
      final result = await faceDetection.startFaceRecognition();

      if (!mounted || result == null || result.isEmpty) {
        return;
      }
      LogUtil.d("startFaceRecognition.result: ${result[0]}");
      setState(() {
        _imagePath = result[0];
      });
    } catch (e) {
      LogUtil.e("startFaceRecognition.error: $e");
    }
  }

  Widget _buildImagePreview() {
    return FutureBuilder<bool>(
      future: _checkFileExists(_imagePath!),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return CircularProgressIndicator();
        }
        if (snapshot.data == true) {
          return Image.file(
            File(_imagePath!),
            width: 200,
            height: 200,
            fit: BoxFit.cover,
            errorBuilder: (context, error, stackTrace) {
              return Text('图片加载失败: $error');
            },
          );
        } else {
          return Text("图片文件不存在");
        }
      },
    );
  }

  Future<bool> _checkFileExists(String path) async {
    return await File(path).exists();
  }
}
