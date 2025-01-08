package top.bluecraft.bluepacket.api;

public interface IPaginated {
    /**
     * 分页物品
     * 将 `Inventory` 中的物品分页到多个 `Page` 中，每个页面最多包含 110 个物品。
     * 已启用懒加载
     */
    void paginateInventoryIfNecessary(int pageIndex);
}
