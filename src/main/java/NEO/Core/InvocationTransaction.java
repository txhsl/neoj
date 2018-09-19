package NEO.Core;


import NEO.Core.Scripts.Program;
import NEO.Fixed8;
import NEO.IO.BinaryReader;
import NEO.IO.BinaryWriter;
import NEO.UInt160;
import NEO.Wallets.Contract;
import org.bouncycastle.math.ec.ECPoint;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;

public class InvocationTransaction extends Transaction {
	public byte[] script;
	public Fixed8 gas;
	public ECPoint invoker;

	public InvocationTransaction() {
		super(TransactionType.InvocationTransaction);
	}
	public InvocationTransaction(ECPoint invoker) {
		super(TransactionType.InvocationTransaction);
		this.invoker = invoker;
	}
	@Override
	protected void deserializeExclusiveData(BinaryReader reader) throws IOException {
		try {
			script = reader.readVarBytes(65536);
			if (version >= 1) {
				gas = reader.readSerializable(Fixed8.class);
				if (gas.compareTo(Fixed8.ZERO) < 0)
					throw new IOException();
			}
			else
				gas = Fixed8.ZERO;
		} catch (Exception e) {
			throw new IOException();
		}
	}
	@Override
	protected void serializeExclusiveData(BinaryWriter writer) throws IOException {
		writer.writeVarBytes(script);
		if (version >= 1)
			writer.writeSerializable(gas);
	}
	@Override
	public UInt160[] getScriptHashesForVerifying() {
		HashSet<UInt160> hashes = new HashSet<UInt160>(Arrays.asList(super.getScriptHashesForVerifying()));
		hashes.add(Program.toScriptHash(Contract.createSignatureRedeemScript(invoker)));
		return hashes.stream().sorted().toArray(UInt160[]::new);
	}
	@Override
	public Fixed8 systemFee(){
		return (gas == null) ? Fixed8.ZERO : gas;
	}

	public static Fixed8 getGas(Fixed8 consumed)
	{
		Fixed8 gas = consumed.subtract(Fixed8.fromDecimal(BigDecimal.TEN));
		if (gas.compareTo(Fixed8.ZERO) <= 0) return Fixed8.ZERO;
		return gas.ceiling();
	}
}
