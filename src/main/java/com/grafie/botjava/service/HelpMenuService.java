package com.grafie.botjava.service;

import com.grafie.botjava.entity.dto.common.BotResponse;
import com.grafie.botjava.entity.dto.common.KeyboardDto;
import com.grafie.botjava.entity.dto.common.MarkdownDto;
import com.grafie.botjava.jx3.http.util.REGEX;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 从统一指令元数据生成文本帮助和 QQ Markdown 按钮菜单。
 */
@Service
public class HelpMenuService {

    public static final String CALLBACK_PREFIX = "jx3:help:";

    private final GroupCommandPolicy commandPolicy;

    public HelpMenuService(GroupCommandPolicy commandPolicy) {
        this.commandPolicy = commandPolicy;
    }

    public BotResponse textHelp(String groupOpenId) {
        return BotResponse.text(REGEX.buildHelpText(commandPolicy.availableDefinitions(groupOpenId)));
    }

    public BotResponse categoryHelp(String groupOpenId, REGEX.CommandGroup group) {
        List<REGEX> definitions = commandPolicy.availableDefinitions(groupOpenId).stream()
                .filter(definition -> definition.getCommandGroup() == group)
                .toList();
        if (definitions.isEmpty()) {
            return BotResponse.text("当前没有可用的“" + group.getDisplayName() + "”指令。");
        }
        return BotResponse.text(REGEX.buildHelpText(definitions));
    }

    public BotResponse interactiveMenu() {
        MarkdownDto markdown = MarkdownDto.content("# 剑网3查询菜单\n请选择要查看的指令分类。");
        List<REGEX.CommandGroup> groups = Arrays.asList(REGEX.CommandGroup.values());
        KeyboardDto keyboard = new KeyboardDto();
        keyboard.setContent(new KeyboardDto.Content(List.of(
                new KeyboardDto.Row(List.of(button(groups.get(0)), button(groups.get(1)))),
                new KeyboardDto.Row(List.of(button(groups.get(2)), button(groups.get(3))))
        )));
        return BotResponse.markdown(markdown, keyboard);
    }

    private KeyboardDto.Button button(REGEX.CommandGroup group) {
        KeyboardDto.RenderData renderData = new KeyboardDto.RenderData();
        renderData.setLabel(group.getDisplayName());
        renderData.setVisitedLabel(group.getDisplayName());
        renderData.setStyle(1);

        KeyboardDto.Permission permission = new KeyboardDto.Permission();
        permission.setType(2);

        KeyboardDto.Action action = new KeyboardDto.Action();
        action.setType(1);
        action.setPermission(permission);
        action.setData(CALLBACK_PREFIX + group.name());

        KeyboardDto.Button button = new KeyboardDto.Button();
        button.setId("help-" + group.name().toLowerCase());
        button.setRenderData(renderData);
        button.setAction(action);
        return button;
    }
}
