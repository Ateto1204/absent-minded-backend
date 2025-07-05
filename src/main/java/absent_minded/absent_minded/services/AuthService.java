package absent_minded.absent_minded.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class AuthService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String emailFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header"
            );
        }

        try {
            String token = authHeader.substring(7);
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Missing or invalid Authorization header"
                );
            }

            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(parts[1]),
                    StandardCharsets.UTF_8
            );

            JsonNode payload = MAPPER.readTree(payloadJson);
            if (!payload.has("email")) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Missing or invalid Authorization header"
                );
            }
            return payload.get("email").asText();

        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header"
            );
        }
    }
}