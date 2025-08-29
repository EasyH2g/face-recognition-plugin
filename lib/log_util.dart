import 'package:logger/logger.dart';

class LogUtil {
  static final Logger _logger = Logger(
    printer: PrettyPrinter(
      methodCount: 0, // 不显示调用堆栈
      colors: true, // 终端彩色输出
      printEmojis: false, // 显示表情符号标识级别
    ),
  );

  static void v(dynamic msg) => _logger.v('VERBOSE: $msg');
  static void d(dynamic msg) => _logger.d('DEBUG: $msg');
  static void i(dynamic msg) => _logger.i('INFO: $msg');
  static void w(dynamic msg) => _logger.w('WARN: $msg');
  static void e(dynamic msg, [dynamic error]) =>
      _logger.e('ERROR: $msg', error: error);
}
