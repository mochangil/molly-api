package org.example.mollyapi.product.dto.response;

import org.springframework.data.domain.Slice;

public record PageResDto(
        Long size,
        Boolean hasNext,
        Boolean isFirst,
        Boolean isLast,
        Long lastElementId
) {
    public static PageResDto of(Slice<?> slice, Long lastElementId) {
        return new PageResDto(
                (long)slice.getContent().size(),
                slice.hasNext(),
                slice.isFirst(),
                slice.isLast(),
                lastElementId
        );
    }
}
