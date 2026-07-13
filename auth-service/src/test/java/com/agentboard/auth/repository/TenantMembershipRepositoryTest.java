package com.agentboard.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentboard.auth.domain.MembershipRole;
import com.agentboard.commons.domain.DataSource;
import com.agentboard.auth.domain.Tenant;
import com.agentboard.auth.domain.TenantMembership;
import com.agentboard.auth.domain.UserAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Repository slice tests for {@link TenantMembershipRepository} against PostgreSQL 16.
 */
class TenantMembershipRepositoryTest extends AbstractRepositoryTest {

  @Autowired
  TenantMembershipRepository membershipRepository;

  @Test
  void save_persistsDataSourceColumn() {
    Tenant tenant = persistTenant();
    UserAccount user = persistUser();
    TenantMembership saved = membershipRepository.save(new TenantMembership(
        user.getId(), tenant.getId(), MembershipRole.USER, DataSource.AUTOMATION));

    assertThat(membershipRepository.findById(saved.getId()).orElseThrow().getDataSource())
        .isEqualTo(DataSource.AUTOMATION);
  }

  @Test
  void findByTenantId_returnsOnlyMembershipsOfThatTenant() {
    Tenant tenantA = persistTenant();
    Tenant tenantB = persistTenant();
    UserAccount userOne = persistUser();
    UserAccount userTwo = persistUser();

    membershipRepository.save(new TenantMembership(
        userOne.getId(), tenantA.getId(), MembershipRole.ADMIN));
    membershipRepository.save(new TenantMembership(
        userTwo.getId(), tenantA.getId(), MembershipRole.USER));
    membershipRepository.save(new TenantMembership(
        userTwo.getId(), tenantB.getId(), MembershipRole.ADMIN));

    assertThat(membershipRepository.findByTenantId(tenantA.getId()))
        .hasSize(2)
        .allSatisfy(m -> assertThat(m.getTenantId()).isEqualTo(tenantA.getId()));
    assertThat(membershipRepository.findByTenantId(tenantB.getId())).hasSize(1);
  }

  @Test
  void findByUserId_returnsMembershipsAcrossTenants() {
    Tenant tenantA = persistTenant();
    Tenant tenantB = persistTenant();
    UserAccount user = persistUser();

    membershipRepository.save(new TenantMembership(
        user.getId(), tenantA.getId(), MembershipRole.ADMIN));
    membershipRepository.save(new TenantMembership(
        user.getId(), tenantB.getId(), MembershipRole.USER));

    assertThat(membershipRepository.findByUserId(user.getId()))
        .hasSize(2)
        .extracting(TenantMembership::getTenantId)
        .containsExactlyInAnyOrder(tenantA.getId(), tenantB.getId());
  }

  @Test
  void countByTenantIdAndRole_countsOnlyAdminsOfThatTenant() {
    Tenant tenantA = persistTenant();
    Tenant tenantB = persistTenant();
    UserAccount admin = persistUser();
    UserAccount member = persistUser();
    UserAccount otherAdmin = persistUser();

    membershipRepository.save(new TenantMembership(
        admin.getId(), tenantA.getId(), MembershipRole.ADMIN));
    membershipRepository.save(new TenantMembership(
        member.getId(), tenantA.getId(), MembershipRole.USER));
    membershipRepository.save(new TenantMembership(
        otherAdmin.getId(), tenantB.getId(), MembershipRole.ADMIN));

    assertThat(membershipRepository.countByTenantIdAndRole(
        tenantA.getId(), MembershipRole.ADMIN)).isEqualTo(1);
    assertThat(membershipRepository.countByTenantIdAndRole(
        tenantA.getId(), MembershipRole.USER)).isEqualTo(1);
    assertThat(membershipRepository.countByTenantIdAndRole(
        tenantB.getId(), MembershipRole.ADMIN)).isEqualTo(1);
  }

  @Test
  void findByUserIdAndTenantId_andExists_respectTenantScope() {
    Tenant tenantA = persistTenant();
    Tenant tenantB = persistTenant();
    UserAccount user = persistUser();

    membershipRepository.save(new TenantMembership(
        user.getId(), tenantA.getId(), MembershipRole.USER));

    assertThat(membershipRepository.findByUserIdAndTenantId(user.getId(), tenantA.getId()))
        .isPresent();
    assertThat(membershipRepository.findByUserIdAndTenantId(user.getId(), tenantB.getId()))
        .isEmpty();
    assertThat(membershipRepository.existsByUserIdAndTenantId(user.getId(), tenantA.getId()))
        .isTrue();
    assertThat(membershipRepository.existsByUserIdAndTenantId(user.getId(), tenantB.getId()))
        .isFalse();
  }
}
