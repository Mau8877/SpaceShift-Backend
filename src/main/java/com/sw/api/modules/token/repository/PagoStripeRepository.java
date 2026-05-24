package com.sw.api.modules.token.repository;

import com.sw.api.modules.token.model.PagoStripe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagoStripeRepository extends JpaRepository<PagoStripe, UUID> {
    Optional<PagoStripe> findByStripeSessionId(String stripeSessionId);
    boolean existsByStripeSessionId(String stripeSessionId);
}
