package com.sw.api.modules.iot.repository;

import com.sw.api.modules.iot.model.InstallationTicket;
import com.sw.api.modules.iot.model.InstallationTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstallationTicketRepository extends JpaRepository<InstallationTicket, UUID> {
    List<InstallationTicket> findAllByStatusIn(List<InstallationTicketStatus> statuses);

    boolean existsByInmueble_IdAndDispositivoId(UUID inmuebleId, String dispositivoId);

    Optional<InstallationTicket> findFirstByInmueble_IdAndDispositivoIdAndStatusIn(
            UUID inmuebleId, String dispositivoId, List<InstallationTicketStatus> statuses);
}
