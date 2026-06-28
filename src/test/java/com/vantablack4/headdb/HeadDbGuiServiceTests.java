package com.vantablack4.headdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

final class HeadDbGuiServiceTests {
    @Test
    void resultSlotsMatchSixRowChestContentArea() {
        assertThat(HeadDbGuiService.RESULT_SLOTS)
            .hasSize(HeadDbGuiService.PAGE_SIZE)
            .containsExactly(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
            );
    }

    @Test
    void detectsResultSlots() {
        assertThat(HeadDbGuiService.isResultSlot(10)).isTrue();
        assertThat(HeadDbGuiService.isResultSlot(43)).isTrue();
        assertThat(HeadDbGuiService.isResultSlot(HeadDbGuiService.SLOT_PREVIOUS)).isFalse();
        assertThat(HeadDbGuiService.resultIndex(37)).isEqualTo(21);
        assertThat(HeadDbGuiService.resultIndex(8)).isEqualTo(-1);
    }

    @Test
    void calculatesPageCount() {
        assertThat(HeadDbGuiService.pageCount(0, HeadDbGuiService.PAGE_SIZE)).isEqualTo(1);
        assertThat(HeadDbGuiService.pageCount(1, HeadDbGuiService.PAGE_SIZE)).isEqualTo(1);
        assertThat(HeadDbGuiService.pageCount(28, HeadDbGuiService.PAGE_SIZE)).isEqualTo(1);
        assertThat(HeadDbGuiService.pageCount(29, HeadDbGuiService.PAGE_SIZE)).isEqualTo(2);
        assertThat(HeadDbGuiService.pageCount(56, HeadDbGuiService.PAGE_SIZE)).isEqualTo(2);
        assertThat(HeadDbGuiService.pageCount(57, HeadDbGuiService.PAGE_SIZE)).isEqualTo(3);
    }

    @Test
    void rejectsInvalidPageSize() {
        assertThatThrownBy(() -> HeadDbGuiService.pageCount(1, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("positive");
    }

    @Test
    void clampsRequestedPageIndex() {
        assertThat(HeadDbGuiService.clampedPageIndex(-4, 57, HeadDbGuiService.PAGE_SIZE)).isEqualTo(0);
        assertThat(HeadDbGuiService.clampedPageIndex(0, 57, HeadDbGuiService.PAGE_SIZE)).isEqualTo(0);
        assertThat(HeadDbGuiService.clampedPageIndex(2, 57, HeadDbGuiService.PAGE_SIZE)).isEqualTo(2);
        assertThat(HeadDbGuiService.clampedPageIndex(99, 57, HeadDbGuiService.PAGE_SIZE)).isEqualTo(2);
        assertThat(HeadDbGuiService.clampedPageIndex(99, 0, HeadDbGuiService.PAGE_SIZE)).isEqualTo(0);
    }
}
