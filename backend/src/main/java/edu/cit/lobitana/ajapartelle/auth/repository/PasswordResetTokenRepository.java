package edu.cit.lobitana.ajapartelle.auth.repository;

import edu.cit.lobitana.ajapartelle.auth.entity.PasswordResetToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    long countByUserIdAndCreatedAtAfter(Long userId, OffsetDateTime since);

    @Query("select t.userId from PasswordResetToken t where t.tokenHash = :hash")
    Optional<Long> findUserIdByHash(String hash);

    /** Returns 1 only for the one caller that uses a still-valid token (single use). */
    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = :now "
            + "where t.tokenHash = :hash and t.usedAt is null and t.expiresAt > :now")
    int markUsed(String hash, OffsetDateTime now);

    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = :now where t.userId = :userId and t.usedAt is null")
    int invalidateUnused(Long userId, OffsetDateTime now);

    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :cutoff")
    int deleteExpiredBefore(OffsetDateTime cutoff);
}
