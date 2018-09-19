package NEO.Wallets;

import NEO.Core.TransactionOutput;
import NEO.Fixed8;
import NEO.UInt160;
import NEO.UInt256;
import NEO.UIntBase;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

/**
 * @author: HuShili
 * @date: 2018/9/10
 * @description: none
 */
public class TransferOutput {
    public UIntBase assetId;
    public BigDecimal value;
    public UInt160 scriptHash;

    public TransferOutput(UIntBase assetId, BigDecimal value, UInt160 scriptHash){
        this.assetId = assetId;
        this.value = value;
        this.scriptHash = scriptHash;
    }

    public boolean IsGlobalAsset() {
        return assetId.size() == 32;
    }

    public TransactionOutput ToTxOutput() {

        if (assetId.getClass() == UInt256.class) {
            TransactionOutput to = new TransactionOutput();
            to.assetId = (UInt256) assetId;
            to.value = Fixed8.fromDecimal(value);
            to.scriptHash = scriptHash;

            return to;
        }
        throw new NoSuchElementException();
    }
}
