package dev.miniExchange.position.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import dev.miniExchange.position.entity.Position;

import java.util.List;

public interface PositionRepository extends JpaRepository<Position, Long> {
    Optional<Position> findByAssetIdAndPortfolioId(Long assetId, Long portfolioId);
    void deleteByAssetIdAndPortfolioId(Long assetId, Long portfolioId);
    void updateQuantityByAssetIdAndPortfolioId(Long assetId, Long portfolioId, java.math.BigInteger quantity);
    boolean existsByAssetIdAndPortfolioId(Long assetId, Long portfolioId);
    List<Position> findAllByPortfolioId(Long portfolioId);
    Optional<Position> findByPortfolioUserIdAndAssetSymbol(Long userId, String symbol);
    List<Position> findByPortfolioUserId(Long userId);
}
