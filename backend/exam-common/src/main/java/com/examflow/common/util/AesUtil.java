package com.examflow.common.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AES-256-GCM 字段加密(见 TDD §7.2)。
 * 密钥来源:环境变量 EXAMSECRET_KEY(Base64,32 字节)。
 *
 * <p>安全基线:生产环境未注入 EXAMSECRET_KEY 时,回退内置开发密钥并持续 WARN;
 * 该密钥为公开固定值,严禁用于生产 —— 部署平台必须通过环境变量/KMS 注入真实密钥。
 */
public final class AesUtil {

    private static final Logger log = LoggerFactory.getLogger(AesUtil.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_BITS = 128;
    private static final int NONCE_LEN = 12;

    /** 开发默认密钥(仅本地开发使用,生产必须注入 EXAMSECRET_KEY)。
     *  解码后必须恰为 32 字节(256bit)。 */
    private static final String DEV_KEY_B64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private static volatile boolean devKeyWarned = false;

    private AesUtil() {
    }

    private static SecretKeySpec loadKey() {
        String b64 = System.getenv("EXAMSECRET_KEY");
        if (b64 == null || b64.isBlank()) {
            if (!devKeyWarned) {
                log.warn("未配置环境变量 EXAMSECRET_KEY,回退内置开发密钥 —— 严禁用于生产环境");
                devKeyWarned = true;
            }
            b64 = DEV_KEY_B64;
        }
        byte[] raw = Base64.getDecoder().decode(b64);
        if (raw.length != 32) {
            throw new IllegalStateException("EXAMSECRET_KEY 必须为 32 字节(256bit)");
        }
        return new SecretKeySpec(raw, "AES");
    }

    /** 加密:输出 Base64(nonce + ciphertext)。 */
    public static String encrypt(String plaintext) throws Exception {
        if (plaintext == null) {
            return null;
        }
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        byte[] nonce = new byte[NONCE_LEN];
        new SecureRandom().nextBytes(nonce);
        cipher.init(Cipher.ENCRYPT_MODE, loadKey(), new GCMParameterSpec(TAG_BITS, nonce));
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] out = new byte[nonce.length + encrypted.length];
        System.arraycopy(nonce, 0, out, 0, nonce.length);
        System.arraycopy(encrypted, 0, out, nonce.length, encrypted.length);
        return Base64.getEncoder().encodeToString(out);
    }

    /** 解密:入参为 {@link #encrypt(String)} 的输出。 */
    public static String decrypt(String data) throws Exception {
        if (data == null) {
            return null;
        }
        byte[] in = Base64.getDecoder().decode(data);
        if (in.length <= NONCE_LEN) {
            throw new IllegalArgumentException("密文长度非法");
        }
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, loadKey(), new GCMParameterSpec(TAG_BITS, in, 0, NONCE_LEN));
        byte[] plain = cipher.doFinal(in, NONCE_LEN, in.length - NONCE_LEN);
        return new String(plain, StandardCharsets.UTF_8);
    }
}
