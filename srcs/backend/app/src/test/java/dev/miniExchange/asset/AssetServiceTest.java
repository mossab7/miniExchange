package dev.miniExchange.asset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import dev.miniExchange.asset.dto.CreateAssetRequest;
import dev.miniExchange.asset.dto.UpdateAssetRequest;
import dev.miniExchange.asset.entity.Asset;
import dev.miniExchange.asset.service.AssetService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AssetServiceTest {

    @Autowired
    private AssetService assetService;

    @Test
    void testCreateAndUpdateAsset() {
        CreateAssetRequest createReq = new CreateAssetRequest("BTC", "Bitcoin", 8, true);
        Asset created = assetService.create(createReq);
        assertNotNull(created);
        assertEquals("BTC", created.getSymbol());
        assertEquals("Bitcoin", created.getName());
        assertEquals(8, created.getDecimalPlaces());

        // Update asset
        UpdateAssetRequest updateReq = new UpdateAssetRequest("BTC", "Bitcoin Core", 8, true);
        Asset updated = assetService.update("BTC", updateReq);

        assertEquals(created.getId(), updated.getId());
        assertEquals("Bitcoin Core", updated.getName());
    }
}
