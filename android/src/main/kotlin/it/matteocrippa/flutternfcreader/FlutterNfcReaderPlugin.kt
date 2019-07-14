package it.matteocrippa.flutternfcreader

import android.Manifest
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.NfcAdapter.ReaderCallback
import android.nfc.Tag
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.StreamHandler
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import android.util.Log
import android.nfc.tech.*
import android.os.*
import java.nio.charset.Charset

const val PERMISSION_NFC = 1007

class FlutterNfcReaderPlugin(registrar: Registrar) : MethodCallHandler, StreamHandler, ReaderCallback {
    private val activity = registrar.activity()

    private var isReading = false
    private var nfcAdapter: NfcAdapter? = null

    private var eventSink: EventChannel.EventSink? = null

    private var kId = "nfcId"
    private var kContent = "nfcContent"
    private var kError = "nfcError"
    private var kStatus = "nfcStatus"
    private var kType = "ndefType"
    private var kIsWritable = "ndefIsWritable"
    private var kCanMakeReadOnly = "ndefCanMakeReadOnly"
    private var kMaxSize = "ndefMaxSize"

    private var READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val messenger = registrar.messenger()
            val methodChannel = MethodChannel(messenger, "flutter_nfc_reader")
            val eventChannel = EventChannel(messenger, "it.matteocrippa.flutternfcreader.flutter_nfc_reader")
            val plugin = FlutterNfcReaderPlugin(registrar)

            eventChannel.setStreamHandler(plugin)
            methodChannel.setMethodCallHandler(plugin)
        }
    }

    fun setStaticValue(className: String, fieldName: String, newValue: Any) {
        val field = Class.forName(className).getDeclaredField(fieldName)
        field.setAccessible(true)
        val oldValue = field.get(Class.forName(className));
        field.set(oldValue, newValue);
    }

    init {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        Log.i("RHALFF", "ON METHOD CALL ${call.method}")
        when (call.method) {
            "NfcStart" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    activity.requestPermissions(
                        arrayOf(Manifest.permission.NFC),
                        PERMISSION_NFC
                    )
                }

                startNFC()

                if (!isReading) {
                    result.error("404", "NFC Hardware not found", null)
                    return
                }

                result.success(null)
            }
            "NfcSupported" -> {
                val data = activity.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
                result.success(data)
            }
            "NfcIsEnabled" -> {
                val data = nfcAdapter?.isEnabled
                result.success(data)
            }
            "NfcIsNdefPushEnabled" -> {
                val data = nfcAdapter?.isNdefPushEnabled
                result.success(data)
            }
            "NfcStop" -> {
                stopNFC()
                val data = mapOf(kId to "", kContent to "", kError to "", kStatus to "stopped")
                result.success(data)
            }
            else -> {
                result.notImplemented()
            }
        }
    }


    // EventChannel.StreamHandler methods
    override fun onListen(arguments: Any?, eventSink: EventChannel.EventSink?) {
      this.eventSink = eventSink
    }

    override fun onCancel(arguments: Any?) {
      eventSink = null
      stopNFC()
    }

    private fun startNFC(): Boolean {
        if (nfcAdapter?.isEnabled == true) {
          // Only activate if we need this hack (e.g. on the huwaei watch).
          // because the hack is specific to that watch adapter
          setStaticValue("android.nfc.NfcAdapter", "sHasNfcFeature", true)
          nfcAdapter?.enableReaderMode(activity, this, READER_FLAGS, null)
          Log.i("RHALFF", "ENABLED READER MODE!!!")

          isReading = true
        } else {
          isReading = false
        }

        return isReading
    }

    private fun stopNFC() {
        if (isReading) {
          nfcAdapter?.disableReaderMode(activity)
          Log.i("RHALFF", "DISABLED READER MODE!!!")
        }

        isReading = false
    }

    override fun onTagDiscovered(tag: Tag?) {
        Log.i("RHALFF", "DISCOVERED TAG!!!")
        // convert tag to NDEF tag
        val ndef = Ndef.get(tag)
        // ndef will be null if the discovered tag is not a NDEF tag
        // read NDEF message
        ndef?.connect()
        // val message = ndef?.ndefMessage
        val message = ndef?.cachedNdefMessage // take the cached one else it's read again I guess?
                ?.toByteArray()
                ?.toString(Charset.forName("UTF-8")) ?: ""
        val id = bytesToHexString(tag?.id) ?: ""
        ndef?.close()
        if (message != null) {
            val data = mapOf(
              kId to id,
              kContent to message,
              kError to "",
              kStatus to "read",
              kType to ndef.type,
              kIsWritable to ndef.isWritable,
              kMaxSize to ndef.maxSize,
              kCanMakeReadOnly to ndef.canMakeReadOnly()
            )
            eventSink?.success(data)
        }
    }

    private fun bytesToHexString(src: ByteArray?): String? {
        val stringBuilder = StringBuilder("0x")
        if (src == null || src.isEmpty()) {
            return null
        }

        val buffer = CharArray(2)
        for (i in src.indices) {
            buffer[0] = Character.forDigit(src[i].toInt().ushr(4).and(0x0F), 16)
            buffer[1] = Character.forDigit(src[i].toInt().and(0x0F), 16)
            stringBuilder.append(buffer)
        }

        return stringBuilder.toString()
    }
}
