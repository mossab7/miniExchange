package dev.miniExchange.asset.controller;

import dev.miniExchange.asset.service.AssetService;
import dev.miniExchange.asset.entity.Asset;
import dev.miniExchange.asset.dto.CreateAssetRequest;
import dev.miniExchange.asset.dto.UpdateAssetRequest;
import dev.miniExchange.asset.dto.AssetResponse;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.Valid;

import java.util.List;
import java.net.URI;

@RestController
@RequestMapping("/assets")
public class AssetController 
{
    private final AssetService assetService;
    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }
    @PostMapping
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody CreateAssetRequest request) {
        AssetResponse asset = assetService.create(request);
        URI location = URI.create(String.format("/assets/%s", asset.symbol()));
        return ResponseEntity.created(location).body(asset);
    }
    @GetMapping
    public ResponseEntity<List<AssetResponse>> getAll() {
        return ResponseEntity.ok(assetService.getAll());
    }
    @GetMapping("/{symbol}")
    public ResponseEntity<AssetResponse> getBySymbol(@PathVariable String symbol) {
        return ResponseEntity.ok(assetService.getBySymbol(symbol));
    }
    @PostMapping("/{symbol}")
    public ResponseEntity<AssetResponse> update(@PathVariable String symbol, @Valid @RequestBody UpdateAssetRequest request) {
        AssetResponse updatedAsset = assetService.update(symbol, request);
        return ResponseEntity.ok(updatedAsset);
    }
}
