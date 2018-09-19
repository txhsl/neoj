package NEO.Cryptography;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.util.IllformedLocaleException;

import NEO.IO.BinaryReader;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.asn1.x9.*;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;

import NEO.Helper;
import org.bouncycastle.math.ec.custom.sec.SecP256K1FieldElement;
import org.bouncycastle.math.ec.custom.sec.SecP256R1Curve;
import org.bouncycastle.math.ec.custom.sec.SecP256R1FieldElement;
import org.bouncycastle.math.ec.custom.sec.SecP256R1Point;

public class ECC {
	private static final X9ECParameters secp256r1nc = ECNamedCurveTable.getByName("secp256r1");
	public static final ECDomainParameters secp256r1 = new ECDomainParameters(secp256r1nc.getCurve(), secp256r1nc.getG(), secp256r1nc.getN(), secp256r1nc.getH(), secp256r1nc.getSeed());

	public static final ECCurve secp256R1 = new SecP256R1Curve();

	public static int compare(ECPoint a, ECPoint b) {
		if (a == b) {
			return 0;
		}
		int result = a.getXCoord().toBigInteger().compareTo(b.getXCoord().toBigInteger());
		if (result != 0) {
			return result;
		}
		return a.getYCoord().toBigInteger().compareTo(b.getYCoord().toBigInteger());
	}
	
	public static byte[] generateKey(int len) {
		byte[] key = new byte[len];
		SecureRandom sr = new SecureRandom();
		sr.nextBytes(key);
		return key;
	}
	
	public static byte[] generateKey() {
		byte[] key = new byte[32];
		SecureRandom sr = new SecureRandom();
		sr.nextBytes(key);
		return key;
	}

	public static byte[] encodePoint(ECPoint point, boolean compressed) {
		if (point.isInfinity())
			return new byte[1];
		byte[] data;
		if (compressed) {
			data = new byte[33];
		}
		else {
			data = new byte[65];
			byte[] yBytes = point.getYCoord().toBigInteger().toByteArray();
			ArrayUtils.reverse(yBytes);
			System.arraycopy(yBytes, 0, data, 65 - yBytes.length, yBytes.length);
		}
		byte[] xBytes = point.getXCoord().toBigInteger().toByteArray();
		ArrayUtils.reverse(xBytes);
		System.arraycopy(xBytes, 0, data, 33 - xBytes.length, xBytes.length);
		data[0] = compressed ? point.getYCoord().toBigInteger().intValue() % 2 == 0 ? (byte)0x02 : (byte)0x03 : (byte)0x04;
		return data;
	}

	private static ECPoint decompressPoint(int yTilde, BigInteger X1, SecP256R1Curve curve) throws IllformedLocaleException {
		SecP256R1FieldElement x = new SecP256R1FieldElement(X1);
		ECFieldElement alpha = x.multiply(x.square().add(curve.getA())).add(curve.getB());
		ECFieldElement beta = alpha.sqrt();

		if (beta == null)
			throw new IllegalArgumentException();

		BigInteger betaValue = beta.toBigInteger();
		int bit0 = betaValue.mod(BigInteger.valueOf(2)).intValue() == 0 ? 0 : 1;

		if (bit0 != yTilde) {
			beta = new SecP256R1FieldElement(curve.getQ().subtract(betaValue));
		}

		return new SecP256R1Point(curve, x, beta);
	}

	public static ECPoint decodePoint(byte[] encoded, SecP256R1Curve curve) throws IOException {
		ECPoint p = null;
		int expectedLength = (curve.getQ().bitLength() + 7) / 8;
		switch (encoded[0]) {
			case 0x00:
				if (encoded.length != 1)
					throw new IOException();
				p = curve.getInfinity();
				break;
			case 0x02:
			case 0x03:
				if (encoded.length != (expectedLength + 1))
					throw new IOException();
				int yTilde = encoded[0] & 1;
				byte[] temp1 = new byte[encoded.length];
				System.arraycopy(encoded, 1, temp1, 1, encoded.length - 1);
				ArrayUtils.reverse(temp1);
				BigInteger X1 = new BigInteger(temp1);
				p = decompressPoint(yTilde, X1, curve);
				break;
			case 0x04:
			case 0x06:
			case 0x07:
				if (encoded.length != (2 * expectedLength + 1))
					throw new IOException();
				byte[] temp2 = new byte[expectedLength + 1];
				System.arraycopy(encoded, 1, temp2, 1, expectedLength);
				ArrayUtils.reverse(temp2);
				BigInteger X2 = new BigInteger(temp2);

				temp2 = new byte[encoded.length - expectedLength];
				System.arraycopy(encoded, expectedLength + 1, temp2, 1, encoded.length - expectedLength - 1);
				BigInteger Y2 = new BigInteger(temp2);

				p = new SecP256R1Point(curve, new SecP256K1FieldElement(X2), new SecP256K1FieldElement(Y2), true);
				break;
			default:
				throw new IOException();
		}
		return p;
	}

	public static ECPoint deserializeFrom(BinaryReader reader, SecP256R1Curve curve) throws IOException {
		int expectedLength = (curve.getQ().bitLength() + 7) / 8;
		byte[] buffer = new byte[1 + expectedLength * 2];
		buffer[0] = reader.readByte();

		switch (buffer[0]) {
			case 0x00:
				return curve.getInfinity();
			case 0x02:
			case 0x03:
				reader.read(buffer, 1, expectedLength);
				byte[] temp = new byte[1 + expectedLength];
				System.arraycopy(buffer, 0, temp, 0, 1 + expectedLength);
				return decodePoint(temp, curve);
			case 0x04:
			case 0x06:
			case 0x07:
				reader.read(buffer, 1, expectedLength * 2);
				return decodePoint(buffer, curve);
			default:
				throw new IOException();
		}
	}

	public static String toString(ECPoint p) {
		return Helper.toHexString(p.getEncoded(true));
	}

}
