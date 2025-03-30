package org.example.mollyapi.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.mollyapi.common.entity.Base;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends Base {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    Long id;

    String categoryName;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Category> children = new ArrayList<>();  // 자식 카테고리 리스트

    Long level;

    @OneToMany(mappedBy = "category")
    List<Product> products = new ArrayList<>();

    @Builder
    public Category(
            String categoryName,
            Category parent
    ) {
        this.categoryName = categoryName;
        this.parent = parent;
    }
}
