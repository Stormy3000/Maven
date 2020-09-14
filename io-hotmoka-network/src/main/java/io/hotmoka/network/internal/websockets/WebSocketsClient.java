package io.hotmoka.network.internal.websockets;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSession.Subscription;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.network.internal.services.NetworkExceptionResponse;
import io.hotmoka.network.models.errors.ErrorModel;

/**
 * A websockets client class to subscribe, send and receive messages from a websockets end-point.
 * This is meant to be used in a thread-local way.
 */
class WebSocketsClient implements AutoCloseable {

	/**
	 * The supporting STOMP client.
	 */
	private final WebSocketStompClient stompClient;

    /**
     * The unique identifier of this client.
     */
    private final String clientKey;

    /**
     * The websockets end-point.
     */
    private final String url;

    /**
     * The current session.
     */
    private volatile StompSession stompSession;

    /**
     * The websockets subscriptions open so far with this client.
     */
    private final Map<String, Subscription> subscriptions = new HashMap<>();

    /**
     * The last send request with this client. Since this is used in a thread-local way,
     * only one send can exist at most.
     */
    private volatile Send<?> lastSend;

    private final static Logger LOGGER = LoggerFactory.getLogger(WebSocketsClient.class);

    /**
     * Creates an instance of a websockets client to subscribe, send and receive messages from a websockets end-point.
     * 
     * @param url the websockets end-point
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted
     */
    public WebSocketsClient(String url) throws ExecutionException, InterruptedException {
        this.url = url;
        this.clientKey = generateClientKey();

        // container configuration with the message size limit
        WsWebSocketContainer wsWebSocketContainer = new WsWebSocketContainer();
        wsWebSocketContainer.setDefaultMaxTextMessageBufferSize(WebSocketsConfig.MESSAGE_SIZE_LIMIT); // default 8192
        wsWebSocketContainer.setDefaultMaxBinaryMessageBufferSize(WebSocketsConfig.MESSAGE_SIZE_LIMIT); // default 8192

        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient(wsWebSocketContainer));
        this.stompClient.setInboundMessageSizeLimit(WebSocketsConfig.MESSAGE_SIZE_LIMIT); // default 64 * 1024
        this.stompClient.setMessageConverter(new GsonMessageConverter());
        connect();
    }


    /**
     * Sends a request for the given topic, expecting a result of the givem type and
     * bearing the given payload.
     *
     * @param topic the topic
     * @param resultTypeClass the result class type
     * @param payload, the payload, if any
     * @return the result of the request
     */
    public <T> T send(String topic, Class<T> resultTypeClass, Optional<Object> payload) throws ExecutionException, InterruptedException {
    	return new Send<>(topic, resultTypeClass, payload).getResult();
    }

    /**
     * Connects to the websockets end-point and creates the current session.
     * 
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted
     */
    private void connect() throws ExecutionException, InterruptedException {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("uuid", clientKey);
        stompSession = stompClient.connect(url, headers, new StompClientSessionHandler()).get();
    }

    @Override
    public void close() {
    	subscriptions.values().forEach(Subscription::unsubscribe);

    	if (stompSession != null)
    		stompSession.disconnect();

    	stompClient.stop();
    }

    /**
     * Generates a unique key for this websockets client.
     * 
     * @return the unique key
     */
    private static String generateClientKey() {
        try {
            MessageDigest salt = MessageDigest.getInstance("SHA-256");
            salt.update(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            return bytesToHex(salt.digest());
        }
        catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    private static String bytesToHex(byte[] bytes) {
        byte [] HEX_ARRAY = "0123456789abcdef".getBytes();
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return new String(hexChars, StandardCharsets.UTF_8);
    }

    /**
     * Code that sends a request to a websockets topic, creating the subscriptions, if needed.
     */
    private class Send<T> {

    	/**
    	 * The class of the result of the request.
    	 */
    	private final Class<T> resultTypeClass;

    	/**
    	 * The result of the request, if it did not fail.
    	 */
    	private final CompletableFuture<T> result = new CompletableFuture<>();

    	/**
    	 * The failed result of the request, if it failed.
    	 */
    	private final CompletableFuture<ErrorModel> error = new CompletableFuture<>();

    	/**
    	 * Sends the given payload to the given websockets end-point, expecting a result
    	 * of the given type.
    	 * 
    	 * @param topic the topic
    	 * @param resultTypeClass the class of the type of the result
    	 * @param payload the payload, if any
    	 */
        private Send(String topic, Class<T> resultTypeClass, Optional<Object> payload) {
            this.resultTypeClass = resultTypeClass;
            lastSend = this;
            String fullTopic = "/user/" + clientKey + topic;
            subscribeForSuccess(fullTopic);
            subscribeForError(fullTopic);
            stompSession.send(topic, payload.orElse(null));
        }

        /**
		 * Uses a cache to avoid recreating a subscription for the same topic.
		 * 
		 * @param topic the topic
		 * @return the subscription, new or recycled
		 */
		private Subscription subscribeForSuccess(String topic) {
			return subscriptions.computeIfAbsent(topic, _topic -> {
				StompFrameHandler stompHandler = new StompFrameHandler() {
		
					@Override
					public Type getPayloadType(StompHeaders headers) {
						return resultTypeClass; // the type is implied by the topic
					}
		
					@Override
					public void handleFrame(StompHeaders headers, Object payload) {
						lastSend.handleFrameForSuccess(headers, payload);
					}
				};
		
				Subscription stompSubscription = stompSession.subscribe(_topic, stompHandler);
				LOGGER.info("Subscribed to " + _topic);
				return stompSubscription;
			});
		}

		private Subscription subscribeForError(String topic) {
			topic = topic + "/error";
		
			return subscriptions.computeIfAbsent(topic, _topic -> {
				StompFrameHandler stompHandler = new StompFrameHandler() {
		
					@Override
					public Type getPayloadType(StompHeaders headers) {
						return ErrorModel.class;
					}
		
					@Override
					public void handleFrame(StompHeaders headers, Object payload) {
						lastSend.handleFrameForError(headers, payload);
					}
				};
		
				Subscription stompSubscription = stompSession.subscribe(_topic, stompHandler);
				LOGGER.info("Subscribed to " + _topic);
				return stompSubscription;
			});
		}

		@SuppressWarnings("unchecked")
		private void handleFrameForSuccess(StompHeaders headers, Object payload) {
			if (payload == null)
            	error.complete(new ErrorModel(new InternalFailureException("Received a null payload")));
            else if (payload.getClass() == GsonMessageConverter.NullObject.class)
                result.complete(null);
            else if (payload.getClass() != resultTypeClass)
            	error.complete(new ErrorModel(new InternalFailureException(String.format("Unexpected payload type [%s]: expected [%s]" + payload.getClass(), resultTypeClass))));
            else
                result.complete((T) payload);
		}

        private void handleFrameForError(StompHeaders headers, Object payload) {
        	if (payload == null)
            	error.complete(new ErrorModel(new InternalFailureException("Received a null payload")));
			else if (payload instanceof ErrorModel)
				error.complete((ErrorModel) payload);
			else
				error.complete(new ErrorModel(new InternalFailureException(String.format("Unexpected payload type [%s]: expected [%s]" + payload.getClass(), ErrorModel.class))));
		}

        /**
         * Called when the server throws a generic websockets exception or a transport websockets exception.
         */
        private void notifyError() {
            if (!error.isDone() && !result.isDone())
                error.complete(new ErrorModel(new InternalFailureException("Unexpected error")));
        }

        private T getResult() throws NetworkExceptionResponse, InterruptedException, ExecutionException {
        	CompletableFuture.anyOf(result, error).get();

            if (error.isDone())
                throw new NetworkExceptionResponse(error.get());
            else
                return result.get();
        }
    }

    /**
     * Client session handler to handle the lifecycle of a STOMP session.
     */
    private class StompClientSessionHandler implements StompSessionHandler {

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            LOGGER.info("New session established: " + session.getSessionId());
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            LOGGER.error("STOMP Session exception", exception);
            onError();
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            LOGGER.error("STOMP Session Transport Error", exception);
            onError();
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {}

        private void onError() {
        	Send<?> lastSend = WebSocketsClient.this.lastSend;
        	if (lastSend != null)
        		lastSend.notifyError();

            try {
                // on session error, the session gets closed so we reconnect to the websocket endpoint
            	subscriptions.values().forEach(Subscription::unsubscribe);
                connect();
            }
            catch (ExecutionException | InterruptedException e) {
                throw InternalFailureException.of(e);
            }
        }
    }
}