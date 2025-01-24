package com.grafie.botjava.jx3.http.data.other;

import com.grafie.botjava.jx3.http.data.role.attribute.RoleAttributeData;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author grafie.chen
 * @since 2025/1/24  16:12
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DpsComputeData extends RoleAttributeData {
    /**
     * 旗舰 无界
     */
    private String model;
    /**
     * 机器人名字
     */
    private String bot;
    /**
     * 循环名字
     */
    private String loop;
}
