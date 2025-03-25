package org.example.mollyapi.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.mollyapi.common.dto.CommonResDto;
import org.example.mollyapi.common.exception.CustomErrorResponse;
import org.example.mollyapi.product.dto.response.ListResDto;
import org.example.mollyapi.product.dto.response.PageResDto;
import org.example.mollyapi.review.dto.request.AddReviewReqDto;
import org.example.mollyapi.review.dto.response.GetMyReviewResDto;
import org.example.mollyapi.review.dto.response.GetReviewResDto;
import org.example.mollyapi.review.service.ReviewService;
import org.example.mollyapi.user.auth.annotation.Auth;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "리뷰 Controller", description = "리뷰 담당")
@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @Auth
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "리뷰내역 작성 API", description = "배송완료된 상품의 리뷰를 작성할 수 있습니다. id = order_detail_id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리뷰 등록 성공",
                    content = @Content(schema = @Schema(implementation = CommonResDto.class))),
            @ApiResponse(responseCode = "400", description = "1. 존재 하지 않는 사용자 \t\n 2. 주문 상세 조회 불가",
                    content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    })
    public ResponseEntity<?> registerReview(
            @Valid @RequestPart("review") AddReviewReqDto addReviewReqDto,
            @RequestPart(value = "reviewImages", required = false) List<MultipartFile> uploadImages,
            HttpServletRequest request){
        Long userId = (Long) request.getAttribute("userId");
        reviewService.registerReview(addReviewReqDto, uploadImages, userId);
        return ResponseEntity.ok(new CommonResDto("리뷰 등록에 성공했습니다."));
    }

    @Auth
    @GetMapping("/{productId}")
    @Operation(summary = "상품별 리뷰 내역 조회 API", description = "상품의 리뷰를 조회할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리뷰 조회 성공",
                    content = @Content(schema = @Schema(implementation = GetReviewResDto.class))),
            @ApiResponse(responseCode = "204", description = "등록된 리뷰가 없는 상품",
                    content = @Content(schema = @Schema(implementation = CommonResDto.class))),
            @ApiResponse(responseCode = "400", description = "1. 존재 하지 않는 상품 \t\n 2. 리뷰 조회 실패",
                    content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    })
    public ResponseEntity<?> getReviewList(
            @PathVariable("productId") Long productId,
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        PageRequest pageRequest = PageRequest.of(page, size);
        SliceImpl<GetReviewResDto> sliceList = reviewService.getReviewList(pageRequest, productId, userId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ListResDto(
                        new PageResDto(
                                (long) sliceList.getNumberOfElements(), // 현재 페이지 요소 개수
                                sliceList.hasNext(), // 다음 페이지 존재 여부
                                sliceList.isFirst(), // 첫 번째 페이지 여부
                                sliceList.isLast(), //마지막 페이지 여부
                                null
                        ),
                        sliceList.getContent()
                ));
    }

    @GetMapping("/{productId}/new")
    @Operation(summary = "비회원용 상품별 리뷰 내역 조회 API", description = "상품의 리뷰를 조회할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리뷰 조회 성공",
                    content = @Content(schema = @Schema(implementation = GetReviewResDto.class))),
            @ApiResponse(responseCode = "204", description = "등록된 리뷰가 없는 상품",
                    content = @Content(schema = @Schema(implementation = CommonResDto.class))),
            @ApiResponse(responseCode = "400", description = "1. 존재 하지 않는 상품 \t\n 2. 리뷰 조회 실패",
                    content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    })
    public ResponseEntity<?> getReviewListByNonUser(
            @PathVariable("productId") Long productId,
            @RequestParam("page") int page,
            @RequestParam("size") int size
    ) {
        Long userId = 0L; //비회원으로 체크
        PageRequest pageRequest = PageRequest.of(page, size);
        SliceImpl<GetReviewResDto> sliceList = reviewService.getReviewList(pageRequest, productId, userId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ListResDto(
                        new PageResDto(
                                (long) sliceList.getNumberOfElements(), // 현재 페이지 요소 개수
                                sliceList.hasNext(), // 다음 페이지 존재 여부
                                sliceList.isFirst(), // 첫 번째 페이지 여부
                                sliceList.isLast(), //마지막 페이지 여부
                                null
                        ),
                        sliceList.getContent()
                ));
    }

    @Auth
    @GetMapping("/myReview")
    @Operation(summary = "로그인한 사용자의 리뷰내역 조회 API", description = "자신이 작성한 리뷰내역을 조회할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리뷰 조회 성공",
                    content = @Content(schema = @Schema(implementation = GetMyReviewResDto.class))),
            @ApiResponse(responseCode = "204", description = "작성한 리뷰가 없음",
                    content = @Content(schema = @Schema(implementation = CommonResDto.class))),
            @ApiResponse(responseCode = "400", description = "리뷰 조회 실패",
                    content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    })
    public ResponseEntity<?> getMyReviewList(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        PageRequest pageRequest = PageRequest.of(page, size);
        SliceImpl<GetMyReviewResDto> sliceList =  reviewService.getMyReviewList(pageRequest, userId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ListResDto(
                        new PageResDto(
                                (long) sliceList.getNumberOfElements(), // 현재 페이지 요소 개수
                                sliceList.hasNext(), // 다음 페이지 존재 여부
                                sliceList.isFirst(), // 첫 번째 페이지 여부
                                sliceList.isLast(), //마지막 페이지 여부
                                null
                        ),
                        sliceList.getContent()
                ));
    }

    @Auth
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "리뷰 수정 API", description = "자신이 작성한 리뷰 내역을 수정할 수 있습니다. id = review_id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리뷰 수정 성공",
                    content = @Content(schema = @Schema(implementation = CommonResDto.class))),
            @ApiResponse(responseCode = "400", description = "1. 리뷰 수정 실패 \t\n 2. 존재하지 않는 사용자 \t\n 3. 작성 권한 없음 ",
                    content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    })
    public ResponseEntity<?> updateReview(
            @Valid @RequestPart("review") AddReviewReqDto addReviewReqDto,
            @RequestPart(value = "reviewImages", required = false) List<MultipartFile> uploadImages,
            HttpServletRequest request){
        Long userId = (Long) request.getAttribute("userId");
        reviewService.updateReview(addReviewReqDto, uploadImages, userId);
        return ResponseEntity.ok(new CommonResDto("리뷰 수정에 성공했습니다."));
    }

    @Auth
    @DeleteMapping("/{reviewId}")
    @Operation(summary = "리뷰 삭제 API", description = "자신이 작성한 리뷰 내역을 삭제할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리뷰 삭제 성공",
                    content = @Content(schema = @Schema(implementation = CommonResDto.class))),
            @ApiResponse(responseCode = "204", description = "작성한 리뷰가 없음",
                    content = @Content(schema = @Schema(implementation = CustomErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "리뷰 삭제 실패",
                    content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    })
    public ResponseEntity<?> deleteReview(
            @PathVariable("reviewId") Long reviewId,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok(new CommonResDto("리뷰 삭제에 성공했습니다."));
    }
}
