package com.agentboard.commons.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentboard.commons.domain.FeatureStage;
import org.junit.jupiter.api.Test;

class FeatureStageTest {

  @Test
  void shouldReturnTrue_whenIsAutoOnlyForInDevelopment() {
    assertThat(FeatureStage.IN_DEVELOPMENT.isAutoOnly()).isTrue();
  }

  @Test
  void shouldReturnTrue_whenIsAutoOnlyForPrReview() {
    assertThat(FeatureStage.PR_REVIEW.isAutoOnly()).isTrue();
  }

  @Test
  void shouldReturnFalse_whenIsAutoOnlyForManualStages() {
    assertThat(FeatureStage.BACKLOG.isAutoOnly()).isFalse();
    assertThat(FeatureStage.SPECIFY.isAutoOnly()).isFalse();
    assertThat(FeatureStage.CLARIFY.isAutoOnly()).isFalse();
    assertThat(FeatureStage.PLAN.isAutoOnly()).isFalse();
    assertThat(FeatureStage.TASKS.isAutoOnly()).isFalse();
    assertThat(FeatureStage.READY.isAutoOnly()).isFalse();
    assertThat(FeatureStage.DONE.isAutoOnly()).isFalse();
  }

  @Test
  void shouldHaveNineValues_whenValuesReturnAllStages() {
    assertThat(FeatureStage.values()).hasSize(9);
  }

  @Test
  void shouldReturnEnumFromName_whenValueOfCalled() {
    assertThat(FeatureStage.valueOf("BACKLOG")).isEqualTo(FeatureStage.BACKLOG);
    assertThat(FeatureStage.valueOf("IN_DEVELOPMENT")).isEqualTo(FeatureStage.IN_DEVELOPMENT);
    assertThat(FeatureStage.valueOf("DONE")).isEqualTo(FeatureStage.DONE);
  }
}
