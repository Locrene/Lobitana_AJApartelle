package edu.cit.lobitana.ajapartelle.user.repository;

import edu.cit.lobitana.ajapartelle.user.entity.Role;
import edu.cit.lobitana.ajapartelle.user.entity.User;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByRole(Role role);
    long countByRole(Role role);
    long countByRoleAndActive(Role role, boolean active);
    List<User> findByRoleOrderByFullNameAsc(Role role);

    /** Only the id, so that the row can then be read fresh under a lock. */
    @Query("select u.id from User u where lower(u.email) = lower(:email)")
    Optional<Long> findIdByEmail(String email);

    /**
     * Every update of a user must read through this lock. Hibernate writes all columns, so an unlocked
     * profile save could otherwise restore a password or token version that was changed at the same moment.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(Long id);
}
