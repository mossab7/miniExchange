package dev.miniExchange.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import dev.miniExchange.asset.entity.Asset;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    boolean existsBySymbol(String symbol);
    Asset findBySymbol(String symbol);
    List<Asset> findByIsActiveTrue();
    List<Asset> findByIsActiveFalse();
}
