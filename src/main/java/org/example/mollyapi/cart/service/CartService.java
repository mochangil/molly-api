package org.example.mollyapi.cart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mollyapi.cart.dto.Request.AddCartReqDto;
import org.example.mollyapi.cart.dto.Request.UpdateCartReqDto;
import org.example.mollyapi.cart.dto.Response.CartInfoDto;
import org.example.mollyapi.cart.dto.Response.CartInfoResDto;
import org.example.mollyapi.cart.entity.Cart;
import org.example.mollyapi.cart.repository.CartRepository;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.product.dto.response.ColorDetailDto;
import org.example.mollyapi.product.entity.ProductItem;
import org.example.mollyapi.product.repository.ProductItemRepository;
import org.example.mollyapi.product.service.impl.ProductServiceImpl;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.example.mollyapi.common.exception.error.impl.CartError.*;
import static org.example.mollyapi.common.exception.error.impl.ProductItemError.*;
import static org.example.mollyapi.common.exception.error.impl.UserError.NOT_EXISTS_USER;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {
    private final ProductServiceImpl productService;
    private final UserRepository userRep;
    private final ProductItemRepository productItemRep;
    private final CartRepository cartRep;

    /**
     * 장바구니에 상품 담기 기능
     * @param addCartReqDto 추가하려는 데이터
     * @param userId 사용자 PK
     */
    @Transactional
    public void addCart(AddCartReqDto addCartReqDto, Long userId) {
        // 1. 가입된 사용자 여부 체크
        User user = userRep.findById(userId)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_USER));

        // 2. 상품 존재 여부 체크
        ProductItem item = getProductItemInfo(addCartReqDto.itemId());

        // 3. 상품의 재고가 남아 있는 지 체크
        Long itemQuantity = item.getQuantity(); // item.getQuantity()를 변수에 저장
        if(itemQuantity == null || itemQuantity == 0 || itemQuantity < addCartReqDto.quantity())
            throw new CustomException(OVER_QUANTITY);

        // 4. 장바구니에 동일한 상품이 담겨 있는 지 체크
        Cart cart = cartRep.findByProductItemIdAndUserUserId(addCartReqDto.itemId(), userId);

        // 5. 존재하지 않으면 삽입
        if(cart == null) insertNewCart(addCartReqDto, user, item);
        else {
            // 6. 초과 수량을 장바구니에 담는 지 체크
            long totalQuantity = cart.getQuantity() + addCartReqDto.quantity(); //기존에 담아둔 수량 + 추가 하려는 수량
            checkStock(item.getQuantity(), totalQuantity);

            // 6-1. 수량 업데이트
            try {
                cart.updateQuantity(totalQuantity);
            } catch (CustomException e) {
                throw new CustomException(FAIL_UPDATE); // 수량 업데이트 실패
            }
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
    @Transactional(readOnly = true)
    public List<CartInfoResDto> getCartDetail(Long userId) {
        // 가입된 사용자 여부 체크
        existsUser(userId);

        // 사용자 장바구니 조회
        List<CartInfoDto> cartInfoList = cartRep.getCartInfo(userId);
        if(cartInfoList.isEmpty()) throw new CustomException(EMPTY_CART);

        List<CartInfoResDto> responseDtoList = new ArrayList<>();
        Map<Long, List<ColorDetailDto>> colorMap = new HashMap<>();  //상품의 옵션 정보를 저장
        for(CartInfoDto cartInfoDto : cartInfoList) {
            Long productId = cartInfoDto.productId();
            List<ColorDetailDto> colorDetails;

            if(colorMap.containsKey(productId)) { //이미 조회한 적이 있는 상품일 때
                colorDetails = colorMap.get(productId);
            } else {
                //상품에 해당하는 제품 리스트
                List<ProductItem> itemList = productItemRep.findAllByProductId(productId);

                //해당 제품의 컬러 및 사이즈
                colorDetails = productService.groupItemByColor(itemList);
                colorMap.put(productId, colorDetails);
            }
            responseDtoList.add(new CartInfoResDto(cartInfoDto, colorDetails));
        }
        return responseDtoList;
    }

    /**
     * 장바구니에 담긴 상품의 옵션 변경 기능
     * @param updateCartReqDto 상품의 옵션 변경 내역을 담은 Dto
     * @param userId 사용자 PK
     * */
    @Transactional
    public void updateItemOption(UpdateCartReqDto updateCartReqDto, Long userId) {
        // 1. 가입된 사용자 여부 체크
        existsUser(userId);

        // 2. 해당 장바구니 내역 여부 체크
        Cart cart = getCartInfo(updateCartReqDto.cartId(), userId);

        // 3. 변경하려는 아이템 여부 체크
        ProductItem item = getProductItemInfo(updateCartReqDto.itemId());

        // 4. 해당 상품이 장바구니에 담겨있는 지 체크
        if(!cart.getProductItem().getId().equals(item.getId())) {
            Cart checkCart = cartRep.findByProductItemIdAndUserUserId(updateCartReqDto.itemId(), userId);
            if(checkCart != null) throw new CustomException(EXIST_CART);
        }

        // 5. 재고 검증
        checkStock(item.getQuantity(), updateCartReqDto.quantity());

        // 6. 변경 사항 반영
        cart.updateCart(item, updateCartReqDto.quantity());
    }

    /**
     * 장바구니 내역 삭제 기능
     * @param cartList 삭제할 Cart PK를 담은 리스트
     * @param userId 사용자 PK
     * */
    @Transactional
    public void deleteCartItem(List<Long> cartList, Long userId) {
        // 1. 가입된 사용자 여부 체크
        existsUser(userId);

        // 2. 리스트에 담긴 cartId 순서대로 삭제
        for (Long cartId : cartList) {
            cartRep.delete(getCartInfo(cartId, userId));
        }
    }

    /**
     * 가입된 사용자 여부 체크
     * @param userId 사용자 PK
     * */
    public void existsUser(Long userId) {
        boolean existsUser = userRep.existsById(userId);
        if(!existsUser) throw new CustomException(NOT_EXISTS_USER);
    }

    /**
     * 상품 아이템 정보 조회
     * @param itemId 상품 아이템 Pk
     * @return ProductItem entity
     * */
    public ProductItem getProductItemInfo(Long itemId) {
        return productItemRep.findById(itemId)
                .orElseThrow(() -> new CustomException(NOT_EXISTS_ITEM));
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
     * 재고 수량을 초과 여부 계산
     * @param itemQuantity 상품 현재 수량
     * @param nowQuantity 변경할 수량
     * */
    public void checkStock(Long itemQuantity, Long nowQuantity) {
        if (itemQuantity < nowQuantity) throw new CustomException(OVER_QUANTITY);
    }

    /**
     * 장바구니 보관기간이 365일은 넘긴 상품 삭제
     * */
    public void deleteExpiredCarts() {
        List<Long> expiredCartIdList = cartRep.getExpiredCartId();
        if(expiredCartIdList.isEmpty())
            log.info("만료된 장바구니 내역이 존재하지 않습니다.");

        for(Long cartId : expiredCartIdList) {
            cartRep.deleteById(cartId);
        }
    }
}
