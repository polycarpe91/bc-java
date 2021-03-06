package org.bouncycastle.crypto.signers;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.Ed448PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed448PublicKeyParameters;
import org.bouncycastle.math.ec.rfc8032.Ed448;

public class Ed448Signer
    implements Signer
{
    private final Buffer buffer = new Buffer();
    private final byte[] context;

    private boolean forSigning;
    private Ed448PrivateKeyParameters privateKey;
    private Ed448PublicKeyParameters publicKey;

    public Ed448Signer(byte[] context)
    {
        this.context = context.clone();
    }

    public void init(boolean forSigning, CipherParameters parameters)
    {
        this.forSigning = forSigning;

        if (forSigning)
        {
            // TODO Allow AsymmetricCipherKeyPair to be a CipherParameters?

            this.privateKey = (Ed448PrivateKeyParameters)parameters;
            this.publicKey = privateKey.generatePublicKey();
        }
        else
        {
            this.privateKey = null;
            this.publicKey = (Ed448PublicKeyParameters)parameters;
        }

        reset();
    }

    public void update(byte b)
    {
        buffer.write(b);
    }

    public void update(byte[] buf, int off, int len)
    {
        buffer.write(buf, off, len);
    }

    public byte[] generateSignature()
    {
        if (!forSigning)
        {
            throw new IllegalStateException("Ed448Signer not initialised for signature generation.");
        }

        return buffer.generateSignature(privateKey, publicKey, context);
    }

    public boolean verifySignature(byte[] signature)
    {
        if (forSigning)
        {
            throw new IllegalStateException("Ed448Signer not initialised for verification");
        }

        return buffer.verifySignature(publicKey, context, signature);
    }

    public void reset()
    {
        buffer.reset();
    }

    private static class Buffer extends ByteArrayOutputStream
    {
        synchronized byte[] generateSignature(Ed448PrivateKeyParameters privateKey, Ed448PublicKeyParameters publicKey, byte[] ctx)
        {
            byte[] signature = new byte[Ed448PrivateKeyParameters.SIGNATURE_SIZE];
            privateKey.sign(Ed448.Algorithm.Ed448, publicKey, ctx, buf, 0, count, signature, 0);
            reset();
            return signature;
        }

        synchronized boolean verifySignature(Ed448PublicKeyParameters publicKey, byte[] ctx, byte[] signature)
        {
            byte[] pk = publicKey.getEncoded();
            boolean result = Ed448.verify(signature, 0, pk, 0, ctx, buf, 0, count);
            reset();
            return result;
        }

        public synchronized void reset()
        {
            Arrays.fill(buf, 0, count, (byte)0);
            this.count = 0;
        }
    }
}
