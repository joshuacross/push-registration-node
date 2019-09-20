/*
 *
 *  * The contents of this file are subject to the terms of the Common Development and
 *  * Distribution License (the License). You may not use this file except in compliance with the
 *  * License.
 *  *
 *  * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 *  * specific language governing permission and limitations under the License.
 *  *
 *  * When distributing Covered Software, include this CDDL Header Notice in each file and include
 *  * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 *  * Header, with the fields enclosed by brackets [] replaced by your own identifying
 *  * information: "Portions copyright [year] [name of copyright owner]".
 *  *
 *  * Copyright 2017-2019 ForgeRock AS.
 *  * Portions copyright 2019 Josh Cross
 *
 */

package com.forgerock.backstage.ssoextensions.auth.push;

interface PushConstants {

    String MESSAGE_ID_QR_CODE_KEY = "m";
    String SHARED_SECRET_QR_CODE_KEY = "s";
    String BGCOLOUR_QR_CODE_KEY = "b";
    String REG_QR_CODE_KEY = "r";
    String AUTH_QR_CODE_KEY = "a";
    String IMG_QR_CODE_KEY = "image";
    String LOADBALANCER_DATA_QR_CODE_KEY = "l";
    String CHALLENGE_QR_CODE_KEY = "c";
    String ISSUER_QR_CODE_KEY = "issuer";
    String URI_SCHEME = "pushauth";
    String URI_HOST = "push";
    String URI_PATH = "forgerock";
    String MESSAGE_ID_KEY = "pushMessageId";
    String CHALLENGE_KEY = "challengeKey";
    String PUSH_DEVICE_PROFILE_KEY = "pushDeviceProfile";
    String REGISTER_DEVICE_HELP_TEXT = "Scan the barcode image below with the ForgeRock Authenticator app to register your device with your login.";
    String RECOVERY_CODE_KEY = "recoveryCodes";
    String RECOVERY_CODE_DEVICE_NAME = "recoveryCodeDeviceName";
}
