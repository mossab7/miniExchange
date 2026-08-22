package dev.miniExchange.portfolio.position.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import dev.miniExchange.portfolio.position.entity.Position;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, Long> {
    List<Position> findAllByPortfolio_User_Id(Long userId);

    List<Position> findAllByPortfolio_Id(Long portfolioId);

    List<Position> findAllByPortfolio_Uuid(UUID portfolioUuid);

    Optional<Position> findByPortfolio_User_IdAndAsset_Symbol(Long userId, String symbol);

    Optional<Position> findByPortfolio_IdAndAsset_Symbol(Long portfolioId, String assetSymbol);

    Optional<Position> findByPortfolio_UuidAndAsset_Symbol(UUID portfolioUuid, String assetSymbol);

    Optional<Position> findByPortfolio_IdAndAsset_Id(Long portfolioId, Long assetId);

    Optional<Position> findByPortfolio_UuidAndAsset_Id(UUID portfolioUuid, Long assetId);

    boolean existsByAsset_IdAndPortfolio_Id(Long assetId, Long portfolioId);

    void deleteByAsset_IdAndPortfolio_Id(Long assetId, Long portfolioId);
}
