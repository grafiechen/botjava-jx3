package com.grafie.botjava.action;

import com.grafie.botjava.entity.dto.payload.Payload;
import com.grafie.botjava.entity.dto.SignResult;
import com.grafie.botjava.entity.dto.check.ServerCheckDto;
import com.grafie.botjava.util.BotPropertyUtil;
import com.grafie.botjava.util.ObjectMapperUtil;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * 回调验证处理器
 *
 * @author grafie.chen
 * @since 2025/1/22  14:59
 */
@Slf4j
public class CallbackValidAction   {
    private static final int ED25519_SEED_SIZE = 32;

    public static Object doSign(Payload payload){
        try {
            // 补全种子长度至 Ed25519 标准大小
            String seed = BotPropertyUtil.getBotSecret();
            while (seed.length() < ED25519_SEED_SIZE) {
                seed = seed + seed;
            }
            seed = seed.substring(0, ED25519_SEED_SIZE);

            // 生成私钥
            byte[] seedBytes = seed.getBytes(StandardCharsets.UTF_8);
            Ed25519PrivateKeyParameters privateKeyParams = new Ed25519PrivateKeyParameters(seedBytes, 0);

            // 创建签名
            Ed25519Signer signer = new Ed25519Signer();
            signer.init(true, privateKeyParams);
            String jsonString = ObjectMapperUtil.writeValueAsString(payload.getD());
            ServerCheckDto serverCheckDto = ObjectMapperUtil.readValue(jsonString, ServerCheckDto.class);
            String message = serverCheckDto.getEventTs() + serverCheckDto.getPlainToken();
            signer.update(message.getBytes(StandardCharsets.UTF_8), 0, message.length());

            byte[] signatureBytes = signer.generateSignature();
            String signature = HexFormat.of().formatHex(signatureBytes);
            SignResult signResult = new SignResult();
            signResult.setSignature(signature);
            signResult.setPlainToken(serverCheckDto.getPlainToken());
            return signResult;
        }catch (Exception e){
            log.error("回调验证器处理失败，op=>{}，reason=>{}",
                    payload.getOp(), com.grafie.botjava.util.SensitiveDataUtil.summarize(e));
            return null;
        }

    }
}
