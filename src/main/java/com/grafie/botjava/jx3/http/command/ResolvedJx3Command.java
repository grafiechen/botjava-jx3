package com.grafie.botjava.jx3.http.command;

import com.grafie.botjava.jx3.http.action.base.Jx3BaseAction;
import com.grafie.botjava.jx3.http.util.REGEX;

/**
 * 一次已经完成规范化、正则匹配和处理器定位的 JX3 指令。
 */
public record ResolvedJx3Command(
        String command,
        REGEX definition,
        CommandArguments arguments,
        Jx3BaseAction action
) {
}
