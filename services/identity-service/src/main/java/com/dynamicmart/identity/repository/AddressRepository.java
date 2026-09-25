package com.dynamicmart.identity.repository;

import com.dynamicmart.identity.entity.Address;
import com.dynamicmart.identity.entity.AddressStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findAllByUserIdAndStatusOrderByDefaultAddressDescUpdatedAtDesc(UUID userId, AddressStatus status);
    Optional<Address> findByIdAndUserId(UUID id, UUID userId);
}
