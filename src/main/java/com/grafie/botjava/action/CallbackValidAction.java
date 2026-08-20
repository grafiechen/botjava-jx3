package com.grafie.botjava.action;

import com.grafie.botjava.entity.dto.payload.Payload;
import com.grafie.botjava.entity.dto.SignResult;
import com.grafie.botjava.entity.dto.check.ServerCheckDto;
import com.grafie.botjava.qq.QqCallbackSignatureUtil;
import com.grafie.botjava.util.BotPropertyUtil;
import com.grafie.botjava.util.ObjectMapperUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 回调验证处理器
 *
 * @author grafie.chen
 * @since 2025/1/22  14:59
 */
@Slf4j
public class CallbackValidAction   {
    public static Object doSign(Payload payload){
        try {
            String jsonString = ObjectMapperUtil.writeValueAsString(payload.getD());
            ServerCheckDto serverCheckDto = ObjectMapperUtil.readValue(jsonString, ServerCheckDto.class);
            String signature = QqCallbackSignatureUtil.signValidation(
                    BotPropertyUtil.getBotSecret(),
                    serverCheckDto.getEventTs(),
                    serverCheckDto.getPlainToken());
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
