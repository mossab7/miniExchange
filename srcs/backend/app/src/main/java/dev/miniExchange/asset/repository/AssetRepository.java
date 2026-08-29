package dev.miniExchange.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    @Query("""
            SELECT a.id FROM Asset a WHERE a.symbol = :symbol
            """)
    Long getIdBySymbol(@Param("symbol") String symbol);
}
