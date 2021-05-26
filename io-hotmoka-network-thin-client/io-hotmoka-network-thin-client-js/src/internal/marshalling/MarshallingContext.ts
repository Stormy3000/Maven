import {Buffer} from "buffer";

/**
 * A context used during object marshalling into bytes.
 */
export class MarshallingContext {
    /**
     * Java ObjectOutputStream header signature.
     */
    public readonly STREAM_MAGIC = 44269
    public readonly STREAM_VERSION = 5

    /**
     * Block of optional data. Byte following tag indicates number of bytes in this block data.
     */
    private readonly TC_BLOCKDATA = 119
    /**
     * Long Block data. The long following the tag indicates the number of bytes in this block data.
     */
    private readonly TC_BLOCKDATALONG = 122

    /**
     * The buffer to write to.
     */
    private buffer = Buffer.alloc(1024)

    /**
     * The offset of the block body buffer.
     */
    private offset = 0


    /**
     * Writes block data header. Data blocks shorter than 256 bytes
     * are prefixed with a 2-byte header; all others start with
     * a 5-byte header.
     * @return the block data header buffer
     */
    private writeBlockHeader(len: number): Buffer {
        let buffer: Buffer;

        if (len <= 255) {
            buffer = Buffer.alloc(2)
            MarshallingContext.writeByte(buffer, this.TC_BLOCKDATA, 0)
            MarshallingContext.writeByte(buffer, len, 1)
        } else {
            buffer = Buffer.alloc(5)
            MarshallingContext.writeByte(buffer, this.TC_BLOCKDATALONG, 0)
            MarshallingContext.writeByte(buffer, len >>> 24, 1)
            MarshallingContext.writeByte(buffer, len >>> 16, 2)
            MarshallingContext.writeByte(buffer, len >>> 8, 3)
            MarshallingContext.writeByte(buffer, len, 4)
        }

        return buffer
    }

    /**
     * It writes the magic number and version to the stream.
     * @return the stream header buffer
     */
    private writeStreamHeader(): Buffer {
        const buffer = Buffer.alloc(4)
        MarshallingContext.writeByte(buffer, this.STREAM_MAGIC >>> 8, 0)
        MarshallingContext.writeByte(buffer, this.STREAM_MAGIC, 1)
        MarshallingContext.writeByte(buffer, this.STREAM_VERSION >>> 8, 2)
        MarshallingContext.writeByte(buffer, this.STREAM_VERSION, 3)
        return buffer
    }

    /**
     * It returns the buffer.
     * @return the buffer
     */
    public getBuffer(): Buffer {
        return this.buffer
    }

    /**
     * It returns the base64 string representation of the buffer.
     * @return the base64 string representation of the buffer
     */
    public toBase64(): string {
        return this.buffer.toString('base64')
    }

    /**
     * Flushes the stream. This will write any buffered output bytes and flush through to the underlying stream.
     */
    public flush(): void {
        const streamHeaderBuffer = this.writeStreamHeader()
        const blockHeaderBuffer = this.writeBlockHeader(this.offset)
        const blockBodyBuffer = Buffer.alloc(this.offset)
        this.buffer.copy(blockBodyBuffer, 0, 0, this.offset)

        this.buffer = Buffer.concat([streamHeaderBuffer, blockHeaderBuffer, blockBodyBuffer])
    }


    /**
     * Writes a string.
     * @param str the string
     */
    public write(str: string): void {
        this.buffer.write(str)
    }

    /**
     * Writes a 16 bit short.
     * @param val the value
     */
    public writeShort(val: number): void {
        this.writeByte(val >>> 8)
        this.writeByte(val)
    }

    /**
     * Writes a 32 bit int.
     * @param val the value
     */
    public writeInt(val: number): void {
        this.buffer.writeInt32BE(val, this.offset)
        this.offset += 4
    }

    /**
     * Writes a 64 bit long.
     * @param val the value
     */
    public writeLong(val: number): void {
        this.buffer.writeBigInt64BE(BigInt(val), this.offset)
        this.offset += 8
    }

    /**
     * Writes an 8 bit byte.
     * @param val the value
     */
    public writeByte(val: number): void {
        this.buffer.writeInt8(MarshallingContext.toByte(val), this.offset++)
    }

    /**
     * Writes the given integer, in a way that compacts small integers.
     * @param val the integer
     */
    public writeCompactInt(val: number): void {
        if (val < 255) {
            this.writeInt(val)
        } else {
            this.writeByte(255)
            this.writeInt(val)
        }
    }

    /**
     * Writes the given big integer, in a compact way.
     * @param biValue the big integer
     */
    public writeBigInteger(biValue: number): void {
        const small = MarshallingContext.toShort(biValue)

        if (biValue === small) {
            if (0 <= small && small <= 251)
                this.writeByte(4 + small)
            else {
                this.writeByte(0)
                this.writeShort(small)
            }
        } else if (biValue === MarshallingContext.toInt(biValue)) {
            this.writeByte(1)
            this.writeInt(MarshallingContext.toInt(biValue))
        } else if (BigInt(biValue) === MarshallingContext.toBigint(biValue).valueOf()) {
            this.writeByte(2)
            this.writeLong(biValue)
        } else {
            this.writeByte(3)
            const bytes = Buffer.from([biValue])
            this.writeCompactInt(bytes.length)
            this.writeBuffer(bytes)
        }
    }

    /**
     * Writes a buffer.
     * @param buff the buffer
     */
    public writeBuffer(buff: Buffer): void {
        this.buffer = Buffer.concat([this.buffer, buff])
    }

    /**
     * Writes an array of buffers.
     * @param buffers the array of buffers
     */
    public writeBuffers(buffers: Array<Buffer>): void {
        this.writeCompactInt(buffers.length)
        this.buffer = Buffer.concat([this.buffer, ...buffers])
    }

    /**
     * Writes an 8 bit byte to a buffer.
     * @param buffer the buffer
     * @param val the value
     * @param offset the offset
     */
    public static writeByte(buffer: Buffer, val: number, offset: number): void {
        buffer.writeInt8(MarshallingContext.toByte(val), offset)
    }

    public static toShort(val: number): number {
        const int16 = new Int16Array(1)
        int16[0] = val
        return int16[0]
    }

    public static toByte(val: number): number {
        const int8 = new Int8Array(1)
        int8[0] = val
        return int8[0]
    }

    public static toInt(val: number): number {
        const int32 = new Int32Array(1)
        int32[0] = val
        return int32[0]
    }

    public static toBigint(val: number): bigint {
        const bigInt64 = new BigInt64Array(1)
        bigInt64[0] = BigInt(val)
        return bigInt64[0]
    }

}