package com.example.videoplatform.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ErrorCodeTest {

    @Test
    void numericCodesAreUnique() {
        long uniqueCodeCount = Arrays.stream(ErrorCode.values())
                .map(ErrorCode::getCode)
                .distinct()
                .count();

        assertThat(uniqueCodeCount).isEqualTo(ErrorCode.values().length);
    }
}
