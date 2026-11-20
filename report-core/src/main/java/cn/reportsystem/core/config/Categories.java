package cn.reportsystem.core.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class Categories {

    public Gui gui = new Gui();
    public Map<String, Category> categories = new LinkedHashMap<String, Category>();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static final class Gui {
        public int rows = 6;
        public String title = "&8举报";
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static final class Category {
        public String display = "";
        public String material = "PAPER";
        public int slot = 0;
        public String permission = "";
        public List<String> aliases = new ArrayList<String>();
        public List<String> description = new ArrayList<String>();
    }

    public Category find(String keyOrAlias) {
        if (keyOrAlias == null) {
            return null;
        }
        Category direct = categories.get(keyOrAlias.toLowerCase());
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, Category> entry : categories.entrySet()) {
            for (String alias : entry.getValue().aliases) {
                if (alias.equalsIgnoreCase(keyOrAlias)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    public String keyOf(Category category) {
        for (Map.Entry<String, Category> entry : categories.entrySet()) {
            if (entry.getValue() == category) {
                return entry.getKey();
            }
        }
        return null;
    }

    public String keyBySlot(int slot) {
        for (Map.Entry<String, Category> entry : categories.entrySet()) {
            if (entry.getValue().slot == slot) {
                return entry.getKey();
            }
        }
        return null;
    }

    public List<String> validate() {
        List<String> problems = new ArrayList<String>();
        if (categories.isEmpty()) {
            problems.add("categories.yml 里没有配置任何举报类型");
        }
        if (gui.rows < 1 || gui.rows > 6) {
            problems.add("gui.rows 只能在 1 到 6 之间");
        }
        int capacity = gui.rows * 9;
        for (Map.Entry<String, Category> entry : categories.entrySet()) {
            Category category = entry.getValue();
            if (category.slot < 0 || category.slot >= capacity) {
                problems.add("举报类型 " + entry.getKey() + " 的 slot " + category.slot
                        + " 超出界面范围 0~" + (capacity - 1));
            }
        }
        return problems;
    }
}
