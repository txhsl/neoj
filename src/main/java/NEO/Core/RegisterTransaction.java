package NEO.Core;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;

import NEO.Helper;
import org.bouncycastle.math.ec.ECPoint;

import NEO.Fixed8;
import NEO.UInt160;
import NEO.Core.Scripts.Program;
import NEO.Cryptography.ECC;
import NEO.IO.BinaryReader;
import NEO.IO.BinaryWriter;
import NEO.IO.Json.JNumber;
import NEO.IO.Json.JObject;
import NEO.IO.Json.JString;
import NEO.Wallets.Contract;
import org.bouncycastle.math.ec.custom.sec.SecP256R1Curve;


/**
 *  注册资产交易
 *  
 */
public class RegisterTransaction extends Transaction {
	/**
	 * 资产名称
	 */
	public String name;				// 资产名称
	/**
	 * 资产描述
	 */
	public String description;		// 资产描述
	/**
	 * 精度
	 */
	public byte precision;			// 精度
	/**
	 * 资产类型
	 */
	public AssetType assetType;		// 资产类型
	/**
	 * 资产名称
	 */
	public RecordType recordType;	// 记账模式
	/**
	 * 资产数量
	 */
	public Fixed8 amount;			// 资产数量
	/**
	 * 发行者公钥
	 */
	public ECPoint issuer;			// 发行者公钥
	/**
	 * 管理者地址
	 */
	public UInt160 admin;			// 管理者地址
	
	public RegisterTransaction() {
		super(TransactionType.RegisterTransaction);
	}
	
	@Override
	protected void deserializeExclusiveData(BinaryReader reader) throws IOException {
		try {
			assetType = AssetType.valueOf(reader.readByte());
			name = reader.readVarString();
			amount = reader.readSerializable(Fixed8.class);
			//description = reader.readVarString();
			precision = reader.readByte();
			//recordType = RecordType.valueOf(reader.readByte());
	        //issuer = ECC.deserializeFrom(reader, new SecP256R1Curve());
			byte[] xx = reader.readVarBytes();
			byte[] yy = reader.readVarBytes();
			issuer = ECC.secp256r1.getCurve().createPoint(
					new BigInteger(1,xx), new BigInteger(1,yy));
	        admin = reader.readSerializable(UInt160.class);
		} catch (Exception ex) {
			throw new IOException(ex);
		}
	}
	@Override
	protected void onDeserialized() throws IOException {
		//if (assetType == AssetType.GoverningToken && !hash().equals(Blockchain.governingToken().hash()))
		if (assetType == AssetType.GoverningToken && !hash().equals(Blockchain.GoverningToken))
			throw new IOException();
		//if (assetType == AssetType.UtilityToken && !hash().equals(Blockchain.utilityToken().hash()))
		if (assetType == AssetType.UtilityToken && !hash().equals(Blockchain.UtilityToken))
			throw new IOException();
	}
	@Override
	protected void serializeExclusiveData(BinaryWriter writer) throws IOException {
        writer.writeByte(assetType.value());
        writer.writeVarString(name);
        writer.writeSerializable(amount);
        writer.writeByte(precision);
		//writer.writeVarBytes(ECC.encodePoint(issuer, true));
		writer.writeVarBytes(Helper.removePrevZero(issuer.getXCoord().toBigInteger().toByteArray()));
		writer.writeVarBytes(Helper.removePrevZero(issuer.getYCoord().toBigInteger().toByteArray()));
		writer.writeSerializable(admin);
	}
	
	/**
	 * 获取验证脚本
	 */
	@Override
	public UInt160[] getScriptHashesForVerifying() {
        HashSet<UInt160> hashes = new HashSet<UInt160>(Arrays.asList(super.getScriptHashesForVerifying()));
        hashes.add(Program.toScriptHash(Contract.createSignatureRedeemScript(issuer)));
        return hashes.stream().sorted().toArray(UInt160[]::new);
	}


	
	@Override
    public JObject json() {
        JObject json = super.json();
        json.set("Asset", new JObject());
        json.get("Asset").set("Name", new JString(name));
        json.get("Asset").set("Precision", new JNumber(precision));
        json.get("Asset").set("AssetType", new JString(String.valueOf(assetType.value())));
        json.get("Asset").set("RecordType", new JString(String.valueOf(recordType)));
        json.set("Amount", new JNumber(amount.toLong()));
        json.set("Issuer", new JObject());
        json.get("Issuer").set("X", new JString(issuer.getXCoord().toBigInteger().toString()));
        json.get("Issuer").set("Y", new JString(issuer.getYCoord().toBigInteger().toString()));
        json.set("Controller", new JString(admin.toString()));
        return json;
    }

	@Override
	public String toString() {
		return "RegisterTransaction [name=" + name + "]";
	}

}
