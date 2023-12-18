package run.halo.translate;

import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.BasePlugin;

/**
 * 翻译插件
 *
 * @author liuchunming
 * @date 2023-12-14
 */
@Component
public class TranslatePlugin extends BasePlugin {

    public TranslatePlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * This method is called by the application when the plugin is started.
     * See {@link PluginManager#startPlugin(String)}.
     */
    @Override
    public void start() {
        // 插件启动时注册自定义模型

    }

    @Override
    public void stop() {
        // 插件停用时取消注册自定义模型

    }

    /**
     * This method is called by the application when the plugin is deleted.
     * See {@link PluginManager#deletePlugin(String)}.
     */
    @Override
    public void delete() {
        super.delete();
    }
}