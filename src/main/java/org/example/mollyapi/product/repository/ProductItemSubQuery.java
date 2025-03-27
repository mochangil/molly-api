package org.example.mollyapi.product.repository;

import org.example.mollyapi.product.enums.OrderBy;

import java.util.List;
import java.util.stream.Collectors;

public class ProductItemSubQuery {
    private final StringBuilder query;
    private final OrderBy orderBy;
    private final Long offset;
    private final Long pageSize;

    private StringBuilder getBaseWithQuery(OrderBy orderBy, Long offset) {
        StringBuilder query = new StringBuilder();

        if (offset != null && offset > 0) {
            query.append("WITH target AS (\n" +
                    "    SELECT "+ orderBy.getColumnName() + "\n" +
                    "    FROM product_item\n" +
                    "    WHERE product_id = " + offset.toString() +"\n" +
                    "    LIMIT 1\n" +
                    ")\n");
        }

        query.append("SELECT DISTINCT " + orderBy.getColumnName() + ", product_id\n" +
                "FROM product_item\n");

        return query;
    }

    public ProductItemSubQuery(OrderBy orderBy, Long offset, Long pageSize) {
        this.query = getBaseWithQuery(orderBy, offset);
        this.orderBy = orderBy;
        this.offset = offset;
        this.pageSize = pageSize;
    }

    public ProductItemSubQuery appendConditions() {
        String columnName = orderBy.getColumnName();
        String sign = orderBy.getDirection().equals("DESC") ? "<" : ">";


        if (offset != null && offset > 0) {
            query.append("WHERE\n")
                    .append("    (" + columnName + " " + sign + " (SELECT " + columnName + " FROM target)\n")
                    .append("    OR (" + columnName + " = (SELECT " + columnName + " FROM target) AND product_id " + sign + " ")
                    .append(offset).append("))\n");
        } else {
            query.append("WHERE\n")
                    .append("1=1\n");
        }

        return this;
    }

    public ProductItemSubQuery appendExcludeSoldOut(Boolean excludeSoldOut) {
        if (excludeSoldOut == null) {
            return this;
        }
        query.append("AND quantity > 0\n");
        return this;
    }

    public ProductItemSubQuery appendSize(List<String> sizes) {
        if (sizes == null || sizes.isEmpty()) {
            return this;
        }
        String sizeString = sizes.stream()
                .map(size -> "'" + size + "'")
                .collect(Collectors.joining(","));

        query.append("  AND size in (" + sizeString + ")\n");
        return this;
    }

    public ProductItemSubQuery appendColorCode(List<String> colorCodes) {
        if (colorCodes == null || colorCodes.isEmpty()) {
            return this; // 빈 리스트나 null이면 조건 추가 안 함
        }

        String colorCodeString = colorCodes.stream()
                .map(code -> "'" + code + "'")
                .collect(Collectors.joining(", "));
        query.append("  AND color_code IN (" + colorCodeString + ")\n");
        return this;
    }

    public ProductItemSubQuery appendCategoryId(List<Long> categoryId) {
        if (categoryId == null || categoryId.isEmpty()) {
            return this; // 빈 리스트나 null이면 조건 추가 안 함
        }

        String categoryIdString = categoryId.stream()
                .map(code -> "'" + code + "'")
                .collect(Collectors.joining(", "));
        query.append("  AND category_id IN (" + categoryIdString + ")\n");
        return this;
    }

    public ProductItemSubQuery appendBrandName(String brandName) {
        if (brandName == null || brandName.isEmpty()) {
            return this; // 빈 리스트나 null이면 조건 추가 안 함
        }

        query.append("  AND brand_name = '" + brandName + "'\n");
        return this;
    }

    public ProductItemSubQuery appendPriceGoe(Long minPrice) {
        if (minPrice == null || minPrice < 0) {
            return this;
        }

        query.append("   AND price >= ").append(minPrice).append("\n");
        return this;
    }

    public ProductItemSubQuery appendPriceLt(Long maxPrice) {
        if (maxPrice == null || maxPrice < 0) {
            return this;
        }

        query.append("   AND price < ").append(maxPrice).append("\n");
        return this;
    }

    public ProductItemSubQuery appendSellerId(Long sellerId) {
        if (sellerId == null || sellerId < 0) {
            return this;
        }

        query.append("   AND seller_id = ").append(sellerId).append("\n");
        return this;
    }

    public ProductItemSubQuery appendOrderAndLimit() {
        query.append("ORDER BY "+ orderBy.getColumnName() + " " + orderBy.getDirection() + "\n");

        if (pageSize != null && pageSize > 0) {
            query.append("LIMIT ").append(pageSize).append("\n");
        }
        return this;
    }

    public String build() {
        return query.toString();
    }
}
