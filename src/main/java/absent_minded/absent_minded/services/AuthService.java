package absent_minded.absent_minded.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class AuthService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String emailFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return null;

        try {
            String token = authHeader.substring(7);
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(parts[1]),
                    StandardCharsets.UTF_8
            );

            JsonNode payload = MAPPER.readTree(payloadJson);
            return payload.has("email") ? payload.get("email").asText() : null;

        } catch (Exception e) {
            return null;
        }
    }
}