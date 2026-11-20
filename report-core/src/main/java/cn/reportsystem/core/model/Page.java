package cn.reportsystem.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Page<T> {

    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final long total;

    public Page(List<T> items, int page, int pageSize, long total) {
        this.items = Collections.unmodifiableList(new ArrayList<T>(items));
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }

    public static <T> Page<T> empty(int page, int pageSize) {
        return new Page<T>(Collections.<T>emptyList(), page, pageSize, 0L);
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotal() {
        return total;
    }

    public int getTotalPages() {
        if (pageSize <= 0) {
            return 0;
        }
        return (int) ((total + pageSize - 1) / pageSize);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
