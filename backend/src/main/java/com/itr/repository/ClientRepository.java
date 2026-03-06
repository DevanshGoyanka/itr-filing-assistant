package com.itr.repository;

import com.itr.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findByUserId(Long userId);

    @Query("SELECT c FROM Client c WHERE c.id = :id AND c.user.id = :userId")
    Optional<Client> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    boolean existsByPanAndUserId(String pan, Long userId);
}
