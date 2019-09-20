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

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.helpers.QRCallbackBuilder;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.DeviceJsonUtils;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.push.UserPushDeviceProfileManager;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.session.SessionCookies;
import org.forgerock.openam.utils.Alphabet;
import org.forgerock.openam.utils.CodeException;
import org.forgerock.openam.utils.RecoveryCodeGenerator;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.encode.Base64url;

import javax.security.auth.callback.Callback;
import java.util.List;
import java.util.Optional;

import static org.forgerock.openam.services.push.PushNotificationConstants.*;

public class PushHelper extends NodeHelper<PushDeviceSettings> {

    private static final int NUM_CODES = 10;

    private final PushRegistrationNode.Config config;
    private final Realm realm;
    private final SessionCookies sessionCookies;
    private final RecoveryCodeGenerator recoveryCodeGenerator;
    private final UserPushDeviceProfileManager userPushDeviceProfileManager;
    private final MessageService messageService;

    @Inject
    public PushHelper(@Assisted Realm realm,
                      @Assisted PushRegistrationNode.Config config,
                      CoreWrapper coreWrapper,
                      JwtBuilderFactory jwtBuilderFactory,
                      Secrets secrets,
                      DeviceJsonUtils<PushDeviceSettings> deviceJsonUtils,
                      SessionCookies sessionCookies,
                      RecoveryCodeGenerator recoveryCodeGenerator,
                      UserPushDeviceProfileManager userPushDeviceProfileManager,
                      MessageService messageService) {
        super(realm, coreWrapper, jwtBuilderFactory, secrets, deviceJsonUtils);
        this.realm = realm;
        this.config = config;
        this.sessionCookies = sessionCookies;
        this.recoveryCodeGenerator = recoveryCodeGenerator;
        this.userPushDeviceProfileManager = userPushDeviceProfileManager;
        this.messageService = messageService;
    }

    Callback createQRCodeCallback(PushDeviceSettings deviceProfile, AMIdentity id, String messageId,
                                  String challenge, TreeContext context) throws NodeProcessException {
        try {
            QRCallbackBuilder builder = new QRCallbackBuilder().withUriScheme(PushConstants.URI_SCHEME)
                    .withUriHost(PushConstants.URI_HOST)
                    .withUriPath(PushConstants.URI_PATH)
                    .withUriPort(id.getName())
                    .withCallbackIndex(0)
                    .addUriQueryComponent(PushConstants.LOADBALANCER_DATA_QR_CODE_KEY,
                            Base64url.encode(sessionCookies.getLBCookie().getBytes()))
                    .addUriQueryComponent(PushConstants.ISSUER_QR_CODE_KEY, Base64url.encode(config.issuer().getBytes()))
                    .addUriQueryComponent(PushConstants.MESSAGE_ID_QR_CODE_KEY, messageId)
                    .addUriQueryComponent(PushConstants.SHARED_SECRET_QR_CODE_KEY,
                            Base64url.encode(Base64.decode(deviceProfile.getSharedSecret())))
                    .addUriQueryComponent(PushConstants.BGCOLOUR_QR_CODE_KEY, config.color())
                    .addUriQueryComponent(PushConstants.CHALLENGE_QR_CODE_KEY, Base64url.encode(Base64.decode(challenge)))
                    .addUriQueryComponent(PushConstants.REG_QR_CODE_KEY,
                            Base64url.encode(messageService.getAbsoluteServiceAddressUrl(context).getBytes()))
                    .addUriQueryComponent(PushConstants.AUTH_QR_CODE_KEY,
                            Base64url.encode(messageService.getAbsoluteServiceAddressUrl(context).getBytes()));

            if (config.imgUrl() != null) {
                builder.addUriQueryComponent(PushConstants.IMG_QR_CODE_KEY, Base64url.encode(config.imgUrl().getBytes()));
            }

            return builder.build();
        } catch (PushNotificationException e) {
            throw new NodeProcessException("Unable to read service addresses for Push Notification Service.");
        }
    }

    Optional<List<String>> saveDeviceSettings(PushDeviceSettings pushDeviceSettings, JsonValue deviceResponse, AMIdentity username) throws NodeProcessException {

        try {
            pushDeviceSettings.setCommunicationId(deviceResponse.get(COMMUNICATION_ID).asString());
            pushDeviceSettings.setDeviceMechanismUID(deviceResponse.get(MECHANISM_UID).asString());
            pushDeviceSettings.setCommunicationType(deviceResponse.get(COMMUNICATION_TYPE).asString());
            pushDeviceSettings.setDeviceType(deviceResponse.get(DEVICE_TYPE).asString());
            pushDeviceSettings.setDeviceId(deviceResponse.get(DEVICE_ID).asString());
            pushDeviceSettings.setIssuer(config.issuer());


            List<String> recoveryCodes = null;
            if (config.generateRecoveryCodes()) {
                recoveryCodes = recoveryCodeGenerator.generateCodes(NUM_CODES, Alphabet.ALPHANUMERIC, false);
                pushDeviceSettings.setRecoveryCodes(recoveryCodes);

            }
            userPushDeviceProfileManager.saveDeviceProfile(username.getName(), realm.toString(), pushDeviceSettings);

            return Optional.ofNullable(recoveryCodes);
        } catch (NullPointerException e) {
            throw new NodeProcessException("Blank value for necessary data from device response", e);
        } catch (DevicePersistenceException e) {
            throw new NodeProcessException("Unable to store device profile", e);
        } catch (CodeException e) {
            throw new NodeProcessException("Failed to generate recovery codes", e);
        }
    }
}
