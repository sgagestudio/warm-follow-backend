package com.sgagestudio.warm_follow_backend.repository;

import com.sgagestudio.warm_follow_backend.model.Delivery;
import com.sgagestudio.warm_follow_backend.model.DeliveryStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID>, JpaSpecificationExecutor<Delivery> {
    Optional<Delivery> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    List<Delivery> findByTransaction_Id(UUID transactionId);

    Optional<Delivery> findByProviderMessageId(String providerMessageId);

    @Query("select d.transaction.id as transactionId, d.status as status, count(d) as count "
            + "from Delivery d where d.transaction.id in :transactionIds group by d.transaction.id, d.status")
    List<DeliveryStatusCountByTransaction> countByTransactionIds(@Param("transactionIds") Collection<UUID> transactionIds);

    @Query("select d.status as status, count(d) as count from Delivery d where d.transaction.id = :transactionId group by d.status")
    List<DeliveryStatusCount> countByTransactionId(@Param("transactionId") UUID transactionId);

    @Query("select distinct d.transaction.id from Delivery d where d.customer.id = :customerId")
    List<UUID> findDistinctTransactionIdsByCustomerId(@Param("customerId") UUID customerId);

    interface DeliveryStatusCount {
        DeliveryStatus getStatus();

        long getCount();
    }

    interface DeliveryStatusCountByTransaction {
        UUID getTransactionId();

        DeliveryStatus getStatus();

        long getCount();
    }
}
