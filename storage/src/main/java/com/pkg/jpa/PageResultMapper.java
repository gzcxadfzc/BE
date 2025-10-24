package com.pkg.jpa;

import com.pkg.domain.common.PageInfo;
import com.pkg.domain.common.PageResult;
import org.springframework.data.domain.Page;

public class PageResultMapper {

    public static PageResult<?> toPageResult(Page<?> page) {
        PageInfo pageInfo = new PageInfo(
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isLast()
                );
        return new PageResult<>(page.getContent(), pageInfo);
    }
}
