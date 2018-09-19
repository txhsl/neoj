package NEO.sdk;

import NEO.Core.*;
import NEO.Core.Contract.ContractParameterType;
import NEO.Core.Scripts.Program;
import NEO.Core.Scripts.ScriptBuilder;
import NEO.Fixed8;
import NEO.Helper;
import NEO.UInt160;
import NEO.UInt256;
import NEO.Wallets.Account;
import NEO.Wallets.Contract;
import NEO.Wallets.Wallet;
import NEO.sdk.abi.AbiFunction;
import NEO.sdk.abi.Parameter;
import com.alibaba.fastjson.JSON;
import org.bouncycastle.math.ec.ECPoint;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 */
public class SmartContractTx {

    public SmartContractTx(){

    }
    public static Transaction makeInvocationTransaction(String contractAddress, ECPoint publicKey, AbiFunction abiFunction, Fixed8 fee) throws Exception {
        if (contractAddress == null) {
            throw new Exception("null contractHash");
        }
        contractAddress = contractAddress.replace("0x", "");
        List list = new ArrayList<Object>();
        list.add(abiFunction.getName().getBytes());
        List tmp = new ArrayList<Object>();
        for (Parameter obj : abiFunction.getParameters()) {
            if ("ByteArray".equals(obj.getType())) {
                tmp.add(JSON.parseObject(obj.getValue(), byte[].class));
            } else if ("String".equals(obj.getType())) {
                tmp.add(obj.getValue());
            } else if ("Boolean".equals(obj.getType())) {
                tmp.add(JSON.parseObject(obj.getValue(), boolean.class));
            } else if ("Integer".equals(obj.getType())) {
                tmp.add(JSON.parseObject(obj.getValue(), Long.class));
            } else if ("Array".equals(obj.getType())) {
                tmp.add(JSON.parseObject(obj.getValue(), Array.class));
            } else if ("InteropInterface".equals(obj.getType())) {
                tmp.add(JSON.parseObject(obj.getValue(), Object.class));
            } else if ("Void".equals(obj.getType())) {

            } else {
                throw new Exception("type error");
            }
        }
        if(list.size()>0) {
            list.add(tmp);
        }
        byte[] params = createCodeParamsScript(list);
        params = Helper.addBytes(params, new byte[]{0x67});
        params = Helper.addBytes(params, Helper.hexToBytes(contractAddress));

        Transaction tx = makeInvocationTransaction(params, publicKey, fee);
        return tx;
    }


    private static byte[] createCodeParamsScript(ScriptBuilder builder, List<Object> list) {
        try {
            for (int i = list.size() - 1; i >= 0; i--) {
                Object val = list.get(i);
                if (val instanceof byte[]) {
                    builder.push((byte[]) val);
                } else if (val instanceof Boolean) {
                    builder.push((Boolean) val);
                } else if (val instanceof Long) {
                    builder.push(BigInteger.valueOf((long)val));
                } else if (val instanceof List) {
                    List tmp = (List) val;
                    createCodeParamsScript(builder, tmp);
                    builder.push(new BigInteger(String.valueOf(tmp.size())));
                    builder.pushPack();

                } else {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return builder.toArray();
    }

    /**
     * @param list
     * @return
     */
    public static byte[] createCodeParamsScript(List<Object> list) {
        ScriptBuilder sb = new ScriptBuilder();
        try {
            for (int i = list.size() - 1; i >= 0; i--) {
                Object val = list.get(i);
                if (val instanceof byte[]) {
                    sb.push((byte[]) val);
                } else if (val instanceof Boolean) {
                    sb.push((Boolean) val);
                } else if (val instanceof Long) {
                    sb.push(BigInteger.valueOf(((long)val)));
                } else if (val instanceof List) {
                    List tmp = (List) val;
                    createCodeParamsScript(sb, tmp);
                    sb.push(new BigInteger(String.valueOf(tmp.size())));
                    sb.pushPack();
                } else {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toArray();
    }

    public static PublishTransaction makePublishTransaction(String codeStr, boolean needStorage, String name, String codeVersion, String author, String email, String desp, ContractParameterType returnType) throws Exception {
        PublishTransaction tx = new PublishTransaction();
        tx.version = 1;
        tx.attributes = new TransactionAttribute[1];
        tx.attributes[0] = new TransactionAttribute();
        tx.attributes[0].usage = TransactionAttributeUsage.DescriptionUrl;
        tx.attributes[0].data =UUID.randomUUID().toString().getBytes();
        tx.inputs = new TransactionInput[0];
        tx.outputs = new TransactionOutput[0];
        tx.script = Helper.hexToBytes(codeStr);
        tx.parameterList =  new ContractParameterType[]{ContractParameterType.ByteArray, ContractParameterType.Array};
        tx.returnType = returnType;
        tx.codeVersion = codeVersion;
        tx.needStorage = needStorage;
        tx.name = name;
        tx.author = author;
        tx.email = email;
        tx.description = desp;
        return tx;
    }

    public static InvocationTransaction makeInvocationTransaction(byte[] paramsHexStr, ECPoint publicKey, Fixed8 fee) throws Exception {
        InvocationTransaction tx = new InvocationTransaction(publicKey);
        tx.version = 1;
        tx.attributes = new TransactionAttribute[2];
        tx.attributes[0] = new TransactionAttribute();
        tx.attributes[0].usage = TransactionAttributeUsage.Script;
        tx.attributes[0].data = Program.toScriptHash(Contract.createSignatureRedeemScript(publicKey)).toArray();
        tx.attributes[1] = new TransactionAttribute();
        tx.attributes[1].usage = TransactionAttributeUsage.DescriptionUrl;
        tx.attributes[1].data = UUID.randomUUID().toString().getBytes();
        tx.inputs = new TransactionInput[0];
        tx.outputs = new TransactionOutput[0];
        tx.script = paramsHexStr;

        Fixed8 gasFee = null;
        Fixed8 netFee = null;
        if (tx.systemFee().compareTo(Fixed8.ZERO) <= 0) {
            gasFee = Fixed8.ZERO;
            netFee = Fixed8.fromDecimal(new BigDecimal("0.0001"));
        }
        else {
            gasFee = tx.systemFee();
            netFee = Fixed8.ZERO;
        }

        tx.gas = gasFee;
        fee.assign(netFee);
        return tx;
    }

    public static ContractTransaction makeContractTransaction(String toAddress, UInt256 assetId, Fixed8 amount, Fixed8 fee) {
        ContractTransaction tx = new ContractTransaction();

        TransactionOutput output = new TransactionOutput();
        output.assetId = assetId;
        output.value = amount;
        output.scriptHash = Wallet.toScriptHash(toAddress);

        tx.version = 0;
        tx.attributes = new TransactionAttribute[0];
        tx.inputs = new TransactionInput[0];
        tx.outputs = new TransactionOutput[1];
        tx.outputs[0] = output;
        tx.scripts = new Program[0];

        Fixed8 gasFee = null;
        Fixed8 netFee = null;
        if (tx.systemFee().compareTo(Fixed8.ZERO) <= 0) {
            gasFee = Fixed8.ZERO;
            netFee = Fixed8.fromDecimal(new BigDecimal("0.0001"));
        }
        else {
            gasFee = tx.systemFee();
            netFee = Fixed8.ZERO;
        }

        fee.assign(netFee);
        return tx;
    }
}
