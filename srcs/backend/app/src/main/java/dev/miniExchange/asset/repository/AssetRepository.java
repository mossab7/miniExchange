package dev.miniExchange.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import dev.miniExchange.asset.entity.Asset;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    boolean existsBySymbol(String symbol);
    Optional<Asset> findBySymbol(String symbol);
    Optional<Asset> findByUuid(UUID uuid);
    List<Asset> findByIsActiveTrue();
    List<Asset> findByIsActiveFalse();
}
