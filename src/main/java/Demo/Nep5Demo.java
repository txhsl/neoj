package Demo;

import NEO.Core.Blockchain;
import NEO.Core.SignatureContext;
import NEO.Core.Transaction;
import NEO.Fixed8;
import NEO.Helper;
import NEO.IO.Json.JArray;
import NEO.IO.Json.JNumber;
import NEO.IO.Json.JObject;
import NEO.IO.Json.JString;
import NEO.Implementations.Blockchain.RPC.RpcBlockchain;
import NEO.Implementations.Wallets.SQLite.UserWallet;
import NEO.Settings;
import NEO.UInt160;
import NEO.Wallets.Account;
import NEO.Wallets.Contract;
import NEO.Wallets.Wallet;
import NEO.sdk.SmartContractTx;
import NEO.sdk.abi.AbiFunction;
import NEO.sdk.abi.AbiInfo;
import NEO.sdk.wallet.AccountManager;
import com.alibaba.fastjson.JSON;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Nep5Demo {
	public static String privatekey1 = "1094e90dd7c4fdfd849c14798d725ac351ae0d924b29a279a9ffa77d5737bd96";
	public static String privatekey2 = "bc254cf8d3910bc615ba6bf09d4553846533ce4403bc24f58660ae150a6d64cf";
	public static Contract contract1 = null;
	public static Contract contract2 = null;
	public static void main(String[] args) throws Exception {
        System.out.println("Hi NEO, Nep-5 smartcontract invoke test!");

        Blockchain bc = new RpcBlockchain(null);
        Blockchain.register(bc);

		Settings.getSettings(Settings.CONFIG_PRIVNET);
		UserWallet uw = UserWallet.create("Nep5Demo", "Nep5Demo");

		Account account1 = uw.createAccount(Helper.hexToBytes(privatekey1));
		contract1 = Contract.createSignatureContract(account1.publicKey);
		System.out.println("contract1 address:" + contract1.address());

		Account account2 = uw.createAccount(Helper.hexToBytes(privatekey2));
		contract2 = Contract.createSignatureContract(account2.publicKey);
		System.out.println("contract2 address:" + contract2.address());

		//read smart contract abi file
		InputStream is2 = new FileInputStream("nep-5.abi.json");
		byte[] bys2 = new byte[is2.available()];
		is2.read(bys2);
		is2.close();
		String abi = new String(bys2);

		AbiInfo abiinfo = JSON.parseObject(abi, AbiInfo.class);
		System.out.println("Entrypoint:" + abiinfo.getEntrypoint());
		System.out.println("contractAddress:"+abiinfo.getHash());
		System.out.println("Functions:" + abiinfo.getFunctions());

		AbiFunction func = abiinfo.getFunction("Transfer");//BalanceOf
		func.name = func.name.toLowerCase();
		func.setParamsValue(Wallet.toScriptHash(contract1.address()).toArray(),Wallet.toScriptHash(contract2.address()).toArray(),Long.valueOf(1));

		//make transaction
		Fixed8 fee = new Fixed8();
		Transaction tx = SmartContractTx.makeInvocationTransaction(Helper.reverse("5bb169f915c916a5e30a3c13a5e0cd228ea26826"), account1.publicKey, func, fee);
		System.out.println(tx.hash().toString());
		tx = uw.makeTransaction(tx, fee);

		//sign tx
		SignatureContext context= new SignatureContext(tx,new UInt160[]{Wallet.toScriptHash(contract1.address())});
		sign(context, contract1, privatekey1);
		if (context.isCompleted()) {
			tx.scripts = context.getScripts();
			System.out.println("scripts:"+tx.scripts[0].json());
		}
		String txHex = Helper.toHexString(tx.toArray());

		//send tx to neo node
		sendRawTransaction("http://127.0.0.1:30333",txHex);
	}
	public static boolean sign(SignatureContext context, Contract contract, String privateKey) {
		boolean fSuccess = false;

		if (contract == null) {
			return fSuccess;
		}
		Account account = new Account(Helper.hexToBytes(privateKey));
		if (account == null) {
			return fSuccess;
		}
		byte[] signature = context.signable.sign(account);
		fSuccess |= context.add(contract, account.publicKey, signature);

		return fSuccess;
	}

	public static String sendRawTransaction(String url,String sData) throws Exception {
		JObject result = call(url,"sendrawtransaction", new JObject[]{new JString(sData)});
		return result.asString();
	}
	public static JObject call(String url,String method, JObject[] params) throws Exception {
		JObject response = send(url,makeRequest(method, params));
		System.out.println(response);
		if (response.containsProperty("result")) {
			return response.get("result");
		}
		else if (response.containsProperty("Result")) {
			return response.get("Result");
		}
		else if (response.containsProperty("error")) {
			throw new Exception(response.asString());
		}
		else {
			throw new IOException();
		}
	}
	public static JObject makeRequest(String method, JObject[] params) {
		JObject request = new JObject();
		request.set("jsonrpc", new JString("2.0"));
		request.set("method", new JString(method));
		request.set("params", new JArray(params));
		request.set("id", new JNumber(1));

		System.out.println("request: " + request.toString());
		return request;
	}
	private static JObject send(String url,JObject request) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		try (OutputStreamWriter w = new OutputStreamWriter(connection.getOutputStream())) {
			w.write(request.toString());
		}
		try (InputStreamReader r = new InputStreamReader(connection.getInputStream())) {
			return JObject.parse(r);
		}
	}
}
