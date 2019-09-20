/*
 *             ______
 *           /      /  /\
 *          /      /  /  \
 *         /      /  /    \
 *        /      /  /     /
 *       /      /  /     /
 *      /      /  /     /  /\
 *     /      /  /     /  /  \
 *    /______/  /     /  /    \
 *    _______  /     /  /      \
 *    \      \ \    /  /       /
 *     \      \ \  /  /       /
 *      \      \ \/  /       /
 *       \      \   /       /
 *        \      \  \      /
 *         \______\  \____/
 *
 *            ForgeRock®
 *            BackStage
 *
 *
 *   https://backstage.forgerock.com/
 *   Copyright (c) 2013-2019 ForgeRock BackStage All rights reserved.
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
