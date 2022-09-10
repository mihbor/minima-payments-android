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

import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

/**
 * This is a sample APDU Service which demonstrates how to interface with the card emulation support
 * added in Android 4.4, KitKat.
 *
 *
 * This sample will be invoked for any terminals selecting AID of 0xF22222222.
 * See src/main/res/xml/aid_list.xml for more details.
 *
 *
 * Note: This is a low-level interface. Unlike the NdefMessage many developers
 * are familiar with for implementing Android Beam in apps, card emulation only provides a
 * byte-array based communication channel. It is left to developers to implement higher level
 * protocol support as needed.
 */
class CardService : HostApduService() {
  private var data: String? = ""
  override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
    Log.i(TAG, "Received start command")
    // Check if intent has extras
    intent.extras?.let {

      // Get message
      data = it.getString("data")
      Log.i(TAG, "Data of size " + data?.length + " : " + data)
    }
    return START_NOT_STICKY
  }

  /**
   * Called if the connection to the NFC card is lost, in order to let the application know the
   * cause for the disconnection (either a lost link, or another AID being selected by the
   * reader).
   *
   * @param reason Either DEACTIVATION_LINK_LOSS or DEACTIVATION_DESELECTED
   */
  override fun onDeactivated(reason: Int) {}

  /**
   * This method will be called when a command APDU has been received from a remote device. A
   * response APDU can be provided directly by returning a byte-array in this method. In general
   * response APDUs must be sent as quickly as possible, given the fact that the user is likely
   * holding his device over an NFC reader when this method is called.
   *
   *
   * If there are multiple services that have registered for the same AIDs in
   * their meta-data entry, you will only get called if the user has explicitly selected your
   * service, either as a default or just for the next tap.
   *
   *
   * This method is running on the main thread of your application. If you
   * cannot return a response APDU immediately, return null and use the [ ][.sendResponseApdu] method later.
   *
   * @param commandApdu The APDU that received from the remote device
   * @param extras A bundle containing extra data. May be null.
   * @return a byte-array containing the response APDU, or null if no response APDU can be sent
   * at this point.
   */
  // BEGIN_INCLUDE(processCommandApdu)
  override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
    Log.w(TAG, "Received APDU: " + commandApdu.toHex())
    // If the APDU matches the SELECT AID command for this service,
    // send the loyalty card account number, followed by a SELECT_OK status trailer (0x9000).
    Log.i(TAG, "SELECT_APDU is: " + SELECT_APDU.toHex())
    return if (SELECT_APDU.contentEquals(commandApdu)) {
//            String account = AccountStorage.GetAccount(this);
      val dataBytes = data!!.toByteArray()
      Log.w(TAG, "Sending data of size " + dataBytes.size + " : " + data)
      dataBytes + SELECT_OK_SW
    } else {
      UNKNOWN_CMD_SW
    }
  }

  companion object {
    private const val TAG = "CardService"

    // AID for our loyalty card service.
    private const val AID = "F222222222"

    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private const val SELECT_APDU_HEADER = "00A40400"

    // "OK" status word sent in response to SELECT AID command (0x9000)
    private val SELECT_OK_SW = "9000".decodeHex()

    // "UNKNOWN" status word sent in response to invalid APDU command (0x0000)
    private val UNKNOWN_CMD_SW = "0000".decodeHex()
    private val SELECT_APDU = BuildSelectApdu(AID)
    // END_INCLUDE(processCommandApdu)
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
}