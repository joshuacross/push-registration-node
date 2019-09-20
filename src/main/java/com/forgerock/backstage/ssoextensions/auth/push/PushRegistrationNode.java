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

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.push.UserPushDeviceProfileManager;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.MessageState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.TextOutputCallback;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.forgerock.backstage.ssoextensions.auth.push.PushConstants.*;
import static org.forgerock.openam.auth.node.api.Action.send;

@Node.Metadata(outcomeProvider = PushRegistrationNodeOutcomeProvider.class,
        configClass = PushRegistrationNode.Config.class)
public class PushRegistrationNode implements Node {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushRegistrationNode.class);

    private final Config config;
    private final UserPushDeviceProfileManager userPushDeviceProfileManager;
    private final PushHelper helper;
    private final MessageService messageService;

    public interface Config extends PushRegistrationNodeConfig {
    }

    @Inject
    public PushRegistrationNode(@Assisted Config config,
                                UserPushDeviceProfileManager userPushDeviceProfileManager,
                                PushHelper helper,
                                MessageService messageService) {
        this.config = config;
        this.userPushDeviceProfileManager = userPushDeviceProfileManager;
        this.helper = helper;
        this.messageService = messageService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        Optional<PollingWaitCallback> pollingWaitCallback = context.getCallback(PollingWaitCallback.class);

        if (pollingWaitCallback.isPresent()) {

            if (!context.sharedState.isDefined(MESSAGE_ID_KEY)) {
                throw new NodeProcessException("Unable to find push message ID");
            }

            return getActionFromState(context);
        }

        return createDeviceSettingsAndFinishWithCallbacks(context);
    }

    private Action getActionFromState(TreeContext context) throws NodeProcessException {
        try {
            MessageId messageId = messageService.getMessageId(context);
            PushDeviceSettings pushDeviceSettings = helper.getDeviceProfileFromSharedState(context, PUSH_DEVICE_PROFILE_KEY)
                    .orElseThrow(() -> new NodeProcessException("No device profile found on shared state"));
            AMIdentity userIdentity = helper.getIdentity(context);
            MessageState state = messageService.getMessageState(messageId);

            if (state == null) {
                LOGGER.error("The push message with ID {} has timed out", messageId);
                return buildAction(PushRegistrationNodeOutcomeProvider.PushRegOutcome.EXPIRED, context);
            }

            switch (state) {
                case SUCCESS:
                    JsonValue pushContent = messageService.deleteToken(messageId);
                    Optional<List<String>> recoveryCodes = helper.saveDeviceSettings(pushDeviceSettings, pushContent, userIdentity);

                    return recoveryCodes.map(codes -> buildActionWithRecoveryCodes(context, pushDeviceSettings, codes))
                            .orElseGet(() -> buildAction(PushRegistrationNodeOutcomeProvider.PushRegOutcome.SUCCESS, context));
                case DENIED:
                    messageService.deleteToken(messageId);

                    return buildAction(PushRegistrationNodeOutcomeProvider.PushRegOutcome.FAILURE, context);
                case UNKNOWN:
                    String challenge = context.sharedState.get(CHALLENGE_KEY).asString();
                    Callback qrCallback = helper.createQRCodeCallback(pushDeviceSettings, userIdentity, messageId.toString(), challenge, context);

                    return send(
                            new TextOutputCallback(TextOutputCallback.INFORMATION, REGISTER_DEVICE_HELP_TEXT),
                            qrCallback,
                            getPollingWaitCallback()
                    ).build();
                default:
                    throw new NodeProcessException("Unrecognized push message status: " + state);
            }
        } catch (CoreTokenException e) {
            throw new NodeProcessException("An unexpected error occurred while verifying the push result", e);
        }
    }

    private PollingWaitCallback getPollingWaitCallback() {
        return PollingWaitCallback.makeCallback()
                .withWaitTime(String.valueOf(config.timeout() * 1000))
                .build();
    }

    private Action buildActionWithRecoveryCodes(TreeContext context, PushDeviceSettings pushDeviceSettings, List<String> recoveryCodes) {
        Action.ActionBuilder builder = Action.goTo(PushRegistrationNodeOutcomeProvider.PushRegOutcome.SUCCESS.name());
        JsonValue transientState = context.transientState.copy();
        transientState
                .put(RECOVERY_CODE_KEY, recoveryCodes)
                .put(RECOVERY_CODE_DEVICE_NAME, pushDeviceSettings.getDeviceName());

        return cleanupSharedState(context, builder).replaceTransientState(transientState).build();
    }

    private Action buildAction(PushRegistrationNodeOutcomeProvider.PushRegOutcome outcome, TreeContext context) {
        Action.ActionBuilder builder = Action.goTo(outcome.name());
        return cleanupSharedState(context, builder).build();
    }

    private Action.ActionBuilder cleanupSharedState(TreeContext context, Action.ActionBuilder builder) {
        JsonValue sharedState = context.sharedState.copy();
        sharedState.remove(MESSAGE_ID_KEY);
        sharedState.remove(PUSH_DEVICE_PROFILE_KEY);
        sharedState.remove(CHALLENGE_KEY);

        return builder.replaceSharedState(sharedState);
    }

    private Action createDeviceSettingsAndFinishWithCallbacks(TreeContext context) throws NodeProcessException {
        PushDeviceSettings pushDeviceSettings = userPushDeviceProfileManager.createDeviceProfile();
        try {
            MessageId messageId = messageService.getMessageId();
            String challenge = userPushDeviceProfileManager.createRandomBytes();
            AMIdentity userIdentity = helper.getIdentity(context);

            Callback qrCodeCallback = helper.createQRCodeCallback(pushDeviceSettings, userIdentity, messageId.toString(), challenge, context);
            List<Callback> callbacks = ImmutableList.of(
                    new TextOutputCallback(TextOutputCallback.INFORMATION, REGISTER_DEVICE_HELP_TEXT),
                    qrCodeCallback,
                    getPollingWaitCallback()
            );
            messageService.updateMessageDispatcher(pushDeviceSettings, messageId, challenge);

            JsonValue sharedState = context.sharedState.copy()
                    .put(PUSH_DEVICE_PROFILE_KEY, helper.encryptDeviceSettings(pushDeviceSettings))
                    .put(CHALLENGE_KEY, challenge)
                    .put(MESSAGE_ID_KEY, messageId.toString());

            return send(callbacks)
                    .replaceSharedState(sharedState)
                    .build();
        } catch (IOException e) {
            throw new NodeProcessException(e);
        }
    }
}
