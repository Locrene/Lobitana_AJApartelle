package edu.cit.lobitana.ajapartelle.repository;

import edu.cit.lobitana.ajapartelle.entity.Role;
import edu.cit.lobitana.ajapartelle.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByRole(Role role);
    long countByRole(Role role);
    long countByRoleAndActive(Role role, boolean active);
    List<User> findByRoleOrderByFullNameAsc(Role role);
}
