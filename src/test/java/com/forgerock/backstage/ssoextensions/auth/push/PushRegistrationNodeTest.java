package com.forgerock.backstage.ssoextensions.auth.push;

import com.google.common.collect.ImmutableList;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.idm.AMIdentity;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.push.UserPushDeviceProfileManager;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.MessageState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.security.auth.callback.TextOutputCallback;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.forgerock.backstage.ssoextensions.auth.push.PushConstants.*;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PushRegistrationNodeTest {

    @Mock
    private PushRegistrationNode.Config config;
    @Mock
    private UserPushDeviceProfileManager userPushDeviceProfileManager;
    @Mock
    private PushHelper pushHelper;
    @Mock
    private MessageService messageService;
    @InjectMocks
    private PushRegistrationNode pushRegistrationNode;

    private final JsonValue emptySharedState = new JsonValue(new HashMap<>());
    private final ExternalRequestContext request = new ExternalRequestContext.Builder().parameters(emptyMap()).build();
    private final PushDeviceSettings settings = new PushDeviceSettings();

    private JsonValue sharedState;

    @Before
    public void setup() {
        settings.setSharedSecret("olVsCC00XtifveplR0fI7ZeE3r0i3ei+lERaPESSoPg=");
        settings.setDeviceName("Push Device");
        Map<String, String> map = new HashMap<>();
        map.put(MESSAGE_ID_KEY, "REGISTER:6ed29738-20ef-4bc3-bece-48c238660f341558691882220");
        map.put(CHALLENGE_KEY, "6Cr+h+kd1j5Rr2A8DDeg3ekwZVD06bVJCpAHoR9aAJI=");
        sharedState = new JsonValue(map);

        when(config.timeout()).thenReturn(8);
    }

    @Test
    public void process_whenFirstVisit_thenReturnCallbacks() throws NodeProcessException, IOException {
        TreeContext context = new TreeContext(emptySharedState, request, ImmutableList.of());
        MessageId messageId = mock(MessageId.class);

        when(messageService.getMessageId()).thenReturn(messageId);
        when(messageId.toString()).thenReturn("REGISTER:6ed29738-20ef-4bc3-bece-48c238660f341558691882220");
        when(pushHelper.encryptDeviceSettings(any())).thenReturn("eyJ0eXAiOiJKV1QiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiY...");
        when(userPushDeviceProfileManager.createRandomBytes()).thenReturn("6Cr+h+kd1j5Rr2A8DDeg3ekwZVD06bVJCpAHoR9aAJI=");
        when(pushHelper.createQRCodeCallback(any(), any(), any(), any(), any())).thenReturn(mock(ScriptTextOutputCallback.class));

        Action action = pushRegistrationNode.process(context);

        verify(messageService).updateMessageDispatcher(any(), any(), any());
        assertThat(action.outcome).isNull();
        assertThat(action.callbacks.get(0)).isInstanceOf(TextOutputCallback.class);
        assertThat(action.callbacks.get(1)).isInstanceOf(ScriptTextOutputCallback.class);
        assertThat(action.callbacks.get(2)).isInstanceOf(PollingWaitCallback.class);
        assertThat(action.sharedState.get(PUSH_DEVICE_PROFILE_KEY).asString()).isEqualTo("eyJ0eXAiOiJKV1QiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiY...");
        assertThat(action.sharedState.get(MESSAGE_ID_KEY).asString()).isEqualTo("REGISTER:6ed29738-20ef-4bc3-bece-48c238660f341558691882220");
        assertThat(action.sharedState.get(CHALLENGE_KEY).asString()).isEqualTo("6Cr+h+kd1j5Rr2A8DDeg3ekwZVD06bVJCpAHoR9aAJI=");
    }

    @Test
    public void process_whenStateIsNull_thenReturnExpiredOutcome() throws CoreTokenException, NodeProcessException {
        TreeContext context = new TreeContext(sharedState, request, ImmutableList.of(mock(PollingWaitCallback.class)));

        when(pushHelper.getDeviceProfileFromSharedState(any(), any())).thenReturn(Optional.of(settings));
        when(messageService.getMessageState(any())).thenReturn(null);

        Action action = pushRegistrationNode.process(context);

        assertThat(action.outcome).isEqualTo("EXPIRED");
        assertThat(action.sharedState.isDefined(MESSAGE_ID_KEY)).isFalse();
        assertThat(action.sharedState.isDefined(CHALLENGE_KEY)).isFalse();
        assertThat(action.sharedState.isDefined(PUSH_DEVICE_PROFILE_KEY)).isFalse();
        assertThat(action.transientState).isNull();
    }

    @Test
    public void process_whenStateSuccess_thenReturnSuccessOutcomeAndRecoveryCodes() throws CoreTokenException, NodeProcessException {
        TreeContext context = new TreeContext(sharedState, request, ImmutableList.of(mock(PollingWaitCallback.class)));

        when(pushHelper.getDeviceProfileFromSharedState(any(), any())).thenReturn(Optional.of(settings));
        when(pushHelper.getIdentity(any())).thenReturn(mock(AMIdentity.class));
        when(messageService.getMessageState(any())).thenReturn(MessageState.SUCCESS);
        when(pushHelper.saveDeviceSettings(any(), any(), any())).thenReturn(Optional.of(Arrays.asList("z0WKEw0Wc8", "Ios4LnA2Qn")));

        Action action = pushRegistrationNode.process(context);

        assertThat(action.outcome).isEqualTo("SUCCESS");
        assertThat(action.sharedState.isDefined(MESSAGE_ID_KEY)).isFalse();
        assertThat(action.sharedState.isDefined(CHALLENGE_KEY)).isFalse();
        assertThat(action.sharedState.isDefined(PUSH_DEVICE_PROFILE_KEY)).isFalse();
        assertThat(action.transientState.get(RECOVERY_CODE_KEY).asList()).containsExactlyInAnyOrder("z0WKEw0Wc8", "Ios4LnA2Qn");
        assertThat(action.transientState.get(RECOVERY_CODE_DEVICE_NAME).asString()).isEqualTo("Push Device");
    }

    @Test
    public void process_whenStateSuccessAndRecoveryCodesDisabled_thenReturnSuccessOutcome() throws CoreTokenException, NodeProcessException {
        TreeContext context = new TreeContext(sharedState, request, ImmutableList.of(mock(PollingWaitCallback.class)));

        when(pushHelper.getDeviceProfileFromSharedState(any(), any())).thenReturn(Optional.of(settings));
        when(pushHelper.getIdentity(any())).thenReturn(mock(AMIdentity.class));
        when(messageService.getMessageState(any())).thenReturn(MessageState.SUCCESS);
        when(pushHelper.saveDeviceSettings(any(), any(), any())).thenReturn(Optional.empty());

        Action action = pushRegistrationNode.process(context);

        assertThat(action.outcome).isEqualTo("SUCCESS");
        assertThat(action.sharedState.isDefined(MESSAGE_ID_KEY)).isFalse();
        assertThat(action.sharedState.isDefined(CHALLENGE_KEY)).isFalse();
        assertThat(action.sharedState.isDefined(PUSH_DEVICE_PROFILE_KEY)).isFalse();
        assertThat(action.transientState).isNull();
    }

    @Test
    public void process_whenStateDenied_thenReturnFailureOutcome() throws CoreTokenException, NodeProcessException {
        TreeContext context = new TreeContext(sharedState, request, ImmutableList.of(mock(PollingWaitCallback.class)));

        when(pushHelper.getDeviceProfileFromSharedState(any(), any())).thenReturn(Optional.of(settings));
        when(pushHelper.getIdentity(any())).thenReturn(mock(AMIdentity.class));
        when(messageService.getMessageState(any())).thenReturn(MessageState.DENIED);

        Action action = pushRegistrationNode.process(context);

        assertThat(action.outcome).isEqualTo("FAILURE");
        assertThat(action.sharedState.isDefined(MESSAGE_ID_KEY)).isFalse();
        assertThat(action.sharedState.isDefined(CHALLENGE_KEY)).isFalse();
        assertThat(action.sharedState.isDefined(PUSH_DEVICE_PROFILE_KEY)).isFalse();
        assertThat(action.transientState).isNull();
    }

    @Test
    public void process_whenStateUnknown_thenReturnCallbacks() throws CoreTokenException, NodeProcessException {
        Map<String, String> map = new HashMap<>();
        map.put(MESSAGE_ID_KEY, "REGISTER:6ed29738-20ef-4bc3-bece-48c238660f341558691882220");
        map.put(CHALLENGE_KEY, "6Cr+h+kd1j5Rr2A8DDeg3ekwZVD06bVJCpAHoR9aAJI=");
        map.put(PUSH_DEVICE_PROFILE_KEY, "eyJ0eXAiOiJKV1QiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiY...");
        JsonValue sharedState = new JsonValue(map);
        TreeContext context = new TreeContext(sharedState, request, ImmutableList.of(mock(PollingWaitCallback.class)));

        when(messageService.getMessageId(any())).thenReturn(mock(MessageId.class));
        when(pushHelper.getDeviceProfileFromSharedState(any(), any())).thenReturn(Optional.of(settings));
        when(pushHelper.getIdentity(any())).thenReturn(mock(AMIdentity.class));
        when(messageService.getMessageState(any())).thenReturn(MessageState.UNKNOWN);
        when(pushHelper.createQRCodeCallback(any(), any(), any(), any(), any())).thenReturn(mock(ScriptTextOutputCallback.class));

        Action action = pushRegistrationNode.process(context);

        assertThat(action.outcome).isNull();
        assertThat(action.callbacks.get(0)).isInstanceOf(TextOutputCallback.class);
        assertThat(action.callbacks.get(1)).isInstanceOf(ScriptTextOutputCallback.class);
        assertThat(action.callbacks.get(2)).isInstanceOf(PollingWaitCallback.class);
    }
}