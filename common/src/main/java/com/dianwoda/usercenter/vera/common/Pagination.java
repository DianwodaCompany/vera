package com.dianwoda.usercenter.vera.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hollisgw on 17/7/3.
 * 分页工具类
 */
public class Pagination<T> implements Serializable {

    private static final long serialVersionUID = -3509205611665142708L;

    // 当前页
    private int currentPage;
    // 当前页页码下标索引
    private int currentPageIndex;
    // 每页查询数量
    private int pageSize;
    // 总记录数
    private int totalCount;
    // 总页数
    private int pageCount;
    // MySQL查询数据开始下标
    private int offset;
    // 查询数据结果列表
    private List<T> list;
    /**
     * 默认每页记录数
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 默认当前页
     */
    public static final int DEFAULT_CURRENT_PAGE = 1;

    /**
     * 构造函数
     */
    public Pagination() {
        init(null, null);
    }

    /**
     * 构造方法
     *
     * @param currentPage 页码索引下标
     */
    public Pagination(Integer currentPage) {
        init(currentPage, null);
    }

    /**
     * 构造方法
     *
     * @param currentPage 页码索引下标
     * @param pageSize    每页查询数量
     */
    public Pagination(Integer currentPage, Integer pageSize) {
        init(currentPage, pageSize);
    }

    /**
     * 初始化分页器
     *
     * @param currentPage
     * @param pageSize
     */
    private void init(Integer currentPage, Integer pageSize) {
        this.currentPage = (currentPage == null || currentPage < 1) ? DEFAULT_CURRENT_PAGE : currentPage;
        this.pageSize = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : pageSize;
        this.currentPageIndex = getCurrentPage() - 1;
        this.offset = getCurrentPageIndex() * getPageSize();
        this.list = new ArrayList<T>();
    }

    /**
     * 设置总记录数，并计算总页数
     *
     * @param totalCount
     */
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
        this.pageCount = totalCount % pageSize == 0 ? totalCount / pageSize : totalCount / pageSize + 1;
        if (pageCount != 0 && currentPage > pageCount) {
            this.currentPage = pageCount;
        }
        this.currentPageIndex = getCurrentPage() - 1;
        this.offset = getCurrentPageIndex() * getPageSize();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage < 1 ? DEFAULT_CURRENT_PAGE : currentPage;
    }

    public int getCurrentPageIndex() {
        return currentPageIndex;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize < 1 ? DEFAULT_PAGE_SIZE : pageSize;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getPageCount() {
        return pageCount;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public int getOffset() {
        return offset;
    }
}
