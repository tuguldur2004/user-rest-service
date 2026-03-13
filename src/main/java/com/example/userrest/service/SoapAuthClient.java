package com.example.userrest.service;

import com.example.userrest.dto.TokenValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/**
 * SOAP client that delegates authentication to the user-soap-service.
 *
 * Communication flow:
 * 1. AuthTokenFilter extracts the Bearer token from the HTTP header.
 * 2. The filter calls {@link #validateToken(String)} on this bean.
 * 3. This class constructs a raw SOAP/XML envelope and POSTs it to the
 * SOAP endpoint using Spring's RestTemplate.
 * 4. The XML response is parsed with the JDK's built-in DocumentBuilder
 * (no extra dependencies required).
 * 5. The parsed result is returned to the filter, which either allows or
 * rejects the incoming request.
 *
 * WSDL reference: user-soap-service/src/wsdl/UserAuth.wsdl
 * Operation : ValidateToken
 * Namespace : http://userauth.soap.service/
 */
@Service
public class SoapAuthClient {

    private static final Logger log = LoggerFactory.getLogger(SoapAuthClient.class);
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String AUTH_NS = "http://userauth.soap.service/";

    private final RestTemplate restTemplate;

    /** Configured in application.properties: soap.service.url */
    @Value("${soap.service.url}")
    private String soapUrl;

    public SoapAuthClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Sends a {@code ValidateToken} SOAP request to the Auth Service.
     *
     * @param token JWT token extracted from the Authorization header
     * @return result object with {@code valid}, {@code userId}, {@code username}
     */
    public TokenValidationResult validateToken(String token) {
        TokenValidationResult result = new TokenValidationResult();
        result.setValid(false);

        String envelope = buildValidateTokenEnvelope(token);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_XML);
            // SOAPAction header required by some SOAP servers (empty string = no action)
            headers.set("SOAPAction", "\"\"");

            HttpEntity<String> request = new HttpEntity<>(envelope, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(soapUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                parseValidateTokenResponse(response.getBody(), result);
            } else {
                log.warn("SOAP ValidateToken returned HTTP {}", response.getStatusCode());
            }

        } catch (Exception ex) {
            // If the SOAP service is unreachable, the token is treated as invalid.
            log.error("SOAP ValidateToken call failed: {}", ex.getMessage());
        }

        return result;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Builds the SOAP 1.1 envelope for the ValidateToken operation.
     *
     * Example output:
     * 
     * <pre>
     * &lt;soapenv:Envelope xmlns:soapenv="..." xmlns:tns="..."&gt;
     *   &lt;soapenv:Header/&gt;
     *   &lt;soapenv:Body&gt;
     *     &lt;tns:ValidateTokenRequest&gt;
     *       &lt;tns:token&gt;eyJhbGci...&lt;/tns:token&gt;
     *     &lt;/tns:ValidateTokenRequest&gt;
     *   &lt;/soapenv:Body&gt;
     * &lt;/soapenv:Envelope&gt;
     * </pre>
     */
    private String buildValidateTokenEnvelope(String token) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<soapenv:Envelope" +
                "  xmlns:soapenv=\"" + SOAP_NS + "\"" +
                "  xmlns:tns=\"" + AUTH_NS + "\">" +
                "  <soapenv:Header/>" +
                "  <soapenv:Body>" +
                "    <tns:ValidateTokenRequest>" +
                "      <tns:token>" + escapeXml(token) + "</tns:token>" +
                "    </tns:ValidateTokenRequest>" +
                "  </soapenv:Body>" +
                "</soapenv:Envelope>";
    }

    /**
     * Parses the SOAP response body and populates the result object.
     *
     * Expected response fragment (namespace-aware):
     * 
     * <pre>
     * &lt;ValidateTokenResponse&gt;
     *   &lt;valid&gt;true&lt;/valid&gt;
     *   &lt;userId&gt;42&lt;/userId&gt;
     *   &lt;username&gt;johndoe&lt;/username&gt;
     * &lt;/ValidateTokenResponse&gt;
     * </pre>
     */
    private void parseValidateTokenResponse(String xml, TokenValidationResult result)
            throws Exception {

        Document doc = parseXml(xml);

        String valid = getTagValue(doc, "valid");
        String userId = getTagValue(doc, "userId");
        String username = getTagValue(doc, "username");
        String role = getTagValue(doc, "role");

        result.setValid(Boolean.parseBoolean(valid));
        result.setUserId(userId != null && !userId.isBlank()
                ? Integer.parseInt(userId.trim())
                : 0);
        result.setUsername(username != null ? username : "");
        result.setRole(role != null ? role : "");
    }

    /** Parses an XML string into a DOM Document. */
    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    /**
     * Returns the text content of the first element matching {@code tag},
     * searching across any namespace (wildcard "*").
     */
    private String getTagValue(Document doc, String tag) {
        // Try namespace-aware lookup first
        NodeList nodes = doc.getElementsByTagNameNS("*", tag);
        if (nodes.getLength() == 0) {
            // Fallback: no-namespace lookup
            nodes = doc.getElementsByTagName(tag);
        }
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
    }

    /** Escapes special XML characters to prevent injection in the SOAP envelope. */
    private String escapeXml(String value) {
        if (value == null)
            return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
