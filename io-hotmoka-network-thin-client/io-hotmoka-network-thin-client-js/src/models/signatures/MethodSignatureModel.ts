import {CodeSignatureModel} from "./CodeSignatureModel";
import {MarshallingContext} from "../../internal/marshalling/MarshallingContext";

/**
 * The model of the signature of a method of a class.
 */
export abstract class MethodSignatureModel extends CodeSignatureModel {
    /**
     * The name of the method.
     */
    methodName: string


    constructor(methodName: string,
                definingClass: string,
                formals: Array<string>) {
        super(definingClass, formals)
        this.methodName = methodName
    }

    public into(context: MarshallingContext): void {
        super.into(context)
        context.writeString(this.methodName)
    }
}