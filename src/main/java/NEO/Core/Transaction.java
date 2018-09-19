package NEO.Core;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.*;

import NEO.*;
import NEO.Core.Scripts.Program;
import NEO.IO.*;
import NEO.IO.Json.*;
import NEO.Network.*;

/**
 *  交易
 */
public abstract class Transaction extends Inventory {
	/**
	 * 最大属性数量
	 */
	private final int MaxTransactionAttributes = 16;

	/**
	 * 交易类型
	 */
	public final TransactionType type;
	/**
	 * 版本
	 */
	public byte version = 0;
	/**
	 * 随机数
	 */
	public long nonce;
	/**
	 * 交易属性
	 */
	public TransactionAttribute[] attributes;
	/**
	 * 交易资产来源
	 */
	public TransactionInput[] inputs;
	/**
	 * 交易资产去向
	 */
	public TransactionOutput[] outputs;
	/**
	 * 验证脚本
	 */
	public Program[] scripts = new Program[0];
	
	protected Transaction(TransactionType type) {
		this.type = type;
	}
	
	@Override
	public void deserialize(BinaryReader reader) throws IOException {
		deserializeUnsigned(reader);
		try {
			scripts = reader.readSerializableArray(Program.class);
		} catch (InstantiationException | IllegalAccessException ex) {
			throw new RuntimeException(ex);
		}
		onDeserialized();
	}
	@Override
	public void deserializeUnsigned(BinaryReader reader) throws IOException {
        if (type.value() != reader.readByte()) { // type
            throw new IOException();
        }
        deserializeUnsignedWithoutType(reader);
	}

	private void deserializeUnsignedWithoutType(BinaryReader reader) throws IOException {
        try {
            version = reader.readByte();
            deserializeExclusiveData(reader);
			attributes = reader.readSerializableArray(TransactionAttribute.class, MaxTransactionAttributes);
	        inputs = reader.readSerializableArray(TransactionInput.class);
	        TransactionInput[] inputs_all = getAllInputs().toArray(TransactionInput[]::new);
	        for (int i = 1; i < inputs_all.length; i++) {
	            for (int j = 0; j < i; j++) {
	                if (inputs_all[i].prevHash == inputs_all[j].prevHash && inputs_all[i].prevIndex == inputs_all[j].prevIndex) {
	                    throw new IOException();
	                }
	            }
	        }
	        outputs = reader.readSerializableArray(TransactionOutput.class);
		} catch (InstantiationException | IllegalAccessException ex) {
			throw new IOException(ex);
		}
	}
	
	protected void deserializeExclusiveData(BinaryReader reader) throws IOException {
	}
	
	@Override
	public void serialize(BinaryWriter writer) throws IOException {
        serializeUnsigned(writer);
        writer.writeSerializableArray(scripts);
	}
	
	@Override
	public void serializeUnsigned(BinaryWriter writer) throws IOException {
        writer.writeByte(type.value());
        writer.writeByte(version);
        serializeExclusiveData(writer);
        writer.writeSerializableArray(attributes);
        writer.writeSerializableArray(inputs);
        writer.writeSerializableArray(outputs);
	}
	
	protected void serializeExclusiveData(BinaryWriter writer) throws IOException {
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Transaction)) {
			return false;
		}
		Transaction tx = (Transaction)obj;
		return hash().equals(tx.hash());
	}
	
	@Override
	public int hashCode() {
		return hash().hashCode();
	}
	
	/**
     * 反序列化Transaction(static)
     */
	public static Transaction deserializeFrom(byte[] value) throws IOException {
		return deserializeFrom(value, 0);
	}
	
	public static Transaction deserializeFrom(byte[] value, int offset) throws IOException {
		try (ByteArrayInputStream ms = new ByteArrayInputStream(value, offset, value.length - offset)) {
			try (BinaryReader reader = new BinaryReader(ms)) {
				return deserializeFrom(reader);
			}
		}
	}

	public static Transaction deserializeFrom(BinaryReader reader) throws IOException {
        try {
            TransactionType type = TransactionType.valueOf(reader.readByte());
            String typeName = "NEO.Core." + type.toString();
            Transaction transaction = (Transaction)Class.forName(typeName).newInstance();
            transaction.deserializeUnsignedWithoutType(reader);
			transaction.scripts = reader.readSerializableArray(Program.class);
			return transaction;
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
			throw new IOException(ex);
		}
	}
	
	public Stream<TransactionInput> getAllInputs() {
		return Arrays.stream(inputs);
	}
	
	public Stream<TransactionOutput> getAllOutputs() {
		return Arrays.stream(outputs);
	}
	
	/**
	 * 获取验证脚本
	 */
	@Override
	public UInt160[] getScriptHashesForVerifying() {
        if (references() == null) throw new IllegalStateException();
        HashSet<UInt160> hashes = new HashSet<UInt160>(getAllInputs().map(p -> references().get(p).scriptHash).collect(Collectors.toList()));
        for (Entry<UInt256, List<TransactionOutput>> group : getAllOutputs().collect(Collectors.groupingBy(p -> p.assetId)).entrySet()) {
            Transaction tx;
			try {
				tx = Blockchain.current().getTransaction(group.getKey());
			} catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
            if (tx == null || !(tx instanceof RegisterTransaction)) { 
            	throw new IllegalStateException();
            }
        }
        return hashes.stream().sorted().toArray(UInt160[]::new);
	}
	
    public TransactionResult[] getTransactionResults() {
        if (references() == null) return null;
        Stream<TransactionResult> in = references().values().stream().map(p -> new TransactionResult(p.assetId, p.value));
        Stream<TransactionResult> out = getAllOutputs().map(p -> new TransactionResult(p.assetId, p.value.negate()));
        Map<UInt256, Fixed8> results = Stream.concat(in, out).collect(Collectors.toMap(p -> p.assetId, p -> p.amount, (a, b) -> a.add(b)));
        return results.entrySet().stream().filter(p -> !p.getValue().equals(Fixed8.ZERO)).map(p -> new TransactionResult(p.getKey(), p.getValue())).toArray(TransactionResult[]::new);
    }

	@Override
	public final InventoryType inventoryType() {
		return InventoryType.TX;
	}
	
	public JObject json() {
        JObject json = new JObject();
        json.set("txid", new JString(hash().toString()));
		json.set("TxType", new JString(type.toString()));
		json.set("PayloadVersion", new JString(String.valueOf(version)));
		json.set("Nonce", new JNumber(nonce));
		json.set("Attributes", new JArray(Arrays.stream(attributes).map(p -> p.json()).toArray(JObject[]::new)));
		json.set("UTXOInputs", new JArray(Arrays.stream(inputs).map(p -> p.json()).toArray(JObject[]::new)));
		json.set("Outputs", new JArray(IntStream.range(0, outputs.length).boxed().map(i -> outputs[i].json(i)).toArray(JObject[]::new)));
		json.set("Programs", new JArray(Arrays.stream(scripts).map(p -> p.json()).toArray(JObject[]::new)));
		return json;
	}
	
	
	protected void onDeserialized() throws IOException {
	}
	
    //[NonSerialized]
    private Map<TransactionInput, TransactionOutput> _references = null;
    public Map<TransactionInput, TransactionOutput> references() {
        if (_references == null) {
        	Map<TransactionInput, TransactionOutput> map = new HashMap<TransactionInput, TransactionOutput>();
            for (Entry<UInt256, List<TransactionInput>> entry : getAllInputs().collect(Collectors.groupingBy(p -> p.prevHash)).entrySet()) {
                Transaction tx;
				try {
					tx = Blockchain.current().getTransaction(entry.getKey());
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
                if (tx == null) {
                	return null;
                }
                for (TransactionInput input : entry.getValue()) {
                    map.put(input, tx.outputs[input.prevIndex]);
                }
            }
            _references = map;
        }
        return _references;
	}

	/**
	 * 系统费用
	 */
	public Fixed8 systemFee() {
		switch (type){
			case IssueTransaction:
				return Fixed8.fromLong(Settings.ISSUE_TX_FEE);
			case PublishTransaction:
				return Fixed8.fromLong(Settings.PUBLISH_TX_FEE);
			case EnrollmentTransaction:
				return Fixed8.fromLong(Settings.ENROLLMENT_TX_FEE);
			case RegisterTransaction:
				return Fixed8.fromLong(Settings.REGISTER_TX_FEE);
			default:
				return Fixed8.ZERO;
		}
	}

	/**
	 * 网络费用
	 */
	private Fixed8 _networkFee = null;
	public Fixed8 networkFee() {

		if (_networkFee == null) {
			Fixed8 input = Fixed8.ZERO;

			Map<TransactionInput, TransactionOutput> refs = references();

			for (TransactionInput coinRef : refs.keySet()) {
				TransactionOutput to = refs.get(coinRef);
				//if (to.assetId == Blockchain.utilityToken().hash()) {
				if (to.assetId == Blockchain.UtilityToken) {
					input = input.add(to.value);
				}
			}

			Fixed8 output = Fixed8.ZERO;

			for (TransactionOutput to : outputs) {
				//if (to.assetId == Blockchain.utilityToken().hash()) {
				if (to.assetId == Blockchain.UtilityToken) {
					output = output.add(to.value);
				}
			}
			_networkFee = (input.subtract(output)).subtract(systemFee());
		}

		return _networkFee;
	}

	/**
	 * 校验
	 */
	@Override
	public boolean verify() {
		return true;
	}

	public boolean verify(Transaction[] mempool) {
		for (int i = 1; i < inputs.length; i++) {
			for (int j = 0; j < i; j++) {
				if (inputs[i].prevHash == inputs[j].prevHash && inputs[i].prevIndex == inputs[j].prevIndex)
					return false;
			}
		}
		if (Arrays.stream(mempool).filter(p -> p != this).map(p -> p.inputs).filter(p -> Arrays.asList(inputs).contains(p)).count() > 0)
			return false;
		if (Blockchain.current().isDoubleSpend(this))
			return false;
		//for (Map.Entry<UInt256, List<TransactionOutput>> group : Arrays.stream(outputs).collect(Collectors.groupingBy(p -> p.assetId)).entrySet()) {

		TransactionResult[] results = getTransactionResults();
		if (results == null)
			return false;
		TransactionResult[] results_destroy = Arrays.stream(results).filter(p -> p.amount.compareTo(Fixed8.ZERO) > 0).toArray(TransactionResult[]::new);
		if (results_destroy.length > 1)
			return false;
		//if (results_destroy.length == 1 && results_destroy[0].assetId != Blockchain.utilityToken().hash())
		if (results_destroy.length == 1 && results_destroy[0].assetId != Blockchain.UtilityToken)
			return false;
		if (systemFee().compareTo(Fixed8.ZERO) > 0 && (results_destroy.length == 0 || results_destroy[0].amount.compareTo(systemFee()) < 0))
			return false;
		TransactionResult[] results_issue = Arrays.stream(results).filter(p -> p.amount.compareTo(Fixed8.ZERO) < 0).toArray(TransactionResult[]::new);
		switch (type) {
			case MinerTransaction:
			case ClaimTransaction:
				//if (Arrays.stream(results_issue).allMatch(p -> p.assetId != Blockchain.utilityToken().hash()))
				if (Arrays.stream(results_issue).allMatch(p -> p.assetId != Blockchain.UtilityToken))
					return false;
				break;
			case IssueTransaction:
				//if (Arrays.stream(results_issue).allMatch(p -> p.assetId == Blockchain.utilityToken().hash()))
				if (Arrays.stream(results_issue).allMatch(p -> p.assetId == Blockchain.UtilityToken))
					return false;
				break;
				default:
					if (results_issue.length > 0)
						return false;
					break;
		}
		if (Arrays.stream(attributes).filter(p -> p.usage == TransactionAttributeUsage.ECDH02 || p.usage == TransactionAttributeUsage.ECDH03).count() > 1)
			return false;
		//return this.VerifyScripts();
		return true;
	}
}
