package com.agentboard.commons.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentboard.commons.domain.WorkItemType;
import org.junit.jupiter.api.Test;

class WorkItemTypeTest {

  @Test
  void shouldHaveThreeValues_whenValuesReturnAllTypes() {
    assertThat(WorkItemType.values()).hasSize(3);
  }

  @Test
  void shouldContainExpectedTypes_whenCheckingNames() {
    assertThat(WorkItemType.FEATURE.name()).isEqualTo("FEATURE");
    assertThat(WorkItemType.USER_STORY.name()).isEqualTo("USER_STORY");
    assertThat(WorkItemType.TASK.name()).isEqualTo("TASK");
  }

  @Test
  void shouldReturnEnumFromName_whenValueOfCalled() {
    assertThat(WorkItemType.valueOf("FEATURE")).isEqualTo(WorkItemType.FEATURE);
    assertThat(WorkItemType.valueOf("USER_STORY")).isEqualTo(WorkItemType.USER_STORY);
    assertThat(WorkItemType.valueOf("TASK")).isEqualTo(WorkItemType.TASK);
  }
}
