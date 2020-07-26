package io.hotmoka.network.internal.models.requests;

import java.util.List;

import io.hotmoka.network.internal.models.storage.ValueModel;

public abstract class MethodCallTransactionRequestModel extends NonInitialTransactionRequestModel {
	private String constructorType;
    private boolean voidReturnType;
    private String methodName;
    private String returnType;
    private List<ValueModel> values;

    public String getConstructorType() {
        return constructorType;
    }

    public void setConstructorType(String constructorType) {
        this.constructorType = constructorType;
    }

    public boolean isVoidReturnType() {
        return voidReturnType;
    }

    public void setVoidReturnType(boolean voidReturnType) {
        this.voidReturnType = voidReturnType;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public List<ValueModel> getValues() {
        return values;
    }

    public void setValues(List<ValueModel> values) {
        this.values = values;
    }
}