/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.testapp

import android.nfc.NfcAdapter.ReaderCallback
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import java.io.IOException
import java.lang.ref.WeakReference
import java.nio.charset.Charset
import java.util.*

/**
 * Callback class, invoked when an NFC card is scanned while the device is running in reader mode.
 *
 * Reader mode can be invoked by calling NfcAdapter
 */
class CardReader(dataCallback: DataCallback) : ReaderCallback {
  // Weak reference to prevent retain loop. mAccountCallback is responsible for exiting
  // foreground mode before it becomes invalid (e.g. during onPause() or onStop()).
  private val dataCallback: WeakReference<DataCallback>

  interface DataCallback {
    fun onDataReceived(data: String)
  }

  /**
   * Callback when a new tag is discovered by the system.
   *
   *
   * Communication with the card should take place here.
   *
   * @param tag Discovered tag
   */
  override fun onTagDiscovered(tag: Tag) {
    Log.i(TAG, "New tag discovered")
    // Android's Host-based Card Emulation (HCE) feature implements the ISO-DEP (ISO 14443-4)
    // protocol.
    //
    // In order to communicate with a device using HCE, the discovered tag should be processed
    // using the IsoDep class.
    val isoDep = IsoDep.get(tag)
    if (isoDep != null) {
      try {
        // Connect to the remote NFC device
        isoDep.connect()
        isoDep.timeout = 15000
        Log.i(TAG, "isExtendedLengthApduSupported: " + isoDep.isExtendedLengthApduSupported)
        // Build SELECT AID command for our service.
        // This command tells the remote device which service we wish to communicate with.
        Log.i(TAG, "Requesting remote AID: " + AID + " max transceive length is: " + isoDep.maxTransceiveLength)
        val command = BuildSelectApdu(AID)
        // Send command to remote device
        Log.i(TAG, "Sending: " + command.toHex())
        val result = isoDep.transceive(command)
        // If AID is successfully selected, 0x9000 is returned as the status word (last 2
        // bytes of the result) by convention. Everything before the status word is
        // optional payload, which is used here to hold the account number.
        val resultLength = result.size
        Log.i(TAG, "Response length: $resultLength")
        val statusWord = byteArrayOf(result[resultLength - 2], result[resultLength - 1])
        Log.i(TAG, "Status word: " + statusWord.toHex())
        val payload = Arrays.copyOf(result, resultLength - 2)
        if (SELECT_OK_SW.contentEquals(statusWord)) {
          // The remote NFC device will immediately respond with its stored account number
          val data = String(payload, Charset.forName("UTF-8"))
          Log.i(TAG, "Received: $data")
          // Inform CardReaderFragment of received account number
          dataCallback.get()!!.onDataReceived(data)
        }
      } catch (e: IOException) {
        Log.e(TAG, "Error communicating with card: $e")
      }
    }
  }

  companion object {
    private const val TAG = "CardReader"

    // AID for our loyalty card service.
    private const val AID = "F222222222"

    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private const val SELECT_APDU_HEADER = "00A40400"

    // "OK" status word sent in response to SELECT AID command (0x9000)
    private val SELECT_OK_SW = byteArrayOf(0x90.toByte(), 0x00.toByte())

    /**
     * Build APDU for SELECT AID command. This command indicates which service a reader is
     * interested in communicating with. See ISO 7816-4.
     *
     * @param aid Application ID (AID) to select
     * @return APDU for SELECT AID command
     */
    fun BuildSelectApdu(aid: String): ByteArray {
      // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
      return (SELECT_APDU_HEADER + String.format("%02X", aid.length / 2) + aid).decodeHex()
    }

    fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02X".format(eachByte) }

    fun String.decodeHex(): ByteArray {
      check(length % 2 == 0) { "Must have an even length" }

      return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
    }
  }

  init {
    this.dataCallback = WeakReference(dataCallback)
  }
}