package com.complaint.complaintbot.service;

import com.complaint.complaintbot.entity.Message;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GeminiChatService {

    private final ChatLanguageModel chatLanguageModel;

    /**
     * Send a new message to Gemini, providing the full prior conversation as context.
     *
     * @param history    Previous messages in the chat (may be empty for a new chat)
     * @param userMessage The latest user message
     * @return The AI assistant's reply text
     */
    public String chat(List<Message> history, String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();

        for (Message msg : history) {
            if (msg.getRole() == Message.MessageRole.USER) {
                messages.add(UserMessage.from(msg.getContent()));
            } else {
                messages.add(AiMessage.from(msg.getContent()));
            }
        }

        messages.add(UserMessage.from(userMessage));

        return chatLanguageModel.generate(messages).content().text();
    }
}
