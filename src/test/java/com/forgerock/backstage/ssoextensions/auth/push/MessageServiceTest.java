package com.forgerock.backstage.ssoextensions.auth.push;

import com.google.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.services.push.*;
import org.forgerock.openam.services.push.dispatch.handlers.ClusterMessageHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static com.forgerock.backstage.ssoextensions.auth.push.PushConstants.MESSAGE_ID_KEY;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MessageServiceTest {

    @Mock
    private Realm realm;
    @Mock
    private PushNotificationService pushNotificationService;
    @Mock
    private MessageIdFactory messageIdFactory;
    @Mock
    private ClusterMessageHandler messageHandler;
    @Mock
    private MessageId messageId;

    private MessageService messageService;

    @Before
    public void setup() throws PushNotificationException {
        Map<MessageType, ClusterMessageHandler> messageHandlers = new HashMap<>();
        messageHandlers.put(DefaultMessageTypes.REGISTER, messageHandler);
        when(pushNotificationService.getMessageHandlers(any())).thenReturn(messageHandlers);
        messageService = new MessageService(realm, pushNotificationService, messageIdFactory);
    }

    @Test
    public void getMessageState_whenMessageIdSuccess_thenReturnSuccess() throws CoreTokenException, NodeProcessException {
        when(messageId.getMessageType()).thenReturn(DefaultMessageTypes.REGISTER);
        when(messageHandler.check(messageId)).thenReturn(MessageState.SUCCESS);

        MessageState messageState = messageService.getMessageState(messageId);

        assertThat(messageState).isEqualTo(MessageState.SUCCESS);
    }

    @Test
    public void getMessageId_whenRegisterKey_thenReturnRegisterMessageId() throws PushNotificationException, NodeProcessException {
        TreeContext context = getContextWithMessageId();
        when(messageIdFactory.create(any(), any())).thenReturn(messageId);

        MessageId messageId = messageService.getMessageId(context);

        assertThat(messageId).isEqualToComparingFieldByField(messageId);
    }

    private TreeContext getContextWithMessageId() {
        Map<String, String> map = new HashMap<>();
        map.put(MESSAGE_ID_KEY, "REGISTER:6ed29738-20ef-4bc3-bece-48c238660f341558691882220");
        JsonValue sharedState = new JsonValue(map);
        ExternalRequestContext request = new ExternalRequestContext.Builder().parameters(emptyMap()).build();

        return new TreeContext(sharedState, request, ImmutableList.of());
    }

    @Test
    public void deleteToken() throws CoreTokenException {
        when(messageId.getMessageType()).thenReturn(DefaultMessageTypes.REGISTER);
        when(messageHandler.getContents(messageId)).thenReturn(new JsonValue("contents"));

        JsonValue pushContent = messageService.deleteToken(messageId);

        verify(messageHandler).delete(messageId);
        assertThat(pushContent.asString()).isEqualTo("contents");
    }
}