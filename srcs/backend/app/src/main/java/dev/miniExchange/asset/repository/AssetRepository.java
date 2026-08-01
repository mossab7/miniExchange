package dev.miniExchange.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import dev.miniExchange.asset.entity.Asset;
import java.util.Optional;


public interface AssetRepository extends JpaRepository<Asset, Long> {
    boolean existsBySymbol(String symbol);
    Optional<Asset> findBySymbol(String symbol);
    List<Asset> findByIsActiveTrue();
    List<Asset> findByIsActiveFalse();
}
