package org.example.mollyapi.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.mollyapi.cart.dto.Request.AddCartReqDto;
import org.example.mollyapi.cart.dto.Request.UpdateCartReqDto;
import org.example.mollyapi.cart.dto.Response.CartInfoDto;
import org.example.mollyapi.cart.dto.Response.CartInfoResDto;
import org.example.mollyapi.cart.service.CartService;
import org.example.mollyapi.common.config.ApiQueryCounter;
import org.example.mollyapi.product.dto.response.ColorDetailDto;
import org.example.mollyapi.product.dto.response.SizeDetailDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
public class CartControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApiQueryCounter apiQueryCounter;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @DisplayName("장바구니에 상품을 등록한다.")
    @Test
    void addCart() throws Exception {
        // given
        Long userId = 1L;
        Long itemId = 1L;
        Long quantity = 3L;
        AddCartReqDto addCartReqDto = new AddCartReqDto(itemId, quantity);

        // when & then
        mockMvc.perform(
                post("/cart/add")
                        .content(objectMapper.writeValueAsString(addCartReqDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(mockHttpServletRequest -> {
                            mockHttpServletRequest.setAttribute("userId", userId);
                            return mockHttpServletRequest;
                        }))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("장바구니 등록에 성공했습니다."));
    }

    @DisplayName("장바구니 내역을 조회한다.")
    @Test
    void getCartDetail() throws Exception {
        // given
        Long userId = 1L;
        CartInfoResDto cartInfoResDto = new CartInfoResDto(
                new CartInfoDto(1L, 1L, "Red", "M", 1L, "Product A", "Brand X", 100L, "url", 2L),
                List.of(new ColorDetailDto("Red", "R123", List.of(new SizeDetailDto(1L,"M", 10L)))));
        List<CartInfoResDto> cartInfoList = List.of(cartInfoResDto);

        when(cartService.getCartDetail(userId)).thenReturn(cartInfoList);

        // when // then
        mockMvc.perform(
                    get("/cart")
                        .with(mockHttpServletRequest -> {
                            mockHttpServletRequest.setAttribute("userId", userId);
                            return mockHttpServletRequest;
                        }))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cartInfoDto.productName").value("Product A"))
                .andExpect(jsonPath("$[0].colorDetails[0].color").value("Red"));
    }

    @DisplayName("장바구니 상품의 옵션을 변경한한다.")
    @Test
    void updateItemOptionSuccessfully() throws Exception {
        // given
        Long userId = 1L;
        Long cartId = 1L;
        Long itemId = 1L;
        Long quantity = 3L;

        UpdateCartReqDto updateCartReqDto = new UpdateCartReqDto(cartId, itemId, quantity);

        // when // then
        mockMvc.perform(
                put("/cart")
                        .content(objectMapper.writeValueAsString(updateCartReqDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(mockHttpServletRequest -> {
                            mockHttpServletRequest.setAttribute("userId", userId);
                            return mockHttpServletRequest;
                        }))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("옵션 변경에 성공했습니다."));
    }

    @DisplayName("장바구니 상품을 성공적으로 삭제한다.")
    @Test
    void deleteCartItemSuccessfully() throws Exception {
        // given
        Long userId = 1L;
        List<Long> cartList = List.of(1L, 2L);

        // when // then
        mockMvc.perform(delete("/cart")
                        .content(objectMapper.writeValueAsString(cartList))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(mockHttpServletRequest -> {
                            mockHttpServletRequest.setAttribute("userId", userId);
                            return mockHttpServletRequest;
                        }))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("장바구니 내역 삭제에 성공했습니다."));
    }
}
