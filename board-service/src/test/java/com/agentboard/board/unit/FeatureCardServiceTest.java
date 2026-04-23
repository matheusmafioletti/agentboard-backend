package com.agentboard.board.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentboard.board.domain.ColumnDef;
import com.agentboard.board.domain.FeatureCard;
import com.agentboard.board.domain.Stage;
import com.agentboard.board.dto.FeatureCardResponse;
import com.agentboard.board.repository.ArtifactRepository;
import com.agentboard.board.repository.ColumnDefRepository;
import com.agentboard.board.repository.CommandExecutionRepository;
import com.agentboard.board.repository.FeatureCardRepository;
import com.agentboard.board.repository.TaskRepository;
import com.agentboard.board.service.BoardEventPublisher;
import com.agentboard.board.service.FeatureCardService;
import com.agentboard.commons.exceptions.ResourceNotFoundException;
import com.agentboard.commons.exceptions.TenantMismatchException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link FeatureCardService}. */
@ExtendWith(MockitoExtension.class)
class FeatureCardServiceTest {

  @Mock
  private FeatureCardRepository featureCardRepository;

  @Mock
  private ColumnDefRepository columnDefRepository;

  @Mock
  private BoardEventPublisher boardEventPublisher;

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private ArtifactRepository artifactRepository;

  @Mock
  private CommandExecutionRepository commandExecutionRepository;

  private FeatureCardService service;

  private UUID tenantId;
  private UUID columnId;

  @BeforeEach
  void setUp() {
    service = new FeatureCardService(
        featureCardRepository, columnDefRepository, boardEventPublisher,
        taskRepository, artifactRepository, commandExecutionRepository);
    tenantId = UUID.randomUUID();
    columnId = UUID.randomUUID();
  }

  @Test
  void create_setsDisplayOrderToZeroWhenColumnIsEmpty() {
    ColumnDef backlog = mockBacklogColumn();
    when(featureCardRepository.findMaxDisplayOrderByColumnIdAndTenantId(columnId, tenantId))
        .thenReturn(Optional.empty());
    when(featureCardRepository.save(any(FeatureCard.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    FeatureCardResponse response = service.create(tenantId, "My Feature", null);

    assertThat(response.title()).isEqualTo("My Feature");
    assertThat(response.displayOrder()).isZero();
    assertThat(response.tenantId()).isEqualTo(tenantId);
    assertThat(response.columnId()).isEqualTo(backlog.getId());
  }

  @Test
  void create_incrementsDisplayOrderWhenColumnHasCards() {
    mockBacklogColumn();
    when(featureCardRepository.findMaxDisplayOrderByColumnIdAndTenantId(columnId, tenantId))
        .thenReturn(Optional.of(4));
    when(featureCardRepository.save(any(FeatureCard.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    FeatureCardResponse response = service.create(tenantId, "Next Feature", "desc");

    assertThat(response.displayOrder()).isEqualTo(5);
  }

  @Test
  void create_throwsWhenNoBacklogColumnForTenant() {
    when(columnDefRepository.findByTenantIdAndStage(tenantId, Stage.BACKLOG))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(tenantId, "Title", null))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void update_patchesTitleAndDescription() {
    FeatureCard card = buildCard(tenantId, columnId);
    when(featureCardRepository.findById(card.getId())).thenReturn(Optional.of(card));
    when(featureCardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    FeatureCardResponse result =
        service.update(tenantId, card.getId(), "New Title", "New Desc", null);

    assertThat(result.title()).isEqualTo("New Title");
    assertThat(result.description()).isEqualTo("New Desc");
  }

  @Test
  void update_throwsTenantMismatchForWrongTenant() {
    UUID otherTenant = UUID.randomUUID();
    FeatureCard card = buildCard(otherTenant, columnId);
    when(featureCardRepository.findById(card.getId())).thenReturn(Optional.of(card));

    assertThatThrownBy(() -> service.update(tenantId, card.getId(), "X", null, null))
        .isInstanceOf(TenantMismatchException.class);
  }

  @Test
  void delete_removesCardForCorrectTenant() {
    FeatureCard card = buildCard(tenantId, columnId);
    when(featureCardRepository.findById(card.getId())).thenReturn(Optional.of(card));

    service.delete(tenantId, card.getId());

    verify(featureCardRepository).delete(card);
  }

  @Test
  void delete_throwsTenantMismatchForWrongTenant() {
    UUID otherTenant = UUID.randomUUID();
    FeatureCard card = buildCard(otherTenant, columnId);
    when(featureCardRepository.findById(card.getId())).thenReturn(Optional.of(card));

    assertThatThrownBy(() -> service.delete(tenantId, card.getId()))
        .isInstanceOf(TenantMismatchException.class);
  }

  @Test
  void getById_throwsResourceNotFoundWhenCardMissing() {
    UUID cardId = UUID.randomUUID();
    when(featureCardRepository.findById(cardId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(tenantId, cardId))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void move_updatesColumnIdAndDisplayOrder() {
    UUID targetColumnId = UUID.randomUUID();
    FeatureCard card = buildCard(tenantId, columnId);
    ColumnDef targetColumn = mockColumn(targetColumnId, tenantId, Stage.SPECIFY);
    when(featureCardRepository.findById(card.getId())).thenReturn(Optional.of(card));
    when(featureCardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    FeatureCardResponse result = service.move(tenantId, card.getId(), targetColumnId, 2);

    assertThat(result.columnId()).isEqualTo(targetColumnId);
    assertThat(result.displayOrder()).isEqualTo(2);
    verify(boardEventPublisher).publishCardMoved(any(), any(), any(), any(), any(), anyInt());
  }

  @Test
  void move_throwsTenantMismatchWhenCardBelongsToOtherTenant() {
    UUID otherTenant = UUID.randomUUID();
    FeatureCard card = buildCard(otherTenant, columnId);
    when(featureCardRepository.findById(card.getId())).thenReturn(Optional.of(card));

    assertThatThrownBy(() -> service.move(tenantId, card.getId(), UUID.randomUUID(), 0))
        .isInstanceOf(TenantMismatchException.class);
  }

  @Test
  void move_throwsResourceNotFoundWhenTargetColumnMissing() {
    UUID missingColumnId = UUID.randomUUID();
    FeatureCard card = buildCard(tenantId, columnId);
    when(featureCardRepository.findById(card.getId())).thenReturn(Optional.of(card));
    when(columnDefRepository.findById(missingColumnId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.move(tenantId, card.getId(), missingColumnId, 0))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void setReExecutionPending_setsFlag() {
    FeatureCard card = buildCard(tenantId, columnId);
    when(featureCardRepository.findById(card.getId())).thenReturn(Optional.of(card));
    when(featureCardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    FeatureCardResponse result = service.setReExecutionPending(tenantId, card.getId(), true);

    assertThat(result.reExecutionPending()).isTrue();
  }

  @Test
  void setReExecutionPending_clearsFlagWhenSetFalse() {
    FeatureCard card = buildCard(tenantId, columnId);
    when(featureCardRepository.findById(card.getId())).thenReturn(Optional.of(card));
    when(featureCardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    FeatureCardResponse result = service.setReExecutionPending(tenantId, card.getId(), false);

    assertThat(result.reExecutionPending()).isFalse();
  }

  @Test
  void setReExecutionPending_throwsTenantMismatchForWrongTenant() {
    UUID otherTenant = UUID.randomUUID();
    FeatureCard card = buildCard(otherTenant, columnId);
    when(featureCardRepository.findById(card.getId())).thenReturn(Optional.of(card));

    assertThatThrownBy(() -> service.setReExecutionPending(tenantId, card.getId(), true))
        .isInstanceOf(TenantMismatchException.class);
  }

  @Test
  void move_throwsTenantMismatchWhenTargetColumnBelongsToOtherTenant() {
    UUID targetColumnId = UUID.randomUUID();
    UUID otherTenant = UUID.randomUUID();
    FeatureCard card = buildCard(tenantId, columnId);
    ColumnDef col = org.mockito.Mockito.mock(ColumnDef.class);
    when(col.getTenantId()).thenReturn(otherTenant);
    when(columnDefRepository.findById(targetColumnId)).thenReturn(Optional.of(col));
    when(featureCardRepository.findById(card.getId())).thenReturn(Optional.of(card));

    assertThatThrownBy(() -> service.move(tenantId, card.getId(), targetColumnId, 0))
        .isInstanceOf(TenantMismatchException.class);
  }

  private ColumnDef mockBacklogColumn() {
    ColumnDef backlog = org.mockito.Mockito.mock(ColumnDef.class);
    when(backlog.getId()).thenReturn(columnId);
    when(columnDefRepository.findByTenantIdAndStage(tenantId, Stage.BACKLOG))
        .thenReturn(Optional.of(backlog));
    return backlog;
  }

  private ColumnDef mockColumn(UUID id, UUID owner, Stage stage) {
    ColumnDef col = org.mockito.Mockito.mock(ColumnDef.class);
    when(col.getTenantId()).thenReturn(owner);
    when(col.getStage()).thenReturn(stage);
    when(columnDefRepository.findById(id)).thenReturn(Optional.of(col));
    return col;
  }

  private FeatureCard buildCard(UUID owner, UUID col) {
    FeatureCard card = new FeatureCard(owner, col, "Title", "Desc", 0);
    java.lang.reflect.Field idField;
    try {
      idField = FeatureCard.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(card, UUID.randomUUID());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return card;
  }
}
