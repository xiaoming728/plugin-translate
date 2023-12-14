import {definePlugin} from "@halo-dev/console-shared";
import HomeView from "./views/HomeView.vue";
import {IconArrowUpDownLine, VDropdownItem} from "@halo-dev/components";
import {markRaw} from "vue";
import type {ListedPost} from "@halo-dev/api-client";
import axios from "axios";

// @ts-ignore
export default definePlugin({
    components: {},
    extensionPoints: {
        "post:list-item:operation:create": () => {
            return [
                {
                    priority: 21,
                    component: markRaw(VDropdownItem),
                    label: "一键翻译",
                    permissions: [],
                    action: async (post: ListedPost) => {
                        window.location.href = '/apis/api.plugin.halo.run/v1alpha1/plugins/PluginTranslate/translate/post/' + post.post.metadata.name
                        return true;
                    },
                },
            ];
        }
    }
});

