import 'dart:async';

import 'package:flutter/services.dart';
import 'package:meta/meta.dart';

enum NFCStatus {
  none,
  reading,
  read,
  stopped,
  error,
}

class NfcData {
  final String id;
  final String content;
  final String error;
  final String statusMapper;
  final String type;
  final bool isWritable;
  final int maxSize;

  NFCStatus status;

  NfcData({
    this.id,
    this.content,
    this.error,
    this.statusMapper,
    this.type,
    this.isWritable,
    this.maxSize,
  });

  factory NfcData.fromMap(Map data) {
    NfcData result = NfcData(
      id: data['nfcId'],
      content: data['nfcContent'],
      error: data['nfcError'],
      statusMapper: data['nfcStatus'],
      type: data['ndefType'],
      isWritable: data['ndefIsWritable'],
      maxSize: data['ndefMaxSize'],
    );
    switch (result.statusMapper) {
      case 'none':
        result.status = NFCStatus.none;
        break;
      case 'reading':
        result.status = NFCStatus.reading;
        break;
      case 'stopped':
        result.status = NFCStatus.stopped;
        break;
      case 'error':
        result.status = NFCStatus.error;
        break;
      default:
        result.status = NFCStatus.none;
    }
    return result;
  }
}

class FlutterNfcReader {
  static FlutterNfcReader _instance;
  factory FlutterNfcReader() {
    if (_instance == null) {
      final MethodChannel methodChannel =
          const MethodChannel('flutter_nfc_reader');
      final EventChannel eventChannel = const EventChannel(
          'it.matteocrippa.flutternfcreader.flutter_nfc_reader');
      _instance = FlutterNfcReader.private(methodChannel, eventChannel);
    }

    return _instance;
  }

  @visibleForTesting
  FlutterNfcReader.private(
    this._methodChannel,
    this._eventChannel,
  );

  final MethodChannel _methodChannel;
  final EventChannel _eventChannel;
  Stream<NfcData> _onTagDiscovered;

  Stream<NfcData> get onTagDiscovered {
    if (_onTagDiscovered == null) {
      _onTagDiscovered = _eventChannel
          .receiveBroadcastStream()
          .map((result) => NfcData.fromMap(result));
    }
    return _onTagDiscovered;
  }

  Future<bool> get hasNfcFeature async {
    return await _methodChannel.invokeMethod('NfcSupported');
  }

  Future<bool> get isNfcEnabled async {
    return await _methodChannel.invokeMethod('NfcIsEnabled');
  }

  Future<bool> get isNdefPushEnabled async {
    return await _methodChannel.invokeMethod('NfcIsNdefPushEnabled');
  }

  Future<void> start() async {
    return await _methodChannel.invokeMethod('NfcStart');
  }

  Future<NfcData> stop() async {
    final Map data = await _methodChannel.invokeMethod('NfcStop');

    final NfcData result = NfcData.fromMap(data);

    return result;
  }
}
