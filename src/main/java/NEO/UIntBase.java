package NEO;

import java.io.IOException;
import java.nio.*;
import java.util.Arrays;

import NEO.IO.*;

/**
 * Custom type base abstract class, it defines the storage and the serialization 
 * and deserialization of actual data
 * 
 */
public abstract class UIntBase implements Serializable {
    protected byte[] data_bytes;

    protected UIntBase(int bytes, byte[] value) {
        if (value == null) {
            this.data_bytes = new byte[bytes];
            return;
        }
        if (value.length != bytes) {
        	throw new IllegalArgumentException();
        }
        this.data_bytes = value;
    }

    public int size(){
        return  data_bytes.length;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof UIntBase)) { 
            return false;
        }
        UIntBase other = (UIntBase) obj;
        return Arrays.equals(this.data_bytes, other.data_bytes);
    }

    @Override
    public int hashCode() {
        return ByteBuffer.wrap(data_bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public byte[] toArray() {
        return data_bytes;
    }

    /**
     * 转为16进制字符串(Hex decimal format string in BIG ENDIAN)
     * @return 返回16进制字符串(Hex decimal format string in BIG ENDIAN)
     */
    @Override
    public String toString() {
        return Helper.toHexString(Helper.reverse(data_bytes));
    }
    
    @Override
    public void serialize(BinaryWriter writer) throws IOException {
    	writer.write(data_bytes);
    }
    
    @Override
    public void deserialize(BinaryReader reader) throws IOException {
    	reader.read(data_bytes);
    }
}
