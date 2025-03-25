package org.example.mollyapi.product.repository;

import org.example.mollyapi.product.enums.OrderBy;

import java.util.List;
import java.util.stream.Collectors;

public class ProductReadQuery {

    private static final String BASE_QUERY =
            "SELECT\n" +
                    "    p.product_id,\n" +
                    "    p.category_id,\n" +
                    "    p.brand_name,\n" +
                    "    p.product_name,\n" +
                    "    p.price,\n" +
                    "    p.created_at,\n" +
                    "    p.view_count,\n" +
                    "    p.purchase_count,\n" +
                    "    p.thumbnail_url,\n" +
                    "    p.thumbnail_filename,\n" +
                    "    p.user_id\n" +
                    "FROM\n" +
                    "    product as p\n";

    private StringBuilder query;
    private boolean hasWhere;

    public ProductReadQuery() {
        this.query = new StringBuilder(BASE_QUERY);
        this.hasWhere = false;
    }

    public ProductReadQuery appendJoin(String subQuery) {
        this.query.append("JOIN (\n" +
                subQuery + ") pi ON pi.product_id = p.product_id\n");
        return this;
    }

    public ProductReadQuery appendCategory(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return this; // 빈 리스트나 null이면 조건 추가 안 함
        }

        String categoryString = categoryIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        if (!hasWhere) {
            query.append("WHERE\n")
                    .append("    p.category_id IN (")
                    .append(categoryString)
                    .append(")\n");
            hasWhere = true;
        } else {
            query.append("AND p.category_id IN (")
                    .append(categoryString)
                    .append(")\n");
        }
        return this;
    }

    public ProductReadQuery appendPriceGoe(Long minPrice) {
        if (minPrice == null) {
            return this;
        }

        if (!hasWhere) {
            query.append("WHERE\n").append("    p.price > ").append(minPrice).append("\n");
            hasWhere = true;
        } else {
            query.append("AND p.price > ").append(minPrice).append("\n");
        }
        return this;
    }

    public ProductReadQuery appendPriceLt(Long maxPrice) {
        if (maxPrice == null) {
            return this;
        }

        if (!hasWhere) {
            query.append("WHERE\n").append("    p.price < ").append(maxPrice).append("\n");
            hasWhere = true;
        } else {
            query.append("AND p.price < ").append(maxPrice).append("\n");
        }
        return this;
    }

    public ProductReadQuery appendBrandName(String brandName) {
        if (brandName == null) {
            return this;
        }

        if (!hasWhere) {
            query.append("WHERE\n").append("    p.brand_name = '").append(brandName).append("'\n");
            hasWhere = true;
        } else {
            query.append("AND p.brand_name = '").append(brandName).append("'\n");
        }
        return this;
    }

    public ProductReadQuery appendOrder(OrderBy orderBy) {
        query.append("ORDER BY "+ orderBy.getColumnName() + " " + orderBy.getDirection() + "\n");
        return this;
    }

    public String build() {
        return query.toString();
    }

}
