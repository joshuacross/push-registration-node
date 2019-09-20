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

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.services.push.*;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.forgerock.openam.services.push.dispatch.predicates.Predicate;
import org.forgerock.openam.services.push.dispatch.predicates.PushMessageChallengeResponsePredicate;
import org.forgerock.openam.services.push.dispatch.predicates.SignedJwtVerificationPredicate;
import org.forgerock.util.encode.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.forgerock.openam.services.push.PushNotificationConstants.JWT;

public class MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushRegistrationNode.class);

    private final Realm realm;
    private final PushNotificationService pushNotificationService;
    private final MessageIdFactory messageIdFactory;
    private Map<MessageType, ClusterMessageHandler> messageHandlers;

    @Inject
    public MessageService(@Assisted Realm realm,
                          PushNotificationService pushNotificationService,
                          MessageIdFactory messageIdFactory) {
        this.realm = realm;
        this.pushNotificationService = pushNotificationService;
        this.messageIdFactory = messageIdFactory;

        init();
    }

    private void init() {
        try {
            pushNotificationService.init(realm.toString());
            messageHandlers = pushNotificationService.getMessageHandlers(realm.toString());
        } catch (PushNotificationException e) {
            LOGGER.error("Unable initialise push notification service", e);
        }
    }

    MessageState getMessageState(MessageId messageId) throws NodeProcessException, CoreTokenException {
        ClusterMessageHandler messageHandler = messageHandlers.get(messageId.getMessageType());

        if (messageHandler == null) {
            throw new NodeProcessException("The push message corresponds to " + messageId.getMessageType() +
                    " message type which is not registered in the " + realm + " realm");
        }

        return messageHandler.check(messageId);
    }

    MessageId getMessageId(TreeContext context) throws NodeProcessException {
        try {
            String pushMessageId = context.sharedState.get(PushConstants.MESSAGE_ID_KEY).asString();
            return messageIdFactory.create(pushMessageId, realm.toString());
        } catch (PushNotificationException e) {
            throw new NodeProcessException("Could not get messageId", e);
        }
    }

    MessageId getMessageId() {
        return messageIdFactory.create(DefaultMessageTypes.REGISTER);
    }

    JsonValue deleteToken(MessageId messageId) throws CoreTokenException {
        ClusterMessageHandler messageHandler = messageHandlers.get(messageId.getMessageType());
        JsonValue pushContent = messageHandler.getContents(messageId);
        messageHandler.delete(messageId);

        return pushContent;
    }

    void updateMessageDispatcher(PushDeviceSettings pushDeviceSettings, MessageId messageId, String challenge) {
        byte[] secret = Base64.decode(pushDeviceSettings.getSharedSecret());
        Set<Predicate> servicePredicates = new HashSet<>();
        servicePredicates.add(new SignedJwtVerificationPredicate(secret, JWT));
        servicePredicates.add(new PushMessageChallengeResponsePredicate(secret, challenge, JWT));

        try {
            Set<Predicate> predicates = pushNotificationService.getMessagePredicatesFor(realm.toString()).get(DefaultMessageTypes.REGISTER);
            if (predicates != null) {
                servicePredicates.addAll(predicates);
            }

            pushNotificationService.getMessageDispatcher(realm.toString()).expectInCluster(messageId, servicePredicates);
        } catch (NotFoundException | PushNotificationException e) {
            LOGGER.error("Unable to read service addresses for Push Notification Service.");
        } catch (CoreTokenException e) {
            LOGGER.error("Unable to persist token in core token service.", e);
        }
    }

    String getAbsoluteServiceAddressUrl(TreeContext context) throws PushNotificationException {
        return context.request.serverUrl + "/json" + pushNotificationService.getServiceAddressFor(realm.toString(),
                DefaultMessageTypes.AUTHENTICATE);
    }

}
