package io.hotmoka.network.internal.websocket.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.hotmoka.beans.InternalFailureException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.util.MimeType;

import java.nio.charset.StandardCharsets;

/**
 * A websocket message converter implementation class using the GSON library.
 */
public class GsonMessageConverter extends AbstractMessageConverter {
    private final Gson gson;

    public GsonMessageConverter() {
        super(new MimeType("application", "json"));
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    protected boolean canConvertTo(Object payload, MessageHeaders headers) {
        return true;
    }

    @Override
    protected boolean canConvertFrom(Message<?> message, Class<?> targetClass) {
        return true;
    }

    @Override
    protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {

        try {

            if (new String((byte[]) message.getPayload()).equals("null"))
                return new NullObject();

            return gson.fromJson(new String((byte[]) message.getPayload()), targetClass);
        }
        catch (Exception e) {
            String exceptionMessage = e.getMessage() != null ? ": " + e.getMessage() : "";
            throw new InternalFailureException("Error deserializing json message" + exceptionMessage);
        }
    }

    @Override
    protected Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {

        try {

            if (payload == null)
                return "null".getBytes();

            return gson.toJson(payload).getBytes(StandardCharsets.UTF_8);
        }
        catch (Exception e) {
            String exceptionMessage = e.getMessage() != null ? ": " + e.getMessage() : "";
            throw new InternalFailureException("Error serializing java object" + exceptionMessage);
        }
    }

    /**
     * Special object to wrap a {@code null} reference
     */
    public static class NullObject {}
}
