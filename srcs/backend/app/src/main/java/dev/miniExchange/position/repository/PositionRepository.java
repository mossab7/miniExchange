package dev.miniExchange.position.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import dev.miniExchange.position.entity.Position;

import java.util.List;
import java.util.UUID;
public interface PositionRepository extends JpaRepository<Position, Long> {
    Optional<Position> findByAssetIdAndPortfolioId(Long assetId, Long portfolioId);
    void deleteByAssetIdAndPortfolioId(Long assetId, Long portfolioId);
    boolean existsByAssetIdAndPortfolioId(Long assetId, Long portfolioId);
    List<Position> findAllByPortfolioId(Long portfolioId);
    Optional<Position> findByPortfolioUserIdAndAssetSymbol(Long userId, String symbol);
    List<Position> findByPortfolioUserId(Long userId);
    Optional<Position> findByPortfolioIdAndAssetSymbol(Long portfolioId, String assetSymbol);
    Optional<Position> findByPortfolioIdAndAssetSymbol(UUID portfolioId, String assetSymbol);

}
