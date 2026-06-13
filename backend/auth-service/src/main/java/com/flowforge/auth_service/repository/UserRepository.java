package com.flowforge.auth_service.repository;
import com.flowforge.auth_service.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
     Optional<User> findByEmail(String email);
     Optional<User> findByUsername(String email);
     boolean existsByEmail(String email);
     boolean existsByUsername(String email);

}
