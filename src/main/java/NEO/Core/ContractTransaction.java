package NEO.Core;

import NEO.IO.BinaryReader;

import java.io.IOException;

/**
 *  Global Asset Transfer
 *
 */
public class ContractTransaction extends Transaction {

	public ContractTransaction() {
		super(TransactionType.ContractTransaction);
	}

	@Override
	public void deserializeExclusiveData(BinaryReader reader) throws IOException {
		if (version != 0)
			throw new IOException();
	}
}
