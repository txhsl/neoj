package NEO.Core;

/**
 * 资产类别
 */
public enum AssetType {

    CreditFlag(0x40),
    DutyFlag(0x80),

    GoverningToken(0x00),

    UtilityToken(0x01),

	/**
     * 法币
     */
    Currency(0x08),

    /**
     * 股权
     */
    Share(DutyFlag.value | 0x10),

    /**
     * 电子发票
     */
    Invoice(DutyFlag.value | 0x10),

    /**
     * 代币
     */
    Token(CreditFlag.value | 0x11),
    ;

    private byte value;
    AssetType(int v) {
        value = (byte)v;
    }
    
    public byte value() {
        return value;
    }
    
    public static AssetType valueOf(byte v) {
    	for (AssetType tt : AssetType.values()) {
    		if(tt.value() == v) {
    			return tt;
    		}
    	}
    	throw new IllegalArgumentException();
    }
}
