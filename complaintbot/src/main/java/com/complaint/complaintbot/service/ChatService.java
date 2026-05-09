package com.complaint.complaintbot.service;

import com.complaint.complaintbot.dto.ChatResponse;
import com.complaint.complaintbot.dto.ChatSummaryDto;
import com.complaint.complaintbot.dto.MessageDto;
import com.complaint.complaintbot.entity.Chat;
import com.complaint.complaintbot.entity.Message;
import com.complaint.complaintbot.repository.ChatRepository;
import com.complaint.complaintbot.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final GeminiChatService geminiChatService;

    /** List all chats, newest first, without message bodies. */
    public List<ChatSummaryDto> listChats() {
        return chatRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(ChatSummaryDto::from)
                .toList();
    }

    /** Get a specific chat with its full message history. */
    public ChatResponse getChat(UUID chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found: " + chatId));
        List<MessageDto> messages = messageRepository.findByChatIdOrderByCreatedAtAsc(chatId)
                .stream().map(MessageDto::from).toList();
        return ChatResponse.from(chat, messages);
    }

    /** Start a brand-new chat with the given first message. */
    @Transactional
    public ChatResponse createChat(String userMessage) {
        // Create and persist the chat
        Chat chat = new Chat();
        chat.setTitle(userMessage.length() > 60 ? userMessage.substring(0, 60) + "…" : userMessage);
        chatRepository.save(chat);

        // Persist user message
        Message userMsg = new Message();
        userMsg.setChatId(chat.getId());
        userMsg.setRole(Message.MessageRole.USER);
        userMsg.setContent(userMessage);
        messageRepository.save(userMsg);

        // Call Gemini (no prior history)
        String reply = geminiChatService.chat(List.of(), userMessage);

        // Persist assistant reply
        Message assistantMsg = new Message();
        assistantMsg.setChatId(chat.getId());
        assistantMsg.setRole(Message.MessageRole.ASSISTANT);
        assistantMsg.setContent(reply);
        messageRepository.save(assistantMsg);

        List<MessageDto> messages = List.of(MessageDto.from(userMsg), MessageDto.from(assistantMsg));
        return ChatResponse.from(chat, messages);
    }

    /** Continue an existing chat — loads full history and sends it to Gemini for context. */
    @Transactional
    public ChatResponse continueChat(UUID chatId, String userMessage) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found: " + chatId));

        // Load prior history for AI context
        List<Message> history = messageRepository.findByChatIdOrderByCreatedAtAsc(chatId);

        // Persist new user message
        Message userMsg = new Message();
        userMsg.setChatId(chatId);
        userMsg.setRole(Message.MessageRole.USER);
        userMsg.setContent(userMessage);
        messageRepository.save(userMsg);

        // Call Gemini with full conversation history
        String reply = geminiChatService.chat(history, userMessage);

        // Persist assistant reply
        Message assistantMsg = new Message();
        assistantMsg.setChatId(chatId);
        assistantMsg.setRole(Message.MessageRole.ASSISTANT);
        assistantMsg.setContent(reply);
        messageRepository.save(assistantMsg);

        // Update chat's updatedAt
        chatRepository.save(chat);

        List<MessageDto> allMessages = messageRepository.findByChatIdOrderByCreatedAtAsc(chatId)
                .stream().map(MessageDto::from).toList();
        return ChatResponse.from(chat, allMessages);
    }
}
