/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pe.pi.openpipe;

import com.phono.srtplight.Log;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import pe.pi.client.base.certHolders.CertHolder;

/**
 *
 * @author tim
 */
public class OneOffCert extends CertHolder {

    private static final long YESTERDAY = - 24 * 60 * 60 * 1000;
    private static final long INTENYEARS = + 3650 * 24 * 60 * 60 * 1000;
    long startOff = YESTERDAY;
    long endOff = INTENYEARS;

    OneOffCert() throws UnrecoverableEntryException, KeyStoreException, IOException, FileNotFoundException, NoSuchAlgorithmException, CertificateException {
        super(".");
    }

    @Override
    protected void loadKeyNCert() throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableEntryException, CertificateEncodingException {
        SecureRandom random = new SecureRandom();

        Date startDate = new Date(System.currentTimeMillis() + startOff);
        Date endDate = new Date(System.currentTimeMillis() + endOff);
        BigInteger serialNumber
                = BigInteger.valueOf(Math.abs(random.nextLong()));
        int strength = 2048;
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(strength);

        KeyPair keyPair = generator.generateKeyPair();
        PrivateKey priv = keyPair.getPrivate();
        _key = PrivateKeyFactory.createKey(priv.getEncoded());
        X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
        X500Principal dnName = new X500Principal("CN=| openpipe |");
        certGen.setSerialNumber(serialNumber);
        certGen.setIssuerDN(dnName);
        certGen.setNotBefore(startDate);
        certGen.setNotAfter(endDate);
        certGen.setSubjectDN(dnName);                       // note: same as issuer
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA256withRSA");
        X509Certificate cert;
        try {
            cert = certGen.generate(keyPair.getPrivate(), "BC");
            java.security.cert.Certificate[] chain = new java.security.cert.Certificate[1];
            ByteArrayInputStream bis = new ByteArrayInputStream(cert.getEncoded());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            while (bis.available() > 0) {
                chain[0] = cf.generateCertificate(bis);
            }
            BcTlsCrypto cr = super.getCrypto();
            _cert = cr.createCertificate(chain[0].getEncoded());
        } catch (Exception ex) {
            Log.error("Can't make cert because "+ex.getMessage());
        }
    }

}
