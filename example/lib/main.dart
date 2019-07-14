import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_nfc_reader/flutter_nfc_reader.dart';
import 'package:screen/screen.dart';
import 'package:vibrate/vibrate.dart';

void main() => runApp(new MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  NfcData _nfcData;
  FlutterNfcReader _nfcReader;

  @override
  void initState() {
    Screen.keepOn(true);
    _nfcReader = FlutterNfcReader();
    super.initState();
  }

  Future<void> startNFC() async {
    setState(() {
      _nfcData = NfcData();
      _nfcData.status = NFCStatus.reading;
    });

    print('NFC: Scan started');

    print('NFC: Scan readed NFC tag');
    _nfcReader.onTagDiscovered.listen((response) {
      Vibrate.vibrate();
      // Vibrate.feedback(FeedbackType.success);

      setState(() {
        _nfcData = response;
      });
    });

    _nfcReader.start();
  }

  Future<void> stopNFC() async {
    NfcData response;

    try {
      print('NFC: Stop scan by user');
      response = await _nfcReader.stop();
    } on PlatformException {
      print('NFC: Stop scan exception');
      response = NfcData(
        id: '',
        content: '',
        error: 'NFC scan stop exception',
        statusMapper: '',
      );
      response.status = NFCStatus.error;
    }

    setState(() {
      _nfcData = response;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
          body: SafeArea(
        top: true,
        bottom: true,
        child: new Center(
          child: ListView(
            children: <Widget>[
              new SizedBox(
                height: 10.0,
              ),
              new Text(
                '- NFC Status -\n',
                textAlign: TextAlign.center,
              ),
              new Text(
                _nfcData != null ? 'Status: ${_nfcData.status}' : '',
                textAlign: TextAlign.center,
              ),
              new Text(
                _nfcData != null ? 'Identifier: ${_nfcData.id}' : '',
                textAlign: TextAlign.center,
              ),
              new Text(
                _nfcData != null ? 'Content: ${_nfcData.content}' : '',
                textAlign: TextAlign.center,
              ),
              new Text(
                _nfcData != null ? 'Error: ${_nfcData.error}' : '',
                textAlign: TextAlign.center,
              ),
              new Text(
                _nfcData != null ? 'Type: ${_nfcData.type}' : '',
                textAlign: TextAlign.center,
              ),
              new Text(
                _nfcData != null ? 'Is Writeable: ${_nfcData.isWritable}' : '',
                textAlign: TextAlign.center,
              ),
              new RaisedButton(
                child: Text('Start NFC'),
                onPressed: () {
                  startNFC();
                },
              ),
              new RaisedButton(
                child: Text('Stop NFC'),
                onPressed: () {
                  stopNFC();
                },
              ),
            ],
          ),
        ),
      )),
    );
  }
}
