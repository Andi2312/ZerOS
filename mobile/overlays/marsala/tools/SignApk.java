import com.android.apksig.ApkSigner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Tiny apksigner replacement: SignApk <key.pk8> <cert.pem> <in.apk> <out.apk> <minSdk> */
public final class SignApk {
    public static void main(String[] args) throws Exception {
        PrivateKey key = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(readAll(new File(args[0]))));
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        List<X509Certificate> chain = new ArrayList<>();
        try (InputStream in = new FileInputStream(args[1])) {
            for (Certificate c : cf.generateCertificates(in)) {
                chain.add((X509Certificate) c);
            }
        }
        ApkSigner.SignerConfig config =
                new ApkSigner.SignerConfig.Builder("CERT", key, chain).build();
        new ApkSigner.Builder(Collections.singletonList(config))
                .setInputApk(new File(args[2]))
                .setOutputApk(new File(args[3]))
                .setMinSdkVersion(Integer.parseInt(args[4]))
                .build()
                .sign();
        System.out.println("signed -> " + args[3]);
    }

    private static byte[] readAll(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }
}
