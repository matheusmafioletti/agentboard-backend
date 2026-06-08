package com.agentboard.commons.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentboard.commons.exceptions.ResourceNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResourceNotFoundExceptionTest {

  @Test
  void shouldSetMessage_whenConstructedWithMessage() {
    ResourceNotFoundException ex = new ResourceNotFoundException("WorkItem 123 not found");

    assertThat(ex.getMessage()).isEqualTo("WorkItem 123 not found");
  }

  @Test
  void shouldFormatMessage_whenForIdCalled() {
    UUID id = UUID.randomUUID();
    ResourceNotFoundException ex = ResourceNotFoundException.forId("Project", id);

    assertThat(ex.getMessage()).isEqualTo("Project not found: " + id);
  }

  @Test
  void shouldFormatMessageWithStringId_whenForIdCalledWithString() {
    ResourceNotFoundException ex = ResourceNotFoundException.forId("Feature", "F42");

    assertThat(ex.getMessage()).isEqualTo("Feature not found: F42");
    assertThat(ex).isInstanceOf(RuntimeException.class);
  }
}
