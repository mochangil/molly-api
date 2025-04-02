package org.example.mollyapi.product.mapper;

import java.util.Set;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.mollyapi.product.dto.request.ProductBulkItemReqDto;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ProductItemMapper {

    void insertProductItems(@Param("productItems") List<ProductBulkItemReqDto> productItems,
        @Param("userId") long userId,
        @Param("now") LocalDateTime now);

    Set<Long> findProductIdsByIds(@Param("startId") Long startId, @Param("endId") Long endId);

}