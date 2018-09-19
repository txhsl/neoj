package NEO.Core;

import NEO.Core.Scripts.Program;
import NEO.Cryptography.ECC;
import NEO.IO.BinaryReader;
import NEO.IO.BinaryWriter;
import NEO.UInt160;
import NEO.Wallets.Contract;
import org.bouncycastle.math.ec.ECPoint;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;

public class EnrollmentTransaction extends Transaction{
    /**
     * 记账人公钥
     */
    public ECPoint publicKey;

    private UInt160 _scriptHash;

    private UInt160 scriptHash() {
        if (_scriptHash == null) {
            _scriptHash = Program.toScriptHash(Contract.createSignatureRedeemScript(publicKey));
        }
        return _scriptHash;
    }

    public EnrollmentTransaction() {
        super(TransactionType.EnrollmentTransaction);
    }

    @Override
    protected void deserializeExclusiveData(BinaryReader reader) throws IOException {
        byte[] xx = reader.readVarBytes();
        byte[] yy = reader.readVarBytes();
        publicKey = ECC.secp256r1.getCurve().createPoint(
                new BigInteger(1,xx), new BigInteger(1,yy));
    }

    @Override
    public UInt160[] getScriptHashesForVerifying() {
        HashSet<UInt160> hashs = new HashSet<UInt160>(Arrays.asList(super.getScriptHashesForVerifying()));
        hashs.add(Program.toScriptHash(Contract.createSignatureRedeemScript(publicKey)));
        return hashs.stream().sorted().toArray(UInt160[]::new);
    }

    @Override
    protected void serializeExclusiveData(BinaryWriter writer) throws IOException {
//        writer.writeVarBytes(issuer.getXCoord().toBigInteger().toByteArray());
//        writer.writeVarBytes(issuer.getYCoord().toBigInteger().toByteArray());
    }

}
