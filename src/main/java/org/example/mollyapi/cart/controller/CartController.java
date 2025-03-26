package org.example.mollyapi.cart.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.mollyapi.cart.dto.Request.AddCartReqDto;
import org.example.mollyapi.cart.dto.Request.UpdateCartReqDto;
import org.example.mollyapi.cart.dto.Response.CartInfoResDto;
import org.example.mollyapi.cart.service.CartService;
import org.example.mollyapi.common.dto.CommonResDto;
import org.example.mollyapi.common.exception.CustomErrorResponse;
import org.example.mollyapi.user.auth.annotation.Auth;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "장바구니 Controller", description = "장바구니 기능을 담당")
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @Auth
    @PostMapping("/add")
    @Operation(summary = "장바구니에 상품 추가 API", description = "장바구니에 상품을 추가 할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "장바구니 담기 성공",
                    content = @Content(schema = @Schema(implementation = CommonResDto.class))),
            @ApiResponse(responseCode = "400", description = "1. 존재하지 않는 사용자 \t\n 2. 존재하지 않는 상품 \t\n 3. 상품 품절 \t\n 4. 재고 수량 부족 \t\n 5. 장바구니 최대 수량 초과",
                    content = @Content(schema = @Schema(implementation = CustomErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "장바구니 등록 실패",
                    content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    })
    public ResponseEntity<?> addCart(
            @Valid @RequestBody AddCartReqDto addCartReqDto,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        cartService.addCart(addCartReqDto, userId);
        return ResponseEntity.ok(new CommonResDto("장바구니 등록에 성공했습니다."));
    }

    @Auth
    @GetMapping()
    @Operation(summary = "장바구니 상품 조회 API", description = "장바구니에 담긴 상품을 조회할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "장바구니 내역 조회 성공",
                    content = @Content(schema = @Schema(implementation = CartInfoResDto.class))),
            @ApiResponse(responseCode = "204", description = "빈 장바구니",
                    content = @Content(schema = @Schema(implementation = CustomErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "가입 되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    })
    public ResponseEntity<?> getCartDetail(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<CartInfoResDto> responseDtoList = cartService.getCartDetail(userId);
        return ResponseEntity.ok(responseDtoList);
    }

    @Auth
    @PutMapping()
    @Operation(summary = "장바구니 상품 수정 API", description = "장바구니에 담긴 상품의 옵션을 수정할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "1. 장바구니 내역 수정 성공 \t\n 2. 변경 사항이 없는 경우",
                    content = @Content(schema = @Schema(implementation = CommonResDto.class))),
            @ApiResponse(responseCode = "409", description = "동일한 내역이 이미 장바구니에 존재할 경우",
                    content = @Content(schema = @Schema(implementation = CustomErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "1. 존재하지 않는 사용자 \t\n 2. 요청이 잘못된 경우 \t\n 3. 존재하지 않는 상품 \t\n 4. 준비된 재고 초과",
                    content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    })
    public ResponseEntity<?> updateItemOption(
            @Valid @RequestBody UpdateCartReqDto updateCartReqDto,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        cartService.updateItemOption(updateCartReqDto, userId);
        return ResponseEntity.ok(new CommonResDto("옵션 변경에 성공했습니다."));
    }

    @Auth
    @DeleteMapping()
    @Operation(summary = "장바구니 상품 삭제", description = "장바구니에 담긴 상품을 삭제할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "장바구니 내역 삭제 성공",
                    content = @Content(schema = @Schema(implementation = CommonResDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 삭제 요청",
                    content = @Content(schema = @Schema(implementation = CustomErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "장바구니 내역 삭제 실패",
                    content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    })
    public ResponseEntity<?> deleteCartItem(
            @Valid @RequestBody List<Long> cartList,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        cartService.deleteCartItem(cartList, userId);
        return ResponseEntity.ok(new CommonResDto("장바구니 내역 삭제에 성공했습니다."));
    }
}
