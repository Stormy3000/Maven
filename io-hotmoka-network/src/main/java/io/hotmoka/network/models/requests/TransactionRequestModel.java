package io.hotmoka.network.models.requests;

import java.util.Base64;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.requests.TransactionRequest;

@Immutable
public abstract class TransactionRequestModel {
    protected final byte[] decodeBase64(String what) {
    	return Base64.getDecoder().decode(what);
    }

    /**
     * Builds the model of the given request.
     * 
     * @param request the request
     * @return the corresponding model
     */
    public static TransactionRequestModel from(TransactionRequest<?> request) {
    	if (request == null)
    		throw new InternalFailureException("unexpected null request");
    	else if (request instanceof ConstructorCallTransactionRequest)
    		return new ConstructorCallTransactionRequestModel((ConstructorCallTransactionRequest) request);
    	else if (request instanceof GameteCreationTransactionRequest)
    		return new GameteCreationTransactionRequestModel((GameteCreationTransactionRequest) request);
    	else if (request instanceof InitializationTransactionRequest)
    		return new InitializationTransactionRequestModel((InitializationTransactionRequest) request);
    	else if (request instanceof InstanceMethodCallTransactionRequest)
    		return new InstanceMethodCallTransactionRequestModel((InstanceMethodCallTransactionRequest) request);
    	else if (request instanceof JarStoreInitialTransactionRequest)
    		return new JarStoreInitialTransactionRequestModel((JarStoreInitialTransactionRequest) request);
    	else if (request instanceof JarStoreTransactionRequest)
    		return new JarStoreTransactionRequestModel((JarStoreTransactionRequest) request);
    	else if (request instanceof RedGreenGameteCreationTransactionRequest)
    		return new RedGreenGameteCreationTransactionRequestModel((RedGreenGameteCreationTransactionRequest) request);
    	else if (request instanceof StaticMethodCallTransactionRequest)
    		return new StaticMethodCallTransactionRequestModel((StaticMethodCallTransactionRequest) request);
    	else
    		throw new InternalFailureException("unexpected transaction request of class " + request.getClass().getName());
    }

	/**
	 * Build the transaction request from the given model.
	 * @param requestModel the request model
	 * @return the corresponding transaction request
	 */
	public static TransactionRequest<?> toBeanFrom(TransactionRequestModel requestModel) {
		if (requestModel == null)
			throw new InternalFailureException("unexpected null request model");
		else if (requestModel instanceof ConstructorCallTransactionRequestModel)
			return ((ConstructorCallTransactionRequestModel) requestModel).toBean();
		else if (requestModel instanceof GameteCreationTransactionRequestModel)
			return ((GameteCreationTransactionRequestModel) requestModel).toBean();
		else if (requestModel instanceof InitializationTransactionRequestModel)
			return ((InitializationTransactionRequestModel) requestModel).toBean();
		else if (requestModel instanceof InstanceMethodCallTransactionRequestModel)
			return ((InstanceMethodCallTransactionRequestModel) requestModel).toBean();
		else if (requestModel instanceof JarStoreInitialTransactionRequestModel)
			return ((JarStoreInitialTransactionRequestModel) requestModel).toBean();
		else if (requestModel instanceof JarStoreTransactionRequestModel)
			return ((JarStoreTransactionRequestModel) requestModel).toBean();
		else if (requestModel instanceof RedGreenGameteCreationTransactionRequestModel)
			return ((RedGreenGameteCreationTransactionRequestModel) requestModel).toBean();
		else if (requestModel instanceof StaticMethodCallTransactionRequestModel)
			return ((StaticMethodCallTransactionRequestModel) requestModel).toBean();
		else
			throw new InternalFailureException("unexpected transaction request model of class " + requestModel.getClass().getName());
	}
}