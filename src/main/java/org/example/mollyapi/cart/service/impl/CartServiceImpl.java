package org.example.mollyapi.cart.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.cart.dto.Request.AddCartReqDto;
import org.example.mollyapi.cart.dto.Request.UpdateCartReqDto;
import org.example.mollyapi.cart.dto.Response.CartInfoDto;
import org.example.mollyapi.cart.dto.Response.CartInfoResDto;
import org.example.mollyapi.cart.entity.Cart;
import org.example.mollyapi.cart.repository.CartRepository;
import org.example.mollyapi.cart.service.CartService;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.product.dto.response.ColorDetailDto;
import org.example.mollyapi.product.entity.ProductItem;
import org.example.mollyapi.product.repository.ProductItemRepository;
import org.example.mollyapi.product.service.ProductService;
import org.example.mollyapi.product.service.impl.ProductItemServiceImpl;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.example.mollyapi.common.exception.error.impl.CartError.*;
import static org.example.mollyapi.common.exception.error.impl.ProductItemError.OVER_QUANTITY;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final ProductService productService;
    private final ProductItemRepository productItemRep;
    private final ProductItemServiceImpl productItemService;
    private final CartRepository cartRep;
    private final UserService userService;

    /**
     * 장바구니에 상품 담기 기능
     * @param addCartReqDto 추가하려는 데이터
     * @param userId 사용자 PK
     */
    @Override
    @Transactional
    public void addCart(AddCartReqDto addCartReqDto, Long userId) {
        // 1. 가입된 사용자 여부 체크
        User user = userService.findByUser(userId);

        // 2. 상품 존재 여부 체크
        ProductItem item = productItemService.findByProductItem(addCartReqDto.itemId());

        // 3. 상품의 재고가 남아 있는 지 체크
        validStock(item, addCartReqDto.quantity());

        // 4. 장바구니에 동일한 상품이 담겨 있는 지 체크
        Cart cart = cartRep.findByProductItemIdAndUserUserId(addCartReqDto.itemId(), userId);

        // 5. 존재하지 않으면 삽입
        if(cart == null) {
            insertNewCart(addCartReqDto, user, item);
        } else {
            // 6. 초과 수량을 장바구니에 담는 지 체크
            long totalQuantity = cart.getQuantity() + addCartReqDto.quantity(); //기존에 담아둔 수량 + 추가 하려는 수량
            validStock(item, totalQuantity);

            // 7. 수량 업데이트
            cart.updateQuantity(totalQuantity);
        }
    }

    /**
     * 새로운 상품 장바구니에 추가
     * @param addCartReqDto 추가하려는 데이터
     * @param user 사용자 정보
     * @param item 상품 정보
     */
    public void insertNewCart(AddCartReqDto addCartReqDto, User user, ProductItem item) {
        // 장바구니 최대 수량(30개) 미만 체크
        boolean isMaxCart = cartRep.countByUserUserId(user.getUserId());
        if(isMaxCart) throw new CustomException(MAX_CART);

        // 새로운 Cart 엔티티 생성
        Cart newCart = Cart.builder()
                .quantity(addCartReqDto.quantity())
                .user(user)
                .productItem(item)
                .build();

        cartRep.save(newCart); //장바구니에 데이터 추가
    }

    /**
     * 장바구니 조회 기능
     * @param userId 사용자 PK
     * */
    @Override
    @Transactional(readOnly = true)
    public List<CartInfoResDto> getCartDetail(Long userId) {
        // 가입된 사용자 여부 체크
        userService.validUser(userId);

        // 사용자 장바구니 조회
        List<CartInfoDto> cartInfoList = cartRep.getCartInfo(userId);
        if(cartInfoList.isEmpty()) throw new CustomException(EMPTY_CART);

        Map<Long, List<ColorDetailDto>> colorMap = new HashMap<>();  //상품의 옵션 정보를 저장
        return cartInfoList.stream()
                .map(cartInfoDto -> {
                    Long productId = cartInfoDto.productId();

                    // 해당 상품의 옵션을 조회한 적이 없을 경우
                    List<ColorDetailDto> colorDetailDtoList = colorMap.computeIfAbsent(productId, id -> {
                        List<ProductItem> itemList = productItemRep.findAllByProductId(id); //상품에 해당하는 제품 리스트
                        return productService.groupItemByColor(itemList); //해당 제품의 컬러 및 사이즈
                    });

                    return new CartInfoResDto(cartInfoDto, colorDetailDtoList);
                })
                .collect(Collectors.toList());
    }

    /**
     * 장바구니에 담긴 상품의 옵션 변경 기능
     * @param updateCartReqDto 상품의 옵션 변경 내역을 담은 Dto
     * @param userId 사용자 PK
     * */
    @Override
    @Transactional
    public void updateItemOption(UpdateCartReqDto updateCartReqDto, Long userId) {
        // 1. 가입된 사용자 여부 체크
        userService.validUser(userId);

        // 2. 해당 장바구니 내역 여부 체크
        Cart cart = getCartInfo(updateCartReqDto.cartId(), userId);

        // 3. 변경하려는 아이템 여부 체크
        ProductItem item = productItemService.findByProductItem(updateCartReqDto.itemId());

        // 4. 해당 상품이 장바구니에 담겨있는 지 체크
        if(!cart.getProductItem().getId().equals(item.getId()))
            validCart(updateCartReqDto.cartId(), userId);

        // 5. 재고 검증
        validStock(item, updateCartReqDto.quantity());

        // 6. 변경 사항 반영
        cart.updateCart(item, updateCartReqDto.quantity());
    }

    /**
     * 장바구니 내역 삭제 기능
     * @param cartList 삭제할 Cart PK를 담은 리스트
     * @param userId 사용자 PK
     * */
    @Override
    @Transactional
    public void deleteCartItem(List<Long> cartList, Long userId) {
        // 1. 가입된 사용자 여부 체크
        userService.validUser(userId);

        // 2. 리스트에 담긴 cartId 순서대로 삭제
        for (Long cartId : cartList) {
            cartRep.delete(getCartInfo(cartId, userId));
        }
    }

    /**
     * 해당 장바구니 정보 조회
     * @param cartId 장바구니 PK
     * @param userId 사용자 PK
     * @return Cart entity
     * */
    public Cart getCartInfo(Long cartId, Long userId) {
        return cartRep.findByCartIdAndUserUserId(cartId, userId)
                .orElseThrow(() -> new CustomException(NOT_EXIST_CART));
    }

    /**
     * 변경하려는 상품이 장바구니에 담겨있는 지 체크
     * @param itemId 상품 아이템 PK
     * @param userId 사용자 PK
     * */
    public void validCart(Long itemId, Long userId) {
        boolean existCart = cartRep.existsByProductItemIdAndUserUserId(itemId, userId);
        if(existCart) throw new CustomException(EXIST_CART);
    }

    /**
     * 상품 아이템 재고 조회
     * @param item 상품 아이템 entity
     * @param quantity 변경할 수량
     * */
    public void validStock(ProductItem item, Long quantity) {
        if(!item.validStock(quantity)) throw new CustomException(OVER_QUANTITY);
    }
}