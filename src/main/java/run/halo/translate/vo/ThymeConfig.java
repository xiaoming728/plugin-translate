package run.halo.translate.vo;

import cn.hutool.json.JSONObject;
import lombok.Data;
import java.util.List;

/**
 * @author wuzhangpeng
 * @since 2023/12/19
 */
@Data
public class ThymeConfig {

    /**
     * API 版本
     */
    private String apiVersion;
    /**
     * 数据
     */
    private JSONObject data;
    /**
     * 类
     */
    private String kind;
    /**
     * 元数据
     */
    private JSONObject metadata;
    /**
     * 语言
     */
    private List<String> langs;
}
