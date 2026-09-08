package com.ubidict.backend.member.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.member.domain.Member;
import com.ubidict.backend.member.domain.MemberStatus;
import com.ubidict.backend.member.domain.OAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Flyway 마이그레이션이 아직 이 테스트 슬라이스에 연결되지 않아 스키마는 Hibernate가
 * 생성한다({@code BaseEntityAuditingTest}와 동일한 관례).
 */
@Import(MemberRepositoryTest.MySqlContainerConfiguration.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"})
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TestEntityManager em;

    @DisplayName("이메일이 같은 회원을 두 번 저장하면 실패한다.")
    @Test
    void save_duplicateEmail() {
        // given
        // Spring Data 프록시(memberRepository)를 거쳐야 Hibernate 예외가
        // DataIntegrityViolationException으로 변환된다. em(TestEntityManager)로 직접
        // persist하면 변환 없이 원본 ConstraintViolationException이 그대로 올라온다.
        memberRepository.saveAndFlush(Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1"));

        // when & then
        assertThatThrownBy(() -> memberRepository.saveAndFlush(
                        Member.create("member@example.com", "member2", OAuthProvider.GOOGLE, "google-2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("같은 provider와 providerId를 가진 회원을 두 번 저장하면 실패한다.")
    @Test
    void save_duplicateProviderAccount() {
        // given
        memberRepository.saveAndFlush(
                Member.create("member1@example.com", "member1", OAuthProvider.GOOGLE, "google-1"));

        // when & then
        assertThatThrownBy(() -> memberRepository.saveAndFlush(
                        Member.create("member2@example.com", "member2", OAuthProvider.GOOGLE, "google-1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("provider와 providerId로 회원을 조회할 수 있다.")
    @Test
    void findByProviderAndProviderId() {
        // given
        Member member =
                em.persistFlushFind(Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1"));

        // when
        var found = memberRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-1");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(member.getId());
    }

    @DisplayName("등록된 이메일이면 existsByEmail이 true를 반환한다.")
    @Test
    void existsByEmail() {
        // given
        em.persistAndFlush(Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1"));

        // when & then
        assertThat(memberRepository.existsByEmail("member@example.com")).isTrue();
        assertThat(memberRepository.existsByEmail("other@example.com")).isFalse();
    }

    @DisplayName("탈퇴하면 상태와 삭제 시각이 함께 저장된다.")
    @Test
    void withdraw() {
        // given
        Member member =
                em.persistFlushFind(Member.create("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1"));

        // when
        member.withdraw();
        em.flush();
        em.clear();

        // then
        Member found = em.find(Member.class, member.getId());
        assertThat(found.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(found.isDeleted()).isTrue();
    }

    /**
     * 컨테이너를 Spring 빈으로 두어 컨텍스트 종료 시점과 컨테이너 종료 시점을 맞춘다.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class MySqlContainerConfiguration {

        @Bean
        @ServiceConnection
        MySQLContainer mysqlContainer() {
            return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
        }
    }
}
