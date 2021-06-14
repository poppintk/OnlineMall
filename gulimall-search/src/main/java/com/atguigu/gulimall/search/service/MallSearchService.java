package com.atguigu.gulimall.search.service;

import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;

public interface MallSearchService {

    /**
     * 检索所有参数
     * 返回所有结果
     * @param param
     * @return
     */
    SearchResult search(SearchParam param);
}
