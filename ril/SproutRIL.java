/*
 * Copyright (C) 2014 The CyanogenMod Project
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

package com.android.internal.telephony;

import static com.android.internal.telephony.RILConstants.*;
import static com.android.internal.telephony.TelephonyIntents.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.AsyncResult;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Parcel;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.SignalStrength;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccIoResult;

public class SproutRIL extends RIL implements CommandsInterface {

static final int RIL_REQUEST_SET_3G_CAPABILITY = 128;
static final int RIL_REQUEST_ALLOW_DATA = 125;
static final int RIL_REQUEST_GET_HARDWARE_CONFIG = 122;
static final int RIL_REQUEST_SET_UICC_SUBSCRIPTION = 124;

// MTK-specific vendor commands (Part 1)
static final int RIL_REQUEST_VENDOR_BASE = 2000;
static final int RIL_REQUEST_GET_COLP = (RIL_REQUEST_VENDOR_BASE + 0);
static final int RIL_REQUEST_SET_COLP = (RIL_REQUEST_VENDOR_BASE + 1);
static final int RIL_REQUEST_GET_COLR = (RIL_REQUEST_VENDOR_BASE + 2);
static final int RIL_REQUEST_GET_CCM = (RIL_REQUEST_VENDOR_BASE + 3);
static final int RIL_REQUEST_GET_ACM = (RIL_REQUEST_VENDOR_BASE + 4);
static final int RIL_REQUEST_GET_ACMMAX = (RIL_REQUEST_VENDOR_BASE + 5);
static final int RIL_REQUEST_GET_PPU_AND_CURRENCY = (RIL_REQUEST_VENDOR_BASE + 6);
static final int RIL_REQUEST_SET_ACMMAX = (RIL_REQUEST_VENDOR_BASE + 7);
static final int RIL_REQUEST_RESET_ACM = (RIL_REQUEST_VENDOR_BASE + 8);
static final int RIL_REQUEST_SET_PPU_AND_CURRENCY = (RIL_REQUEST_VENDOR_BASE + 9);
static final int RIL_REQUEST_MODEM_POWEROFF = (RIL_REQUEST_VENDOR_BASE + 10);
static final int RIL_REQUEST_DUAL_SIM_MODE_SWITCH = (RIL_REQUEST_VENDOR_BASE + 11);
static final int RIL_REQUEST_QUERY_PHB_STORAGE_INFO = (RIL_REQUEST_VENDOR_BASE + 12);
static final int RIL_REQUEST_WRITE_PHB_ENTRY = (RIL_REQUEST_VENDOR_BASE + 13);
static final int RIL_REQUEST_READ_PHB_ENTRY = (RIL_REQUEST_VENDOR_BASE + 14);
static final int RIL_REQUEST_SET_GPRS_CONNECT_TYPE = (RIL_REQUEST_VENDOR_BASE + 15);
static final int RIL_REQUEST_SET_GPRS_TRANSFER_TYPE = (RIL_REQUEST_VENDOR_BASE + 16);
static final int RIL_REQUEST_MOBILEREVISION_AND_IMEI  = (RIL_REQUEST_VENDOR_BASE + 17);
static final int RIL_REQUEST_QUERY_SIM_NETWORK_LOCK	= (RIL_REQUEST_VENDOR_BASE + 18);
static final int RIL_REQUEST_SET_SIM_NETWORK_LOCK = (RIL_REQUEST_VENDOR_BASE + 19);
static final int RIL_REQUEST_SET_SCRI = (RIL_REQUEST_VENDOR_BASE + 20);
static final int RIL_REQUEST_BTSIM_CONNECT     = (RIL_REQUEST_VENDOR_BASE + 21);
static final int RIL_REQUEST_BTSIM_DISCONNECT_OR_POWEROFF   = (RIL_REQUEST_VENDOR_BASE + 22);
static final int RIL_REQUEST_BTSIM_POWERON_OR_RESETSIM   = (RIL_REQUEST_VENDOR_BASE + 23);
static final int RIL_REQUEST_BTSIM_TRANSFERAPDU   = (RIL_REQUEST_VENDOR_BASE + 24);
static final int RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL_WITH_ACT   = (RIL_REQUEST_VENDOR_BASE + 25);
static final int RIL_REQUEST_QUERY_ICCID   = (RIL_REQUEST_VENDOR_BASE + 26);
static final int RIL_REQUEST_USIM_AUTHENTICATION      = (RIL_REQUEST_VENDOR_BASE + 27);
static final int RIL_REQUEST_MODEM_POWERON   = (RIL_REQUEST_VENDOR_BASE + 28);
static final int RIL_REQUEST_GET_SMS_SIM_MEM_STATUS   = (RIL_REQUEST_VENDOR_BASE + 29);
static final int RIL_REQUEST_GET_PHONE_CAPABILITY = (RIL_REQUEST_VENDOR_BASE + 30);
static final int RIL_REQUEST_SET_PHONE_CAPABILITY = (RIL_REQUEST_VENDOR_BASE + 31);
static final int RIL_REQUEST_GET_POL_CAPABILITY = (RIL_REQUEST_VENDOR_BASE + 32);
static final int RIL_REQUEST_GET_POL_LIST = (RIL_REQUEST_VENDOR_BASE + 33);
static final int RIL_REQUEST_SET_POL_ENTRY = (RIL_REQUEST_VENDOR_BASE + 34);
static final int RIL_REQUEST_QUERY_UPB_CAPABILITY = (RIL_REQUEST_VENDOR_BASE + 35);
static final int RIL_REQUEST_EDIT_UPB_ENTRY = (RIL_REQUEST_VENDOR_BASE + 36);
static final int RIL_REQUEST_DELETE_UPB_ENTRY = (RIL_REQUEST_VENDOR_BASE + 37);
static final int RIL_REQUEST_READ_UPB_GAS_LIST = (RIL_REQUEST_VENDOR_BASE + 38);
static final int RIL_REQUEST_READ_UPB_GRP  = (RIL_REQUEST_VENDOR_BASE + 39);
static final int RIL_REQUEST_WRITE_UPB_GRP  = (RIL_REQUEST_VENDOR_BASE + 40);
static final int RIL_REQUEST_SET_SIM_RECOVERY_ON = (RIL_REQUEST_VENDOR_BASE + 41);
static final int RIL_REQUEST_GET_SIM_RECOVERY_ON = (RIL_REQUEST_VENDOR_BASE + 42);
static final int RIL_REQUEST_SET_TRM = (RIL_REQUEST_VENDOR_BASE + 43);
static final int RIL_REQUEST_DETECT_SIM_MISSING = (RIL_REQUEST_VENDOR_BASE + 44);
static final int RIL_REQUEST_GET_CALIBRATION_DATA = (RIL_REQUEST_VENDOR_BASE + 45);
static final int RIL_REQUEST_GET_PHB_STRING_LENGTH = (RIL_REQUEST_VENDOR_BASE + 46);
static final int RIL_REQUEST_GET_PHB_MEM_STORAGE = (RIL_REQUEST_VENDOR_BASE + 47);
static final int RIL_REQUEST_SET_PHB_MEM_STORAGE = (RIL_REQUEST_VENDOR_BASE + 48);
static final int RIL_REQUEST_READ_PHB_ENTRY_EXT = (RIL_REQUEST_VENDOR_BASE + 49);
static final int RIL_REQUEST_WRITE_PHB_ENTRY_EXT = (RIL_REQUEST_VENDOR_BASE + 50);
static final int RIL_REQUEST_GET_SMS_PARAMS = (RIL_REQUEST_VENDOR_BASE + 51);
static final int RIL_REQUEST_SET_SMS_PARAMS = (RIL_REQUEST_VENDOR_BASE + 52);
static final int RIL_REQUEST_SIM_TRANSMIT_BASIC = (RIL_REQUEST_VENDOR_BASE + 53);
static final int RIL_REQUEST_SIM_TRANSMIT_CHANNEL = (RIL_REQUEST_VENDOR_BASE + 54);
static final int RIL_REQUEST_SIM_GET_ATR = (RIL_REQUEST_VENDOR_BASE + 55);
static final int RIL_REQUEST_SET_CB_CHANNEL_CONFIG_INFO   = (RIL_REQUEST_VENDOR_BASE + 56);
static final int RIL_REQUEST_SET_CB_LANGUAGE_CONFIG_INFO  = (RIL_REQUEST_VENDOR_BASE + 57);
static final int RIL_REQUEST_GET_CB_CONFIG_INFO           = (RIL_REQUEST_VENDOR_BASE + 58);
static final int RIL_REQUEST_SET_ALL_CB_LANGUAGE_ON       = (RIL_REQUEST_VENDOR_BASE + 59);
static final int RIL_REQUEST_SET_ETWS = (RIL_REQUEST_VENDOR_BASE + 60);
static final int RIL_REQUEST_SET_FD_MODE = (RIL_REQUEST_VENDOR_BASE + 61);
static final int RIL_REQUEST_DETACH_PS = (RIL_REQUEST_VENDOR_BASE + 62);
static final int RIL_REQUEST_SIM_OPEN_CHANNEL_WITH_SW = (RIL_REQUEST_VENDOR_BASE + 63);
static final int RIL_REQUEST_SET_REG_SUSPEND_ENABLED = (RIL_REQUEST_VENDOR_BASE + 64);
static final int RIL_REQUEST_RESUME_REGISTRATION = (RIL_REQUEST_VENDOR_BASE + 65);
static final int RIL_REQUEST_STORE_MODEM_TYPE = (RIL_REQUEST_VENDOR_BASE + 66);
static final int RIL_REQUEST_QUERY_MODEM_TYPE = (RIL_REQUEST_VENDOR_BASE + 67);
static final int RIL_REQUEST_SIM_INTERFACE_SWITCH = (RIL_REQUEST_VENDOR_BASE + 68);
static final int RIL_REQUEST_UICC_SELECT_APPLICATION = (RIL_REQUEST_VENDOR_BASE + 69);
static final int RIL_REQUEST_UICC_DEACTIVATE_APPLICATION = (RIL_REQUEST_VENDOR_BASE + 70);
static final int RIL_REQUEST_UICC_APPLICATION_IO = (RIL_REQUEST_VENDOR_BASE + 71);
static final int RIL_REQUEST_UICC_AKA_AUTHENTICATE = (RIL_REQUEST_VENDOR_BASE + 72);
static final int RIL_REQUEST_UICC_GBA_AUTHENTICATE_BOOTSTRAP = (RIL_REQUEST_VENDOR_BASE + 73);
static final int RIL_REQUEST_UICC_GBA_AUTHENTICATE_NAF = (RIL_REQUEST_VENDOR_BASE + 74);
static final int RIL_REQUEST_STK_EVDL_CALL_BY_AP = (RIL_REQUEST_VENDOR_BASE + 75);
static final int RIL_REQUEST_GET_FEMTOCELL_LIST = (RIL_REQUEST_VENDOR_BASE + 76);
static final int RIL_REQUEST_ABORT_FEMTOCELL_LIST = (RIL_REQUEST_VENDOR_BASE + 77);
static final int RIL_REQUEST_SELECT_FEMTOCELL = (RIL_REQUEST_VENDOR_BASE + 78);
static final int RIL_REQUEST_SEND_OPLMN = (RIL_REQUEST_VENDOR_BASE + 79);
static final int RIL_REQUEST_GET_OPLMN_VERSION = (RIL_REQUEST_VENDOR_BASE + 80);
static final int RIL_REQUEST_ABORT_QUERY_AVAILABLE_NETWORKS = (RIL_REQUEST_VENDOR_BASE + 81);
static final int RIL_REQUEST_DIAL_UP_CSD = (RIL_REQUEST_VENDOR_BASE + 82);
static final int RIL_REQUEST_SET_TELEPHONY_MODE = (RIL_REQUEST_VENDOR_BASE + 83);
static final int RIL_REQUEST_HANGUP_ALL = (RIL_REQUEST_VENDOR_BASE + 84);
static final int RIL_REQUEST_FORCE_RELEASE_CALL = (RIL_REQUEST_VENDOR_BASE + 85);
static final int RIL_REQUEST_SET_CALL_INDICATION = (RIL_REQUEST_VENDOR_BASE + 86);
static final int RIL_REQUEST_EMERGENCY_DIAL = (RIL_REQUEST_VENDOR_BASE + 87);
static final int RIL_REQUEST_SET_ECC_SERVICE_CATEGORY = (RIL_REQUEST_VENDOR_BASE + 88);
static final int RIL_REQUEST_SET_ECC_LIST = (RIL_REQUEST_VENDOR_BASE + 89);
static final int RIL_REQUEST_GENERAL_SIM_AUTH = (RIL_REQUEST_VENDOR_BASE + 90);
static final int RIL_REQUEST_OPEN_ICC_APPLICATION = (RIL_REQUEST_VENDOR_BASE + 91);
static final int RIL_REQUEST_GET_ICC_APPLICATION_STATUS = (RIL_REQUEST_VENDOR_BASE + 92);
static final int RIL_REQUEST_SIM_IO_EX = (RIL_REQUEST_VENDOR_BASE + 93);
static final int RIL_REQUEST_SET_IMS_ENABLE = (RIL_REQUEST_VENDOR_BASE + 94);
static final int RIL_REQUEST_QUERY_AVAILABLE_NETWORKS_WITH_ACT = (RIL_REQUEST_VENDOR_BASE + 95);
static final int RIL_REQUEST_SEND_CNAP = (RIL_REQUEST_VENDOR_BASE + 96);
static final int RIL_REQUEST_SET_CLIP = (RIL_REQUEST_VENDOR_BASE + 97);
static final int RIL_REQUEST_SETUP_DEDICATE_DATA_CALL = (RIL_REQUEST_VENDOR_BASE + 98);
static final int RIL_REQUEST_DEACTIVATE_DEDICATE_DATA_CALL = (RIL_REQUEST_VENDOR_BASE + 99);
static final int RIL_REQUEST_MODIFY_DATA_CALL = (RIL_REQUEST_VENDOR_BASE + 100);
static final int RIL_REQUEST_ABORT_SETUP_DATA_CALL = (RIL_REQUEST_VENDOR_BASE + 101);
static final int RIL_REQUEST_PCSCF_DISCOVERY_PCO = (RIL_REQUEST_VENDOR_BASE + 102);
static final int RIL_REQUEST_CLEAR_DATA_BEARER = (RIL_REQUEST_VENDOR_BASE + 103);
static final int RIL_REQUEST_REMOVE_CB_MESSAGE = (RIL_REQUEST_VENDOR_BASE + 104);
static final int RIL_REQUEST_SET_DATA_CENTRIC = (RIL_REQUEST_VENDOR_BASE + 105);
static final int RIL_REQUEST_ADD_IMS_CONFERENCE_CALL_MEMBER = (RIL_REQUEST_VENDOR_BASE + 106);
static final int RIL_REQUEST_REMOVE_IMS_CONFERENCE_CALL_MEMBER = (RIL_REQUEST_VENDOR_BASE + 107);
static final int RIL_REQUEST_DIAL_WITH_SIP_URI = (RIL_REQUEST_VENDOR_BASE + 108);
static final int RIL_REQUEST_RETRIEVE_HELD_CALL = (RIL_REQUEST_VENDOR_BASE + 109);
static final int RIL_REQUEST_SET_SPEECH_CODEC_INFO = (RIL_REQUEST_VENDOR_BASE + 110);
static final int RIL_REQUEST_SET_DATA_ON_TO_MD = (RIL_REQUEST_VENDOR_BASE + 111);
static final int RIL_REQUEST_SET_REMOVE_RESTRICT_EUTRAN_MODE = (RIL_REQUEST_VENDOR_BASE + 112);
static final int RIL_REQUEST_SET_IMS_CALL_STATUS = (RIL_REQUEST_VENDOR_BASE + 113);
static final int RIL_REQUEST_SET_VT_CAPABILITY = (RIL_REQUEST_VENDOR_BASE + 114);
static final int RIL_REQUEST_VT_DIAL = (RIL_REQUEST_VENDOR_BASE + 115);
static final int RIL_REQUEST_VOICE_ACCEPT = (RIL_REQUEST_VENDOR_BASE + 116);
static final int RIL_REQUEST_CONFIG_MODEM_STATUS = (RIL_REQUEST_VENDOR_BASE + 117);
static final int RIL_REQUEST_SET_ACTIVE_PS_SLOT = (RIL_REQUEST_VENDOR_BASE + 118);
static final int RIL_REQUEST_CONFIRM_INTER_3GPP_IRAT_CHANGE = (RIL_REQUEST_VENDOR_BASE + 119);
static final int RIL_REQUEST_SET_SVLTE_RAT_MODE = (RIL_REQUEST_VENDOR_BASE + 120);
static final int RIL_REQUEST_TRIGGER_LTE_BG_SEARCH = (RIL_REQUEST_VENDOR_BASE + 121);
static final int RIL_REQUEST_CONFERENCE_DIAL = (RIL_REQUEST_VENDOR_BASE + 122);

// MTK-specific vendor commands (Part 2)
static final int RIL_LOCAL_REQUEST_VENDOR_BASE = 2500;
static final int RIL_LOCAL_REQUEST_SIM_AUTHENTICATION = (RIL_LOCAL_REQUEST_VENDOR_BASE + 0);
static final int RIL_LOCAL_REQUEST_USIM_AUTHENTICATION = (RIL_LOCAL_REQUEST_VENDOR_BASE + 1);
static final int RIL_LOCAL_REQUEST_QUERY_MODEM_THERMAL = (RIL_LOCAL_REQUEST_VENDOR_BASE + 2);
static final int RIL_LOCAL_REQUEST_RILD_READ_IMSI = (RIL_LOCAL_REQUEST_VENDOR_BASE + 3);
static final int RIL_LOCAL_REQUEST_GET_SHARED_KEY = (RIL_LOCAL_REQUEST_VENDOR_BASE + 4);
static final int RIL_LOCAL_REQUEST_UPDATE_SIM_LOCK_SETTINGS = (RIL_LOCAL_REQUEST_VENDOR_BASE + 5);
static final int RIL_LOCAL_REQUEST_GET_SIM_LOCK_INFO = (RIL_LOCAL_REQUEST_VENDOR_BASE + 6);
static final int RIL_LOCAL_REQUEST_RESET_SIM_LOCK_SETTINGS = (RIL_LOCAL_REQUEST_VENDOR_BASE + 7);
static final int RIL_LOCAL_REQUEST_GET_MODEM_STATUS = (RIL_LOCAL_REQUEST_VENDOR_BASE + 8);

// MTK-specific unsolicited responses
static final int RIL_UNSOL_VENDOR_BASE = 3000;
static final int RIL_UNSOL_NEIGHBORING_CELL_INFO = (RIL_UNSOL_VENDOR_BASE + 0);
static final int RIL_UNSOL_NETWORK_INFO = (RIL_UNSOL_VENDOR_BASE + 1);
static final int RIL_UNSOL_PHB_READY_NOTIFICATION = (RIL_UNSOL_VENDOR_BASE + 2);
static final int RIL_UNSOL_SIM_INSERTED_STATUS = (RIL_UNSOL_VENDOR_BASE + 3);
static final int RIL_UNSOL_RADIO_TEMPORARILY_UNAVAILABLE = (RIL_UNSOL_VENDOR_BASE + 4);
static final int RIL_UNSOL_ME_SMS_STORAGE_FULL = (RIL_UNSOL_VENDOR_BASE + 5);
static final int RIL_UNSOL_SMS_READY_NOTIFICATION = (RIL_UNSOL_VENDOR_BASE + 6);
static final int RIL_UNSOL_SCRI_RESULT = (RIL_UNSOL_VENDOR_BASE + 7);
static final int RIL_UNSOL_SIM_MISSING = (RIL_UNSOL_VENDOR_BASE + 8);
static final int RIL_UNSOL_GPRS_DETACH = (RIL_UNSOL_VENDOR_BASE + 9);
static final int RIL_UNSOL_ATCI_RESPONSE = (RIL_UNSOL_VENDOR_BASE + 10);
static final int RIL_UNSOL_SIM_RECOVERY = (RIL_UNSOL_VENDOR_BASE + 11);
static final int RIL_UNSOL_VIRTUAL_SIM_ON = (RIL_UNSOL_VENDOR_BASE + 12);
static final int RIL_UNSOL_VIRTUAL_SIM_OFF = (RIL_UNSOL_VENDOR_BASE + 13);
static final int RIL_UNSOL_INVALID_SIM = (RIL_UNSOL_VENDOR_BASE + 14);
static final int RIL_UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED = (RIL_UNSOL_VENDOR_BASE + 15);
static final int RIL_UNSOL_RESPONSE_ACMT = (RIL_UNSOL_VENDOR_BASE + 16);
static final int RIL_UNSOL_EF_CSP_PLMN_MODE_BIT = (RIL_UNSOL_VENDOR_BASE + 17);
static final int RIL_UNSOL_IMEI_LOCK = (RIL_UNSOL_VENDOR_BASE + 18);
static final int RIL_UNSOL_RESPONSE_MMRR_STATUS_CHANGED = (RIL_UNSOL_VENDOR_BASE + 19);
static final int RIL_UNSOL_SIM_PLUG_OUT = (RIL_UNSOL_VENDOR_BASE + 20);
static final int RIL_UNSOL_SIM_PLUG_IN = (RIL_UNSOL_VENDOR_BASE + 21);
static final int RIL_UNSOL_RESPONSE_ETWS_NOTIFICATION = (RIL_UNSOL_VENDOR_BASE + 22);
static final int RIL_UNSOL_RESPONSE_PLMN_CHANGED = (RIL_UNSOL_VENDOR_BASE + 23);
static final int RIL_UNSOL_RESPONSE_REGISTRATION_SUSPENDED = (RIL_UNSOL_VENDOR_BASE + 24);
static final int RIL_UNSOL_STK_EVDL_CALL = (RIL_UNSOL_VENDOR_BASE + 25);
static final int RIL_UNSOL_DATA_PACKETS_FLUSH = (RIL_UNSOL_VENDOR_BASE + 26);
static final int RIL_UNSOL_FEMTOCELL_INFO = (RIL_UNSOL_VENDOR_BASE + 27);
static final int RIL_UNSOL_STK_SETUP_MENU_RESET = (RIL_UNSOL_VENDOR_BASE + 28);
static final int RIL_UNSOL_APPLICATION_SESSION_ID_CHANGED = (RIL_UNSOL_VENDOR_BASE + 29);
static final int RIL_UNSOL_ECONF_SRVCC_INDICATION = (RIL_UNSOL_VENDOR_BASE + 30);
static final int RIL_UNSOL_IMS_ENABLE_DONE = (RIL_UNSOL_VENDOR_BASE + 31);
static final int RIL_UNSOL_IMS_DISABLE_DONE = (RIL_UNSOL_VENDOR_BASE + 32);
static final int RIL_UNSOL_IMS_REGISTRATION_INFO = (RIL_UNSOL_VENDOR_BASE + 33);
static final int RIL_UNSOL_DEDICATE_BEARER_ACTIVATED = (RIL_UNSOL_VENDOR_BASE + 34);
static final int RIL_UNSOL_DEDICATE_BEARER_MODIFIED = (RIL_UNSOL_VENDOR_BASE + 35);
static final int RIL_UNSOL_DEDICATE_BEARER_DEACTIVATED = (RIL_UNSOL_VENDOR_BASE + 36);
static final int RIL_UNSOL_RAC_UPDATE = (RIL_UNSOL_VENDOR_BASE + 37);
static final int RIL_UNSOL_ECONF_RESULT_INDICATION = (RIL_UNSOL_VENDOR_BASE + 38);
static final int RIL_UNSOL_MELOCK_NOTIFICATION = (RIL_UNSOL_VENDOR_BASE + 39);
static final int RIL_UNSOL_CALL_FORWARDING = (RIL_UNSOL_VENDOR_BASE + 40);
static final int RIL_UNSOL_CRSS_NOTIFICATION = (RIL_UNSOL_VENDOR_BASE + 41);
static final int RIL_UNSOL_INCOMING_CALL_INDICATION = (RIL_UNSOL_VENDOR_BASE + 42);
static final int RIL_UNSOL_CIPHER_INDICATION = (RIL_UNSOL_VENDOR_BASE + 43);
static final int RIL_UNSOL_CNAP = (RIL_UNSOL_VENDOR_BASE + 44);
static final int RIL_UNSOL_SIM_COMMON_SLOT_NO_CHANGED = (RIL_UNSOL_VENDOR_BASE + 45);
static final int RIL_UNSOL_DATA_ALLOWED = (RIL_UNSOL_VENDOR_BASE + 46);
static final int RIL_UNSOL_STK_CALL_CTRL = (RIL_UNSOL_VENDOR_BASE + 47);
static final int RIL_UNSOL_VOLTE_EPS_NETWORK_FEATURE_SUPPORT = (RIL_UNSOL_VENDOR_BASE + 48);
static final int RIL_UNSOL_CALL_INFO_INDICATION = (RIL_UNSOL_VENDOR_BASE + 49);
static final int RIL_UNSOL_VOLTE_EPS_NETWORK_FEATURE_INFO = (RIL_UNSOL_VENDOR_BASE + 50);
static final int RIL_UNSOL_SRVCC_HANDOVER_INFO_INDICATION = (RIL_UNSOL_VENDOR_BASE + 51);
static final int RIL_UNSOL_SPEECH_CODEC_INFO = (RIL_UNSOL_VENDOR_BASE + 52);
static final int RIL_UNSOL_MD_STATE_CHANGE = (RIL_UNSOL_VENDOR_BASE + 53);
static final int RIL_UNSOL_REMOVE_RESTRICT_EUTRAN = (RIL_UNSOL_VENDOR_BASE + 54);
static final int RIL_UNSOL_MO_DATA_BARRING_INFO = (RIL_UNSOL_VENDOR_BASE + 55);
static final int RIL_UNSOL_SSAC_BARRING_INFO = (RIL_UNSOL_VENDOR_BASE + 56);
static final int RIL_UNSOL_SIP_CALL_PROGRESS_INDICATOR = (RIL_UNSOL_VENDOR_BASE + 57);
static final int RIL_UNSOL_ABNORMAL_EVENT = (RIL_UNSOL_VENDOR_BASE + 58);
static final int RIL_UNSOL_CDMA_CARD_TYPE = (RIL_UNSOL_VENDOR_BASE + 59);
static final int RIL_UNSOL_INTER_3GPP_IRAT_STATE_CHANGE = (RIL_UNSOL_VENDOR_BASE + 60);
static final int RIL_UNSOL_LTE_BG_SEARCH_STATUS = (RIL_UNSOL_VENDOR_BASE + 61);
static final int RIL_UNSOL_GMSS_RAT_CHANGED = (RIL_UNSOL_VENDOR_BASE + 62);

static int registered = 0;

    public SproutRIL(Context context, int networkMode, int cdmaSubscription) {
        super(context, networkMode, cdmaSubscription, null);
    }

    public SproutRIL(Context context, int networkMode, int cdmaSubscription, Integer instanceId) {
        super(context, networkMode, cdmaSubscription, instanceId);
    }

    private static int readRilMessage(InputStream is, byte[] buffer)
            throws IOException {
        int countRead;
        int offset;
        int remaining;
        int messageLength;

        // First, read in the length of the message
        offset = 0;
        remaining = 4;
        do {
            countRead = is.read(buffer, offset, remaining);

            if (countRead < 0 ) {
                Rlog.e(RILJ_LOG_TAG, "Hit EOS reading message length");
                return -1;
            }

            offset += countRead;
            remaining -= countRead;
        } while (remaining > 0);

        messageLength = ((buffer[0] & 0xff) << 24)
                | ((buffer[1] & 0xff) << 16)
                | ((buffer[2] & 0xff) << 8)
                | (buffer[3] & 0xff);

        // Then, re-use the buffer and read in the message itself
        offset = 0;
        remaining = messageLength;
        do {
            countRead = is.read(buffer, offset, remaining);

            if (countRead < 0 ) {
                Rlog.e(RILJ_LOG_TAG, "Hit EOS reading message.  messageLength=" + messageLength
                        + " remaining=" + remaining);
                return -1;
            }

            offset += countRead;
            remaining -= countRead;
        } while (remaining > 0);

        return messageLength;
    }

    protected RILReceiver createRILReceiver() {
        return new MTKRILReceiver();
    }

    protected class MTKRILReceiver extends RILReceiver {
        byte[] buffer;

        protected MTKRILReceiver() {
            buffer = new byte[RIL_MAX_COMMAND_BYTES];
        }

        @Override
        public void
        run() {
            int retryCount = 0;
            String rilSocket = "rild";

            try {for (;;) {
                LocalSocket s = null;
                LocalSocketAddress l;

                if (mInstanceId == null || mInstanceId == 0 ) {
                    rilSocket = SOCKET_NAME_RIL[0];
                } else {
                    rilSocket = SOCKET_NAME_RIL[mInstanceId];
                }

                int currentSim;

                if (mInstanceId == null || mInstanceId ==0) {
                currentSim = 0;
                } else {
                currentSim = mInstanceId;
                }


                int m3GsimId = 0;
                m3GsimId =  SystemProperties.getInt("gsm.3gswitch", 0);
                if((m3GsimId > 0) && (m3GsimId <= 2)) {
                --m3GsimId;
                } else {
                m3GsimId=0;
                }

                if (m3GsimId >= 1) {
                    if (currentSim == 0) {
                       rilSocket = SOCKET_NAME_RIL[m3GsimId];
                    }
                    else if(currentSim == m3GsimId) {
                       rilSocket = SOCKET_NAME_RIL[0];
                    }
                    if (RILJ_LOGD) riljLog("Capability switched, swap sockets [" + currentSim + ", " + rilSocket + "]");
                }

                try {
                    s = new LocalSocket();
                    l = new LocalSocketAddress(rilSocket,
                            LocalSocketAddress.Namespace.RESERVED);
                    s.connect(l);
                } catch (IOException ex){
                    try {
                        if (s != null) {
                            s.close();
                        }
                    } catch (IOException ex2) {
                        //ignore failure to close after failure to connect
                    }

                    // don't print an error message after the the first time
                    // or after the 8th time

                    if (retryCount == 8) {
                        Rlog.e (RILJ_LOG_TAG,
                            "Couldn't find '" + rilSocket
                            + "' socket after " + retryCount
                            + " times, continuing to retry silently");
                    } else if (retryCount > 0 && retryCount < 8) {
                        Rlog.i (RILJ_LOG_TAG,
                            "Couldn't find '" + rilSocket
                            + "' socket; retrying after timeout");
                    }

                    try {
                        Thread.sleep(SOCKET_OPEN_RETRY_MILLIS);
                    } catch (InterruptedException er) {
                    }

                    retryCount++;
                    continue;
                }

                retryCount = 0;

                mSocket = s;
                Rlog.i(RILJ_LOG_TAG, "Connected to '" + rilSocket + "' socket");

                /* Compatibility with qcom's DSDS (Dual SIM) stack */
                if (needsOldRilFeature("qcomdsds")) {
                    String str = "SUB1";
                    byte[] data = str.getBytes();
                    try {
                        mSocket.getOutputStream().write(data);
                        Rlog.i(RILJ_LOG_TAG, "Data sent!!");
                    } catch (IOException ex) {
                            Rlog.e(RILJ_LOG_TAG, "IOException", ex);
                    } catch (RuntimeException exc) {
                        Rlog.e(RILJ_LOG_TAG, "Uncaught exception ", exc);
                    }
                }

                int length = 0;
                try {
                    InputStream is = mSocket.getInputStream();

                    for (;;) {
                        Parcel p;

                        length = readRilMessage(is, buffer);

                        if (length < 0) {
                            // End-of-stream reached
                            break;
                        }

                        p = Parcel.obtain();
                        p.unmarshall(buffer, 0, length);
                        p.setDataPosition(0);

                        //Rlog.v(RILJ_LOG_TAG, "Read packet: " + length + " bytes");

                        processResponse(p);
                        p.recycle();
                    }
                } catch (java.io.IOException ex) {
                    Rlog.i(RILJ_LOG_TAG, "'" + rilSocket + "' socket closed",
                          ex);
                } catch (Throwable tr) {
                    Rlog.e(RILJ_LOG_TAG, "Uncaught exception read length=" + length +
                        "Exception:" + tr.toString());
                }

                Rlog.i(RILJ_LOG_TAG, "Disconnected from '" + rilSocket
                      + "' socket");

                setRadioState (RadioState.RADIO_UNAVAILABLE);

                try {
                    mSocket.close();
                } catch (IOException ex) {
                }

                mSocket = null;
                RILRequest.resetSerial();

                // Clear request list on close
                clearRequestList(RADIO_NOT_AVAILABLE, false);
            }} catch (Throwable tr) {
                Rlog.e(RILJ_LOG_TAG,"Uncaught exception", tr);
            }

            /* We're disconnected so we don't know the ril version */
            notifyRegistrantsRilConnectionChanged(-1);
        }
    }

   protected RILRequest
    processSolicited (Parcel p) {
        int serial, error;
        boolean found = false;

        serial = p.readInt();
        error = p.readInt();

        RILRequest rr;

        rr = findAndRemoveRequestFromList(serial);

        if (rr == null) {
            Rlog.w(RILJ_LOG_TAG, "Unexpected solicited response! sn: "
                            + serial + " error: " + error);
            return null;
        }

        Object ret = null;

        if (error == 0 || p.dataAvail() > 0) {
            // either command succeeds or command fails but with data payload
            try {switch (rr.mRequest) {
            /*
 cat libs/telephony/ril_commands.h \
 | egrep "^ *{RIL_" \
 | sed -re 's/\{([^,]+),[^,]+,([^}]+).+/case \1: ret = \2(p); break;/'
             */
            case RIL_REQUEST_GET_SIM_STATUS: ret =  responseIccCardStatus(p); break;
            case RIL_REQUEST_ENTER_SIM_PIN: ret =  responseInts(p); break;
            case RIL_REQUEST_ENTER_SIM_PUK: ret =  responseInts(p); break;
            case RIL_REQUEST_ENTER_SIM_PIN2: ret =  responseInts(p); break;
            case RIL_REQUEST_ENTER_SIM_PUK2: ret =  responseInts(p); break;
            case RIL_REQUEST_CHANGE_SIM_PIN: ret =  responseInts(p); break;
            case RIL_REQUEST_CHANGE_SIM_PIN2: ret =  responseInts(p); break;
            case RIL_REQUEST_ENTER_NETWORK_DEPERSONALIZATION: ret =  responseInts(p); break;
            case RIL_REQUEST_GET_CURRENT_CALLS: ret =  responseCallList(p); break;
            case RIL_REQUEST_DIAL: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_IMSI: ret =  responseString(p); break;
            case RIL_REQUEST_HANGUP: ret =  responseVoid(p); break;
            case RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND: ret =  responseVoid(p); break;
            case RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND: {
                if (mTestingEmergencyCall.getAndSet(false)) {
                    if (mEmergencyCallbackModeRegistrant != null) {
                        riljLog("testing emergency call, notify ECM Registrants");
                        mEmergencyCallbackModeRegistrant.notifyRegistrant();
                    }
                }
                ret =  responseVoid(p);
                break;
            }
            case RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE: ret =  responseVoid(p); break;
            case RIL_REQUEST_CONFERENCE: ret =  responseVoid(p); break;
            case RIL_REQUEST_UDUB: ret =  responseVoid(p); break;
            case RIL_REQUEST_LAST_CALL_FAIL_CAUSE: ret =  responseFailCause(p); break;
            case RIL_REQUEST_SIGNAL_STRENGTH: ret =  responseSignalStrength(p); break;
            case RIL_REQUEST_VOICE_REGISTRATION_STATE: ret =  responseStrings(p); break;
            case RIL_REQUEST_DATA_REGISTRATION_STATE: ret =  responseStrings(p); break;
            case RIL_REQUEST_OPERATOR: ret =  responseStrings(p); break;
            case RIL_REQUEST_RADIO_POWER: ret =  responseVoid(p); break;
            case RIL_REQUEST_DTMF: ret =  responseVoid(p); break;
            case RIL_REQUEST_SEND_SMS: ret =  responseSMS(p); break;
            case RIL_REQUEST_SEND_SMS_EXPECT_MORE: ret =  responseSMS(p); break;
            case RIL_REQUEST_SETUP_DATA_CALL: ret =  responseSetupDataCall(p); break;
            case RIL_REQUEST_SIM_IO: ret =  responseICC_IO(p); break;
            case RIL_REQUEST_SEND_USSD: ret =  responseVoid(p); break;
            case RIL_REQUEST_CANCEL_USSD: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_CLIR: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_CLIR: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_CALL_FORWARD_STATUS: ret =  responseCallForward(p); break;
            case RIL_REQUEST_SET_CALL_FORWARD: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_CALL_WAITING: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_CALL_WAITING: ret =  responseVoid(p); break;
            case RIL_REQUEST_SMS_ACKNOWLEDGE: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_IMEI: ret =  responseString(p); break;
            case RIL_REQUEST_GET_IMEISV: ret =  responseString(p); break;
            case RIL_REQUEST_ANSWER: ret =  responseVoid(p); break;
            case RIL_REQUEST_DEACTIVATE_DATA_CALL: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_FACILITY_LOCK: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_FACILITY_LOCK: ret =  responseInts(p); break;
            case RIL_REQUEST_CHANGE_BARRING_PASSWORD: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_AVAILABLE_NETWORKS : ret =  responseOperatorInfos(p); break;
            case RIL_REQUEST_DTMF_START: ret =  responseVoid(p); break;
            case RIL_REQUEST_DTMF_STOP: ret =  responseVoid(p); break;
            case RIL_REQUEST_BASEBAND_VERSION: ret =  responseString(p); break;
            case RIL_REQUEST_SEPARATE_CONNECTION: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_MUTE: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_MUTE: ret =  responseInts(p); break;
            case RIL_REQUEST_QUERY_CLIP: ret =  responseInts(p); break;
            case RIL_REQUEST_LAST_DATA_CALL_FAIL_CAUSE: ret =  responseInts(p); break;
            case RIL_REQUEST_DATA_CALL_LIST: ret =  responseDataCallList(p); break;
            case RIL_REQUEST_RESET_RADIO: ret =  responseVoid(p); break;
            case RIL_REQUEST_OEM_HOOK_RAW: ret =  responseRaw(p); break;
            case RIL_REQUEST_OEM_HOOK_STRINGS: ret =  responseStrings(p); break;
            case RIL_REQUEST_SCREEN_STATE: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_SUPP_SVC_NOTIFICATION: ret =  responseVoid(p); break;
            case RIL_REQUEST_WRITE_SMS_TO_SIM: ret =  responseInts(p); break;
            case RIL_REQUEST_DELETE_SMS_ON_SIM: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_BAND_MODE: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE: ret =  responseInts(p); break;
            case RIL_REQUEST_STK_GET_PROFILE: ret =  responseString(p); break;
            case RIL_REQUEST_STK_SET_PROFILE: ret =  responseVoid(p); break;
            case RIL_REQUEST_STK_SEND_ENVELOPE_COMMAND: ret =  responseString(p); break;
            case RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE: ret =  responseVoid(p); break;
            case RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM: ret =  responseInts(p); break;
            case RIL_REQUEST_EXPLICIT_CALL_TRANSFER: ret =  responseVoid(p); break;
            case RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE: ret =  responseVoid(p); break;
            case RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE: ret =  responseGetPreferredNetworkType(p); break;
            case RIL_REQUEST_GET_NEIGHBORING_CELL_IDS: ret = responseCellList(p); break;
            case RIL_REQUEST_SET_LOCATION_UPDATES: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE: ret =  responseInts(p); break;
            case RIL_REQUEST_SET_TTY_MODE: ret =  responseVoid(p); break;
            case RIL_REQUEST_QUERY_TTY_MODE: ret =  responseInts(p); break;
            case RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE: ret =  responseInts(p); break;
            case RIL_REQUEST_CDMA_FLASH: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_BURST_DTMF: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_SEND_SMS: ret =  responseSMS(p); break;
            case RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE: ret =  responseVoid(p); break;
            case RIL_REQUEST_GSM_GET_BROADCAST_CONFIG: ret =  responseGmsBroadcastConfig(p); break;
            case RIL_REQUEST_GSM_SET_BROADCAST_CONFIG: ret =  responseVoid(p); break;
            case RIL_REQUEST_GSM_BROADCAST_ACTIVATION: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG: ret =  responseCdmaBroadcastConfig(p); break;
            case RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_BROADCAST_ACTIVATION: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY: ret =  responseVoid(p); break;
            case RIL_REQUEST_CDMA_SUBSCRIPTION: ret =  responseStrings(p); break;
            case RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM: ret =  responseInts(p); break;
            case RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM: ret =  responseVoid(p); break;
            case RIL_REQUEST_DEVICE_IDENTITY: ret =  responseStrings(p); break;
            case RIL_REQUEST_GET_SMSC_ADDRESS: ret = responseString(p); break;
            case RIL_REQUEST_SET_SMSC_ADDRESS: ret = responseVoid(p); break;
            case RIL_REQUEST_EXIT_EMERGENCY_CALLBACK_MODE: ret = responseVoid(p); break;
            case RIL_REQUEST_REPORT_SMS_MEMORY_STATUS: ret = responseVoid(p); break;
            case RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING: ret = responseVoid(p); break;
            case RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE: ret =  responseInts(p); break;
            case RIL_REQUEST_ISIM_AUTHENTICATION: ret =  responseString(p); break;
            case RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU: ret = responseVoid(p); break;
            case RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS: ret = responseICC_IO(p); break;
            case RIL_REQUEST_VOICE_RADIO_TECH: ret = responseInts(p); break;
            case RIL_REQUEST_GET_CELL_INFO_LIST: ret = responseCellInfoList(p); break;
            case RIL_REQUEST_SET_UNSOL_CELL_INFO_LIST_RATE: ret = responseVoid(p); break;
            case RIL_REQUEST_SET_INITIAL_ATTACH_APN: ret = responseVoid(p); break;
            case RIL_REQUEST_IMS_REGISTRATION_STATE: ret = responseInts(p); break;
            case RIL_REQUEST_IMS_SEND_SMS: ret =  responseSMS(p); break;
            case RIL_REQUEST_SIM_TRANSMIT_APDU_BASIC: ret =  responseICC_IO(p); break;
            case RIL_REQUEST_SIM_OPEN_CHANNEL: ret  = responseInts(p); break;
            case RIL_REQUEST_SIM_CLOSE_CHANNEL: ret  = responseVoid(p); break;
            case RIL_REQUEST_SIM_TRANSMIT_APDU_CHANNEL: ret = responseICC_IO(p); break;
            case RIL_REQUEST_SIM_GET_ATR: ret = responseString(p); break;
            case RIL_REQUEST_NV_READ_ITEM: ret = responseString(p); break;
            case RIL_REQUEST_SET_3G_CAPABILITY: ret =  responseInts(p); break;
            case RIL_REQUEST_NV_WRITE_ITEM: ret = responseVoid(p); break;
            case RIL_REQUEST_NV_WRITE_CDMA_PRL: ret = responseVoid(p); break;
            case RIL_REQUEST_NV_RESET_CONFIG: ret = responseVoid(p); break;
            case RIL_REQUEST_SET_UICC_SUBSCRIPTION: ret = responseVoid(p); break;
            case RIL_REQUEST_ALLOW_DATA: ret = responseVoid(p); break;
            case RIL_REQUEST_GET_HARDWARE_CONFIG: ret = responseHardwareConfig(p); break;
            case RIL_REQUEST_SHUTDOWN: ret = responseVoid(p); break;
            case RIL_REQUEST_GET_RADIO_CAPABILITY: ret =  responseRadioCapability(p); break;
            case RIL_REQUEST_SET_RADIO_CAPABILITY: ret =  responseRadioCapability(p); break;
            case RIL_REQUEST_START_LCE: ret = responseLceStatus(p); break;
            case RIL_REQUEST_STOP_LCE: ret = responseLceStatus(p); break;
            case RIL_REQUEST_PULL_LCEDATA: ret = responseLceData(p); break;
            case RIL_REQUEST_GET_ACTIVITY_INFO: ret = responseActivityData(p); break;
            default:
                throw new RuntimeException("Unrecognized solicited response: " + rr.mRequest);
            //break;
            }} catch (Throwable tr) {
                // Exceptions here usually mean invalid RIL responses

                Rlog.w(RILJ_LOG_TAG, rr.serialString() + "< "
                        + requestToString(rr.mRequest)
                        + " exception, possible invalid RIL response", tr);

                if (rr.mResult != null) {
                    AsyncResult.forMessage(rr.mResult, null, tr);
                    rr.mResult.sendToTarget();
                }
                return rr;
            }
        }

        if (rr.mRequest == RIL_REQUEST_SHUTDOWN) {
            // Set RADIO_STATE to RADIO_UNAVAILABLE to continue shutdown process
            // regardless of error code to continue shutdown procedure.
            riljLog("Response to RIL_REQUEST_SHUTDOWN received. Error is " +
                    error + " Setting Radio State to Unavailable regardless of error.");
            setRadioState(RadioState.RADIO_UNAVAILABLE);
        }

        // Here and below fake RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED, see b/7255789.
        // This is needed otherwise we don't automatically transition to the main lock
        // screen when the pin or puk is entered incorrectly.
        switch (rr.mRequest) {
            case RIL_REQUEST_ENTER_SIM_PUK:
            case RIL_REQUEST_ENTER_SIM_PUK2:
                if (mIccStatusChangedRegistrants != null) {
                    if (RILJ_LOGD) {
                        riljLog("ON enter sim puk fakeSimStatusChanged: reg count="
                                + mIccStatusChangedRegistrants.size());
                    }
                    mIccStatusChangedRegistrants.notifyRegistrants();
                }
                break;
        }

        if (error != 0) {
            switch (rr.mRequest) {
                case RIL_REQUEST_ENTER_SIM_PIN:
                case RIL_REQUEST_ENTER_SIM_PIN2:
                case RIL_REQUEST_CHANGE_SIM_PIN:
                case RIL_REQUEST_CHANGE_SIM_PIN2:
                case RIL_REQUEST_SET_FACILITY_LOCK:
                    if (mIccStatusChangedRegistrants != null) {
                        if (RILJ_LOGD) {
                            riljLog("ON some errors fakeSimStatusChanged: reg count="
                                    + mIccStatusChangedRegistrants.size());
                        }
                        mIccStatusChangedRegistrants.notifyRegistrants();
                    }
                    break;
                case RIL_REQUEST_GET_RADIO_CAPABILITY: {
                    // Ideally RIL's would support this or at least give NOT_SUPPORTED
                    // but the hammerhead RIL reports GENERIC :(
                    // TODO - remove GENERIC_FAILURE catching: b/21079604
                    if (REQUEST_NOT_SUPPORTED == error ||
                            GENERIC_FAILURE == error) {
                        // we should construct the RAF bitmask the radio
                        // supports based on preferred network bitmasks
                        ret = makeStaticRadioCapability();
                        error = 0;
                    }
                    break;
                }
            }

            if (error != 0) rr.onError(error, ret);
        }
        if (error == 0) {

            if (RILJ_LOGD) riljLog(rr.serialString() + "< " + requestToString(rr.mRequest)
                    + " " + retToString(rr.mRequest, ret));

            if (rr.mResult != null) {
                AsyncResult.forMessage(rr.mResult, ret, null);
                rr.mResult.sendToTarget();
            }
        }
        return rr;
    }

    @Override
    protected void
    processUnsolicited (Parcel p) {
        Object ret;
        int dataPosition = p.dataPosition(); // save off position within the Parcel
        int response = p.readInt();

        switch(response) {
            case RIL_UNSOL_NEIGHBORING_CELL_INFO: ret = responseStrings(p); break;          
            case RIL_UNSOL_NETWORK_INFO: ret = responseStrings(p); break;           
            case RIL_UNSOL_CALL_FORWARDING: ret = responseInts(p); break;
            case RIL_UNSOL_CRSS_NOTIFICATION: ret = responseCrssNotification(p); break;
            case RIL_UNSOL_CALL_PROGRESS_INFO: ret = responseStrings(p); break;         
            case RIL_UNSOL_PHB_READY_NOTIFICATION: ret = responseVoid(p); break;
            case RIL_UNSOL_SIM_INSERTED_STATUS: ret = responseInts(p); break;            
            case RIL_UNSOL_SIM_MISSING: ret = responseInts(p); break;   
            case RIL_UNSOL_SIM_RECOVERY: ret = responseInts(p); break;         
            case RIL_UNSOL_VIRTUAL_SIM_ON: ret = responseInts(p); break; 
            case RIL_UNSOL_VIRTUAL_SIM_OFF: ret = responseInts(p); break; 
            case RIL_UNSOL_SPEECH_INFO: ret = responseInts(p); break;           
            case RIL_UNSOL_RADIO_TEMPORARILY_UNAVAILABLE: ret = responseInts(p); break; 
            case RIL_UNSOL_ME_SMS_STORAGE_FULL: ret =  responseVoid(p); break;
            case RIL_UNSOL_SMS_READY_NOTIFICATION: ret = responseVoid(p); break;
            case RIL_UNSOL_VT_STATUS_INFO: ret = responseInts(p); break;
            case RIL_UNSOL_VT_RING_INFO: ret = responseVoid(p); break;
            case RIL_UNSOL_SCRI_RESULT: ret = responseInts(p); break;
            case RIL_UNSOL_GPRS_DETACH: ret = responseVoid(p); break;
            case RIL_UNSOL_INCOMING_CALL_INDICATION: ret = responseStrings(p); break;
            case RIL_UNSOL_EF_CSP_PLMN_MODE_BIT: ret = responseInts(p); break;
            case RIL_UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED: ret =  responseVoid(p); break;
            case RIL_UNSOL_INVALID_SIM:  ret = responseStrings(p); break;
            case RIL_UNSOL_RESPONSE_ACMT: ret = responseInts(p); break;
            case RIL_UNSOL_IMEI_LOCK: ret = responseVoid(p); break;
            case RIL_UNSOL_RESPONSE_MMRR_STATUS_CHANGED: ret = responseInts(p); break;
            case RIL_UNSOL_SIM_PLUG_OUT: ret = responseInts(p); break;
            case RIL_UNSOL_SIM_PLUG_IN: ret = responseInts(p); break;
            case RIL_UNSOL_RESPONSE_ETWS_NOTIFICATION: ret = responseEtwsNotification(p); break;
            case RIL_UNSOL_CNAP: ret = responseStrings(p); break;
            case RIL_UNSOL_STK_EVDL_CALL: ret = responseInts(p); break;            
            case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED: ret =  responseVoid(p); break;
            default:
                // Rewind the Parcel
                p.setDataPosition(dataPosition);

                // Forward responses that we are not overriding to the super class
                super.processUnsolicited(p);
                return;
        }

        // To avoid duplicating code from RIL.java, we rewrite some response codes to fit
        // AOSP's one (when they do the same effect)
        boolean rewindAndReplace = false;
        int newResponseCode = 0;

        switch (response) {
            case RIL_UNSOL_CALL_PROGRESS_INFO:
		rewindAndReplace = true;
		newResponseCode = RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED;
		break;

            case RIL_UNSOL_INCOMING_CALL_INDICATION:
		setCallIndication((String[])ret);
                rewindAndReplace = true;
		newResponseCode = RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED;
		break;

            case RIL_UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED:
                rewindAndReplace = true;
                newResponseCode = RIL_UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED;
                break;

            case RIL_UNSOL_SIM_INSERTED_STATUS:
            case RIL_UNSOL_SIM_MISSING:
            case RIL_UNSOL_SIM_PLUG_OUT:
            case RIL_UNSOL_SIM_PLUG_IN:
                rewindAndReplace = true;
                newResponseCode = RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED;
                break;

            case RIL_UNSOL_SMS_READY_NOTIFICATION:
                /*if (mGsmSmsRegistrant != null) {
                    mGsmSmsRegistrant
                        .notifyRegistrant();
                }*/
                break;
            case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED:
		// intercept and send GPRS_TRANSFER_TYPE and GPRS_CONNECT_TYPE to RIL
	        setRadioStateFromRILInt(p.readInt());
		rewindAndReplace = true;
		newResponseCode = RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED;
		break;
            default:
                Rlog.i(RILJ_LOG_TAG, "Unprocessed unsolicited known MTK response: " + response);
        }

        if (rewindAndReplace) {
            Rlog.w(RILJ_LOG_TAG, "Rewriting MTK unsolicited response to " + newResponseCode);

            // Rewrite
            p.setDataPosition(dataPosition);
            p.writeInt(newResponseCode);

            // And rewind again in front
            p.setDataPosition(dataPosition);

            super.processUnsolicited(p);
        }
    }
    
    private Object
    responseCrssNotification(Parcel p) {
        /*SuppCrssNotification notification = new SuppCrssNotification();

        notification.code = p.readInt();
        notification.type = p.readInt();
        notification.number = p.readString();
        notification.alphaid = p.readString();
        notification.cli_validity = p.readInt();

        return notification;*/

        Rlog.e(RILJ_LOG_TAG, "NOT PROCESSING CRSS NOTIFICATION");
        return null;
    }
    
    private Object responseEtwsNotification(Parcel p) {
        /*EtwsNotification response = new EtwsNotification();
        
        response.warningType = p.readInt();
        response.messageId = p.readInt();
        response.serialNumber = p.readInt();
        response.plmnId = p.readString();
        response.securityInfo = p.readString();
        
        return response;*/
        Rlog.e(RILJ_LOG_TAG, "NOT PROCESSING ETWS NOTIFICATION");

        return null;
    }
    
    void setCallIndication(String[] incomingCallInfo) {
	RILRequest rr
            = RILRequest.obtain(RIL_REQUEST_SET_CALL_INDICATION, null);

	int callId = Integer.parseInt(incomingCallInfo[0]);
        int callMode = Integer.parseInt(incomingCallInfo[3]);
        int seqNumber = Integer.parseInt(incomingCallInfo[4]);

	// some guess work is needed here, for now, just 0
	callMode = 0;

        rr.mParcel.writeInt(3);

        rr.mParcel.writeInt(callMode);
        rr.mParcel.writeInt(callId);
        rr.mParcel.writeInt(seqNumber);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> "
            + requestToString(rr.mRequest) + " " + callMode + " " + callId + " " + seqNumber);

        send(rr);
    }
    
    private void setRadioStateFromRILInt (int stateCode) {
        switch (stateCode) {
	case 0: case 1: break; // radio off
	default:
	    {
	        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_GPRS_TRANSFER_TYPE, null);

		if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

		rr.mParcel.writeInt(1);
		rr.mParcel.writeInt(1);

		send(rr);
	    }
	    {
	        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_GPRS_CONNECT_TYPE, null);

		if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

		rr.mParcel.writeInt(1);
		rr.mParcel.writeInt(1);

		send(rr);
	    }
	}
    }
    
    static String
    requestToString(int request) {
/*
 cat libs/telephony/ril_commands.h \
 | egrep "^ *{RIL_" \
 | sed -re 's/\{RIL_([^,]+),[^,]+,([^}]+).+/case RIL_\1: return "\1";/'
*/
        switch(request) {
            case RIL_REQUEST_GET_SIM_STATUS: return "GET_SIM_STATUS";
            case RIL_REQUEST_ENTER_SIM_PIN: return "ENTER_SIM_PIN";
            case RIL_REQUEST_ENTER_SIM_PUK: return "ENTER_SIM_PUK";
            case RIL_REQUEST_ENTER_SIM_PIN2: return "ENTER_SIM_PIN2";
            case RIL_REQUEST_ENTER_SIM_PUK2: return "ENTER_SIM_PUK2";
            case RIL_REQUEST_CHANGE_SIM_PIN: return "CHANGE_SIM_PIN";
            case RIL_REQUEST_CHANGE_SIM_PIN2: return "CHANGE_SIM_PIN2";
            case RIL_REQUEST_ENTER_NETWORK_DEPERSONALIZATION: return "ENTER_NETWORK_DEPERSONALIZATION";
            case RIL_REQUEST_GET_CURRENT_CALLS: return "GET_CURRENT_CALLS";
            case RIL_REQUEST_DIAL: return "DIAL";
            case RIL_REQUEST_GET_IMSI: return "GET_IMSI";
            case RIL_REQUEST_HANGUP: return "HANGUP";
            case RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND: return "HANGUP_WAITING_OR_BACKGROUND";
            case RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND: return "HANGUP_FOREGROUND_RESUME_BACKGROUND";
            case RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE: return "REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE";
            case RIL_REQUEST_CONFERENCE: return "CONFERENCE";
            case RIL_REQUEST_UDUB: return "UDUB";
            case RIL_REQUEST_LAST_CALL_FAIL_CAUSE: return "LAST_CALL_FAIL_CAUSE";
            case RIL_REQUEST_SIGNAL_STRENGTH: return "SIGNAL_STRENGTH";
            case RIL_REQUEST_VOICE_REGISTRATION_STATE: return "VOICE_REGISTRATION_STATE";
            case RIL_REQUEST_DATA_REGISTRATION_STATE: return "DATA_REGISTRATION_STATE";
            case RIL_REQUEST_OPERATOR: return "OPERATOR";
            case RIL_REQUEST_RADIO_POWER: return "RADIO_POWER";
            case RIL_REQUEST_DTMF: return "DTMF";
            case RIL_REQUEST_SEND_SMS: return "SEND_SMS";
            case RIL_REQUEST_SEND_SMS_EXPECT_MORE: return "SEND_SMS_EXPECT_MORE";
            case RIL_REQUEST_SETUP_DATA_CALL: return "SETUP_DATA_CALL";
            case RIL_REQUEST_SIM_IO: return "SIM_IO";
            case RIL_REQUEST_SEND_USSD: return "SEND_USSD";
            case RIL_REQUEST_CANCEL_USSD: return "CANCEL_USSD";
            case RIL_REQUEST_GET_CLIR: return "GET_CLIR";
            case RIL_REQUEST_SET_CLIR: return "SET_CLIR";
            case RIL_REQUEST_QUERY_CALL_FORWARD_STATUS: return "QUERY_CALL_FORWARD_STATUS";
            case RIL_REQUEST_SET_CALL_FORWARD: return "SET_CALL_FORWARD";
            case RIL_REQUEST_QUERY_CALL_WAITING: return "QUERY_CALL_WAITING";
            case RIL_REQUEST_SET_CALL_WAITING: return "SET_CALL_WAITING";
            case RIL_REQUEST_SMS_ACKNOWLEDGE: return "SMS_ACKNOWLEDGE";
            case RIL_REQUEST_GET_IMEI: return "GET_IMEI";
            case RIL_REQUEST_GET_IMEISV: return "GET_IMEISV";
            case RIL_REQUEST_ANSWER: return "ANSWER";
            case RIL_REQUEST_DEACTIVATE_DATA_CALL: return "DEACTIVATE_DATA_CALL";
            case RIL_REQUEST_QUERY_FACILITY_LOCK: return "QUERY_FACILITY_LOCK";
            case RIL_REQUEST_SET_FACILITY_LOCK: return "SET_FACILITY_LOCK";
            case RIL_REQUEST_CHANGE_BARRING_PASSWORD: return "CHANGE_BARRING_PASSWORD";
            case RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE: return "QUERY_NETWORK_SELECTION_MODE";
            case RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC: return "SET_NETWORK_SELECTION_AUTOMATIC";
            case RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL: return "SET_NETWORK_SELECTION_MANUAL";
            case RIL_REQUEST_QUERY_AVAILABLE_NETWORKS : return "QUERY_AVAILABLE_NETWORKS ";
            case RIL_REQUEST_DTMF_START: return "DTMF_START";
            case RIL_REQUEST_DTMF_STOP: return "DTMF_STOP";
            case RIL_REQUEST_BASEBAND_VERSION: return "BASEBAND_VERSION";
            case RIL_REQUEST_SEPARATE_CONNECTION: return "SEPARATE_CONNECTION";
            case RIL_REQUEST_SET_MUTE: return "SET_MUTE";
            case RIL_REQUEST_GET_MUTE: return "GET_MUTE";
            case RIL_REQUEST_QUERY_CLIP: return "QUERY_CLIP";
            case RIL_REQUEST_LAST_DATA_CALL_FAIL_CAUSE: return "LAST_DATA_CALL_FAIL_CAUSE";
            case RIL_REQUEST_DATA_CALL_LIST: return "DATA_CALL_LIST";
            case RIL_REQUEST_RESET_RADIO: return "RESET_RADIO";
            case RIL_REQUEST_OEM_HOOK_RAW: return "OEM_HOOK_RAW";
            case RIL_REQUEST_OEM_HOOK_STRINGS: return "OEM_HOOK_STRINGS";
            case RIL_REQUEST_SCREEN_STATE: return "SCREEN_STATE";
            case RIL_REQUEST_SET_SUPP_SVC_NOTIFICATION: return "SET_SUPP_SVC_NOTIFICATION";
            case RIL_REQUEST_WRITE_SMS_TO_SIM: return "WRITE_SMS_TO_SIM";
            case RIL_REQUEST_DELETE_SMS_ON_SIM: return "DELETE_SMS_ON_SIM";
            case RIL_REQUEST_SET_BAND_MODE: return "SET_BAND_MODE";
            case RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE: return "QUERY_AVAILABLE_BAND_MODE";
            case RIL_REQUEST_STK_GET_PROFILE: return "REQUEST_STK_GET_PROFILE";
            case RIL_REQUEST_STK_SET_PROFILE: return "REQUEST_STK_SET_PROFILE";
            case RIL_REQUEST_STK_SEND_ENVELOPE_COMMAND: return "REQUEST_STK_SEND_ENVELOPE_COMMAND";
            case RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE: return "REQUEST_STK_SEND_TERMINAL_RESPONSE";
            case RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM: return "REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM";
            case RIL_REQUEST_EXPLICIT_CALL_TRANSFER: return "REQUEST_EXPLICIT_CALL_TRANSFER";
            case RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE: return "REQUEST_SET_PREFERRED_NETWORK_TYPE";
            case RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE: return "REQUEST_GET_PREFERRED_NETWORK_TYPE";
            case RIL_REQUEST_GET_NEIGHBORING_CELL_IDS: return "REQUEST_GET_NEIGHBORING_CELL_IDS";
            case RIL_REQUEST_SET_LOCATION_UPDATES: return "REQUEST_SET_LOCATION_UPDATES";
            case RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE: return "RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE";
            case RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE: return "RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE";
            case RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE: return "RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE";
            case RIL_REQUEST_SET_TTY_MODE: return "RIL_REQUEST_SET_TTY_MODE";
            case RIL_REQUEST_QUERY_TTY_MODE: return "RIL_REQUEST_QUERY_TTY_MODE";
            case RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE: return "RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE";
            case RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE: return "RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE";
            case RIL_REQUEST_CDMA_FLASH: return "RIL_REQUEST_CDMA_FLASH";
            case RIL_REQUEST_CDMA_BURST_DTMF: return "RIL_REQUEST_CDMA_BURST_DTMF";
            case RIL_REQUEST_CDMA_SEND_SMS: return "RIL_REQUEST_CDMA_SEND_SMS";
            case RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE: return "RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE";
            case RIL_REQUEST_GSM_GET_BROADCAST_CONFIG: return "RIL_REQUEST_GSM_GET_BROADCAST_CONFIG";
            case RIL_REQUEST_GSM_SET_BROADCAST_CONFIG: return "RIL_REQUEST_GSM_SET_BROADCAST_CONFIG";
            case RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG: return "RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG";
            case RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG: return "RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG";
            case RIL_REQUEST_GSM_BROADCAST_ACTIVATION: return "RIL_REQUEST_GSM_BROADCAST_ACTIVATION";
            case RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY: return "RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY";
            case RIL_REQUEST_CDMA_BROADCAST_ACTIVATION: return "RIL_REQUEST_CDMA_BROADCAST_ACTIVATION";
            case RIL_REQUEST_CDMA_SUBSCRIPTION: return "RIL_REQUEST_CDMA_SUBSCRIPTION";
            case RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM: return "RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM";
            case RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM: return "RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM";
            case RIL_REQUEST_DEVICE_IDENTITY: return "RIL_REQUEST_DEVICE_IDENTITY";
            case RIL_REQUEST_GET_SMSC_ADDRESS: return "RIL_REQUEST_GET_SMSC_ADDRESS";
            case RIL_REQUEST_SET_SMSC_ADDRESS: return "RIL_REQUEST_SET_SMSC_ADDRESS";
            case RIL_REQUEST_EXIT_EMERGENCY_CALLBACK_MODE: return "REQUEST_EXIT_EMERGENCY_CALLBACK_MODE";
            case RIL_REQUEST_REPORT_SMS_MEMORY_STATUS: return "RIL_REQUEST_REPORT_SMS_MEMORY_STATUS";
            case RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING: return "RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING";
            case RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE: return "RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE";
            case RIL_REQUEST_ISIM_AUTHENTICATION: return "RIL_REQUEST_ISIM_AUTHENTICATION";
            case RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU: return "RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU";
            case RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS: return "RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS";
            case RIL_REQUEST_VOICE_RADIO_TECH: return "RIL_REQUEST_VOICE_RADIO_TECH";
            case RIL_REQUEST_GET_CELL_INFO_LIST: return "RIL_REQUEST_GET_CELL_INFO_LIST";
            case RIL_REQUEST_SET_UNSOL_CELL_INFO_LIST_RATE: return "RIL_REQUEST_SET_CELL_INFO_LIST_RATE";
            case RIL_REQUEST_SET_INITIAL_ATTACH_APN: return "RIL_REQUEST_SET_INITIAL_ATTACH_APN";
            case RIL_REQUEST_IMS_REGISTRATION_STATE: return "RIL_REQUEST_IMS_REGISTRATION_STATE";
            case RIL_REQUEST_IMS_SEND_SMS: return "RIL_REQUEST_IMS_SEND_SMS";
            case RIL_REQUEST_SIM_TRANSMIT_APDU_BASIC: return "RIL_REQUEST_SIM_TRANSMIT_APDU_BASIC";
            case RIL_REQUEST_SIM_OPEN_CHANNEL: return "RIL_REQUEST_SIM_OPEN_CHANNEL";
            case RIL_REQUEST_SIM_CLOSE_CHANNEL: return "RIL_REQUEST_SIM_CLOSE_CHANNEL";
            case RIL_REQUEST_SIM_TRANSMIT_APDU_CHANNEL: return "RIL_REQUEST_SIM_TRANSMIT_APDU_CHANNEL";
            case RIL_REQUEST_NV_READ_ITEM: return "RIL_REQUEST_NV_READ_ITEM";
            case RIL_REQUEST_NV_WRITE_ITEM: return "RIL_REQUEST_NV_WRITE_ITEM";
            case RIL_REQUEST_NV_WRITE_CDMA_PRL: return "RIL_REQUEST_NV_WRITE_CDMA_PRL";
            case RIL_REQUEST_NV_RESET_CONFIG: return "RIL_REQUEST_NV_RESET_CONFIG";
            case RIL_REQUEST_SET_UICC_SUBSCRIPTION: return "RIL_REQUEST_SET_UICC_SUBSCRIPTION";
            case RIL_REQUEST_ALLOW_DATA: return "RIL_REQUEST_ALLOW_DATA";
            case RIL_REQUEST_GET_HARDWARE_CONFIG: return "GET_HARDWARE_CONFIG";
            case RIL_REQUEST_SHUTDOWN: return "RIL_REQUEST_SHUTDOWN";
            case RIL_REQUEST_SET_RADIO_CAPABILITY:
                    return "RIL_REQUEST_SET_RADIO_CAPABILITY";
            case RIL_REQUEST_GET_RADIO_CAPABILITY:
                    return "RIL_REQUEST_GET_RADIO_CAPABILITY";
			case RIL_REQUEST_SET_3G_CAPABILITY: return "RIL_REQUEST_SET_3G_CAPABILITY";
            case RIL_REQUEST_START_LCE: return "RIL_REQUEST_START_LCE";
            case RIL_REQUEST_STOP_LCE: return "RIL_REQUEST_STOP_LCE";
            case RIL_REQUEST_PULL_LCEDATA: return "RIL_REQUEST_PULL_LCEDATA";
            case RIL_REQUEST_GET_ACTIVITY_INFO: return "RIL_REQUEST_GET_ACTIVITY_INFO";
            default: return "<unknown request>";
        }
    }

    @Override
    public void
    getHardwareConfig (Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_HARDWARE_CONFIG, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    protected Object
    responseFailCause(Parcel p) {
        int numInts;
        int response[];

        numInts = p.readInt();
        response = new int[numInts];
        for (int i = 0 ; i < numInts ; i++) {
            response[i] = p.readInt();
        }
        LastCallFailCause failCause = new LastCallFailCause();
        failCause.causeCode = response[0];
        if (p.dataAvail() > 0) {
          failCause.vendorCause = p.readString();
        }
        return failCause;
    }

    public void setDataAllowed(boolean allowed, Message result) {
        int currentSimId = mInstanceId == null ? 0 : mInstanceId;
        int m3gSimId = get3gSimId();
        if((m3gSimId-1) != currentSimId) {
            RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_3G_CAPABILITY, null);
            rr.mParcel.writeInt(1);
            rr.mParcel.writeInt(currentSimId+1);
            send(rr);
            resetRadio(null);
        }
        else {
            Rlog.i(RILJ_LOG_TAG, "Not setting data subscription on same SIM, requested="
			       +(currentSimId+1)+" current="+m3gSimId);
        }
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_ALLOW_DATA, result);
        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + " " + allowed);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(allowed ? 1 : 0);
        send(rr);
    }

	public int get3gSimId() {
	   return SystemProperties.getInt("gsm.3gswitch", 0);
	}
}
