package com.criptext.monkeychatandroid.models.conversation;

import android.text.TextUtils;

import com.criptext.comunication.MOKConversation;
import com.google.gson.JsonObject;
import com.criptext.monkeychatandroid.models.DatabaseHandler;
import com.criptext.monkeychatandroid.models.message.MessageItem;
import com.criptext.monkeykitui.conversation.MonkeyConversation;
import com.criptext.monkeykitui.conversation.holder.ConversationTransaction;

import org.jetbrains.annotations.NotNull;

public class TransactionCreator {

    public static ConversationTransaction fromSentMessage(@NotNull final MessageItem sentMessage, final boolean deliveredAndRead) {
        return new ConversationTransaction() {
            @Override
            public void updateConversation(MonkeyConversation monkeyConversation) {
                long dateTime = sentMessage.getMessageTimestampOrder();
                ConversationItem conversation = (ConversationItem) monkeyConversation;
                conversation.setDatetime(dateTime > -1 ? dateTime : conversation.getDatetime());
                if (deliveredAndRead) {
                    conversation.lastRead = sentMessage.getMessageTimestampOrder();
                    conversation.setTotalNewMessage(0);
                } else
                    conversation.setTotalNewMessage(conversation.getTotalNewMessages() + 1);
                String secondaryText = DatabaseHandler.getSecondaryTextByMessageType(sentMessage, monkeyConversation.isGroup());
                conversation.setSecondaryText(secondaryText);

                int newStatus;
                if (!sentMessage.isIncomingMessage()) {
                    switch (sentMessage.getDeliveryStatus()) {
                        case sending:
                            newStatus = MonkeyConversation.ConversationStatus.sendingMessage.ordinal();
                            break;
                        case delivered:
                            newStatus = deliveredAndRead
                                    ? MonkeyConversation.ConversationStatus.sentMessageRead.ordinal()
                                    : MonkeyConversation.ConversationStatus.deliveredMessage.ordinal();
                            break;
                        default:
                            throw new UnsupportedOperationException("tried to conversation with outgoing sentMessage with type error");
                    }
                } else {
                    newStatus = MonkeyConversation.ConversationStatus.receivedMessage.ordinal();
                }
                conversation.setStatus(newStatus);
            }
        };
    }

    public static ConversationTransaction fromContactOpenedConversation(final long newLastReadValue) {
        return new ConversationTransaction() {
            @Override
            public void updateConversation(@NotNull MonkeyConversation conversation) {
                ConversationItem conversationItem = (ConversationItem) conversation;
                conversationItem.lastRead = newLastReadValue;
                final MonkeyConversation.ConversationStatus status =
                        MonkeyConversation.ConversationStatus.values()[conversation.getStatus()];
                if (status == MonkeyConversation.ConversationStatus.deliveredMessage) {
                    int newStatus = MonkeyConversation.ConversationStatus.sentMessageRead.ordinal();
                    conversationItem.setStatus(newStatus);
                }
            }
        };
    }

    public static ConversationTransaction fromGroupInfo(final MOKConversation mokConversation) {
        final JsonObject props = mokConversation.getInfo();
        final String avatar = props.get("avatar").getAsString();
        final String name = props.get("name").getAsString();
        final String admin = props.get("admin").getAsString();
        return new ConversationTransaction() {
            @Override
            public void updateConversation(MonkeyConversation conversation) {
                ConversationItem conversationItem = (ConversationItem) conversation;
                conversationItem.setAvatarFilePath(avatar);
                conversationItem.setName(name);
                conversationItem.setAdmins(admin);
                conversationItem.setGroupMembers(TextUtils.join(",", mokConversation.getMembers()));
                conversationItem.setGroup(true);
            }
        };
    }
}

