package io.hotmoka.network.models.requests;

import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.models.signatures.ConstructorSignatureModel;
import io.hotmoka.network.models.values.StorageValueModel;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The model of a constructor call transaction.
 */
public class ConstructorCallTransactionRequestModel extends NonInitialTransactionRequestModel {
    public ConstructorSignatureModel constructor;
    public List<StorageValueModel> actuals;

    public ConstructorCallTransactionRequestModel() {}


    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public ConstructorCallTransactionRequestModel(ConstructorCallTransactionRequest request) {
    	super(request);

    	this.constructor = new ConstructorSignatureModel(request.constructor);
    	this.actuals = request.actuals().map(StorageValueModel::new).collect(Collectors.toList());
    }

    public ConstructorCallTransactionRequest toBean() {
    	return new ConstructorCallTransactionRequest(
        	decodeBase64(signature),
            caller.toBean(),
            new BigInteger(nonce),
            chainId,
            new BigInteger(gasLimit),
            new BigInteger(gasPrice),
            classpath.toBean(),
            constructor.toBean(),
            actuals.stream().map(StorageValueModel::toBean).toArray(StorageValue[]::new));
    }
}