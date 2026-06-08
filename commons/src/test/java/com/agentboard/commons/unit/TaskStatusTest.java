package com.agentboard.commons.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentboard.commons.domain.TaskStatus;
import org.junit.jupiter.api.Test;

class TaskStatusTest {

  @Test
  void shouldHaveThreeValues_whenValuesReturnAllStatuses() {
    assertThat(TaskStatus.values()).hasSize(3);
  }

  @Test
  void shouldContainExpectedStatuses_whenCheckingNames() {
    assertThat(TaskStatus.NEW.name()).isEqualTo("NEW");
    assertThat(TaskStatus.ACTIVE.name()).isEqualTo("ACTIVE");
    assertThat(TaskStatus.CLOSED.name()).isEqualTo("CLOSED");
  }

  @Test
  void shouldReturnEnumFromName_whenValueOfCalled() {
    assertThat(TaskStatus.valueOf("NEW")).isEqualTo(TaskStatus.NEW);
    assertThat(TaskStatus.valueOf("ACTIVE")).isEqualTo(TaskStatus.ACTIVE);
    assertThat(TaskStatus.valueOf("CLOSED")).isEqualTo(TaskStatus.CLOSED);
  }
}
