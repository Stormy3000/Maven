import {expect} from "chai";
import {MarshallingContext} from "../src/internal/marshalling/MarshallingContext";
import {FieldSignatureModel} from "../src/models/signatures/FieldSignatureModel";
import {StorageReferenceModel} from "../src/models/values/StorageReferenceModel";
import {TransactionReferenceModel} from "../src/models/values/TransactionReferenceModel";


describe('Testing the marshalling of the JS objects to base64', () => {

    it('writeShort(22) = rO0ABXcCABY=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeShort(22)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcCABY=')
    })

    it('writeInt(32) = rO0ABXcEAAAAIA==', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeInt(32)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcEAAAAIA==')
    })

    it('writeLong(92) = rO0ABXcIAAAAAAAAAFw=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeLong(92)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcIAAAAAAAAAFw=')
    })

    it('writeLong(1000129) = rO0ABXcIAAAAAAAPQsE=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeLong(1000129)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcIAAAAAAAPQsE=')
    })

    it('writeLong(9007199254740991) = rO0ABXcIAB////////8=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeLong(9007199254740991)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcIAB////////8=')
    })


    it('writeBigInteger(9007199254740991) = rO0ABXcJAgAf////////', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeBigInteger(9007199254740991)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcJAgAf////////')
    })

    it('writeBigInteger(9) = rO0ABXcBDQ==', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeBigInteger(9)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcBDQ==')
    })

    it('writeBigInteger(7654319) = rO0ABXcFAQB0y68=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeBigInteger(7654319)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcFAQB0y68=')
    })

    /*
    it('writeBigInteger(9007199254740991765896) = rO0ABXcMAwoB6Ef//////G2I', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeBigInteger(9007199254740991765896)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcMAwoB6Ef//////G2I')
    })

     */

    it('writeFloat(33.8) = rO0ABXcEQgczMw==', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeFloat(33.8)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcEQgczMw==')
    })

    it('writeBoolean(true) = rO0ABXcBAQ==', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeBoolean(true)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcBAQ==')
    })

    it('writeChar("d") = rO0ABXcCAGQ=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeChar("d")
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcCAGQ=')
    })

    it('writeString("hello world") = rO0ABXcNAAtoZWxsbyB3b3JsZA==', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeString("hello world")
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcNAAtoZWxsbyB3b3JsZA==')
    })

    it('writeBuffer(Buffer.from("hello world")) = rO0ABXcLaGVsbG8gd29ybGQ=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeBuffer(Buffer.from("hello world"))
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcLaGVsbG8gd29ybGQ=')
    })

    it('writeCompactInt(30006) = rO0ABXcF/wAAdTY=', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeCompactInt(30006)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcF/wAAdTY=')
    })

    it('writeStringShared(Hotmoka") = rO0ABXcK/wAHSG90bW9rYQ==', async () => {

        const marshallingContext = new MarshallingContext()
        marshallingContext.writeStringShared("Hotmoka")
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcK/wAHSG90bW9rYQ==')
    })


    it.skip('writeFieldSignature(fieldSignature") = rO0ABXcM/xQAB2JhbGFuY2Ua', async () => {
        const fieldSignature = new FieldSignatureModel("balance", "java.math.BigInteger", "io.takamaka.code.lang.Contract")
        const marshallingContext = new MarshallingContext()
        marshallingContext.writeFieldSignature(fieldSignature)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcM/xQAB2JhbGFuY2Ua')
    })

    it.skip('writeStorageReference(storageReference") = rO0ABXcl///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggBOGA==', async () => {
        const storageReference = new StorageReferenceModel(
            new TransactionReferenceModel("local", "d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
            "19992"
        )
        const marshallingContext = new MarshallingContext()
        marshallingContext.writeStorageReference(storageReference)
        marshallingContext.flush()

        const result = marshallingContext.toBase64()
        expect(result).to.be.eq('rO0ABXcl///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggBOGA==')
    })



})

