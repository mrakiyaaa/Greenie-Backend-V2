package com.example.projectgreenie.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class OpenRouterService {

    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String API_KEY = "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"; // Replace with your key

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenRouterService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public String checkImage(String imageUrl, String description) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + API_KEY);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "openai/gpt-4o");

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", """
            You are an advanced AI verifier for an environmental cleanup reward system. Your decision determines whether a user receives payment.
        
            🔍 Your task:
            Carefully analyze the **uploaded image** and the **description provided**.
        
            You must:
            1. **Determine if the image is 100%% real** (natural, unedited, human-taken).
            2. **Verify the description perfectly matches the image content** (no vague or unrelated text).
            3. **Reject anything AI-generated**, cartoon-like, digitally created, or suspicious.
        
            🛑 Reject and return `Issue` if:
            - The image is AI-generated, cartoon, synthetic, or digitally enhanced in any way.
            - There are signs of abnormal lighting, textures, hands, faces, artifacts, or surreal elements.
            - The image and description do not match precisely in activity, setting, or context.
            - You are uncertain about the authenticity of the image.
        
            ✅ Only approve with `Verified` if:
            - The photo is clearly real and natural.
            - It shows actual cleanup or environmental work by real people or equipment.
            - The description aligns fully with what is shown in the image.
        
            Always err on the side of caution. Users are paid based on your result.
        
            Return your decision in this format:
        
            Status: `Verified` or `Issue`  
            Reason: A very short, strict reason (max 20 words)
        
            Description: %s
            """.formatted(description));


            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("type", "image_url");
            Map<String, Object> imageUrlMap = new HashMap<>();
            imageUrlMap.put("url", imageUrl);
            imagePart.put("image_url", imageUrlMap);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", new Object[]{textPart, imagePart});

            requestBody.put("messages", Collections.singletonList(message));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(API_URL, HttpMethod.POST, entity, String.class);

            System.out.println("📡 OpenRouter API Response: " + response.getBody());

            return extractStatusAndReason(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            return "Issue | AI validation failed due to server error.";
        }
    }

    private String extractStatusAndReason(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            System.out.println("🔍 FULL AI RESPONSE: " + content);

            if (content.toLowerCase().contains("status:") && content.toLowerCase().contains("reason:")) {
                String status = content.split("(?i)status:")[1].split("(?i)reason:")[0].trim();
                String reason = content.split("(?i)reason:")[1].trim();
                return status + " | " + reason;
            }

            return "Issue | AI response format invalid.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Issue | Error parsing AI response.";
        }
    }
}
