package com.complaint.complaintbot.service;

import com.complaint.complaintbot.dto.ChatResponse;
import com.complaint.complaintbot.dto.ChatSummaryDto;
import com.complaint.complaintbot.dto.ComplaintFilingResult;
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
    private final ComplaintFilingService complaintFilingService;

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
        Chat chat = new Chat();
        chat.setTitle(userMessage.length() > 60 ? userMessage.substring(0, 60) + "…" : userMessage);
        chatRepository.save(chat);

        persistMessage(chat.getId(), Message.MessageRole.USER, userMessage);

        // Classify first: only navigate to the portal if it is a complaint
        String reply;
        ComplaintFilingResult filingResult = complaintFilingService.fileComplaint(chat.getId(), userMessage);
        if (filingResult.isNotComplaint()) {
            // Not a complaint — plain conversational reply, no browser navigation
            reply = geminiChatService.chat(List.of(), userMessage);
        } else {
            // Is a complaint — reply comes from the filing / navigation result
            reply = buildReplyFromFilingResult(filingResult);
        }

        persistMessage(chat.getId(), Message.MessageRole.ASSISTANT, reply);

        List<MessageDto> allMessages = messageRepository.findByChatIdOrderByCreatedAtAsc(chat.getId())
                .stream().map(MessageDto::from).toList();
        return ChatResponse.from(chat, allMessages);
    }

    /**
     * Continue an existing chat.
     * If the chat session is AWAITING_OTP, the user's message is treated as the OTP
     * and forwarded to ComplaintFilingService to continue the browser flow.
     */
    @Transactional
    public ChatResponse continueChat(UUID chatId, String userMessage) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found: " + chatId));

        persistMessage(chatId, Message.MessageRole.USER, userMessage);

        String reply;
        if (complaintFilingService.isAwaitingOtp(chatId)) {
            // User is providing the OTP — route it to filing service
            ComplaintFilingResult result = complaintFilingService.submitOtp(chatId, userMessage.trim());
            reply = buildReplyFromFilingResult(result);
        } else {
            // Classify the new message: only navigate to the portal if it is a complaint
            ComplaintFilingResult filingResult = complaintFilingService.fileComplaint(chatId, userMessage);
            if (filingResult.isNotComplaint()) {
                // Not a complaint — plain conversational reply, no browser navigation
                List<Message> history = messageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
                reply = geminiChatService.chat(history, userMessage);
            } else {
                reply = buildReplyFromFilingResult(filingResult);
            }
        }

        persistMessage(chatId, Message.MessageRole.ASSISTANT, reply);
        chatRepository.save(chat); // bumps updatedAt

        List<MessageDto> allMessages = messageRepository.findByChatIdOrderByCreatedAtAsc(chatId)
                .stream().map(MessageDto::from).toList();
        return ChatResponse.from(chat, allMessages);
    }

    /**
     * Explicitly trigger the autonomous complaint filing flow for an existing chat.
     * This is kept for backward compatibility with the explicit /file endpoint.
     */
    @Transactional
    public ComplaintFilingResult fileComplaint(UUID chatId) {
        // Grab the last user message as the complaint text
        List<Message> history = messageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
        String lastUserMessage = history.stream()
                .filter(m -> m.getRole() == Message.MessageRole.USER)
                .map(Message::getContent)
                .reduce((first, second) -> second) // last element
                .orElseThrow(() -> new IllegalArgumentException("No user message in chat: " + chatId));

        ComplaintFilingResult result = complaintFilingService.fileComplaint(chatId, lastUserMessage);

        // Persist result as an assistant message
        persistMessage(chatId, Message.MessageRole.ASSISTANT, buildReplyFromFilingResult(result));

        return result;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Message persistMessage(UUID chatId, Message.MessageRole role, String content) {
        Message msg = new Message();
        msg.setChatId(chatId);
        msg.setRole(role);
        msg.setContent(content);
        return messageRepository.save(msg);
    }

    private String buildReplyFromFilingResult(ComplaintFilingResult result) {
        if (result.awaitingOtp()) return result.message();
        if (result.success()) return result.message() + "\n\nSteps:\n" + String.join("\n", result.stepSummaries());
        return "Filing status: " + result.message();
    }
}
