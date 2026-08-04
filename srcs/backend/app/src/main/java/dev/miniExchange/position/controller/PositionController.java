package dev.miniExchange.position.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import dev.miniExchange.position.service.PositionService;
import dev.miniExchange.position.dto.PositionResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/positions")
public class PositionController {
    public final PositionService positionService;
    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }
    @GetMapping("/{symbol}")
    public ResponseEntity<PositionResponse> getPosition(@PathVariable String symbol) {
        PositionResponse position = positionService.getPosition(symbol);
        return ResponseEntity.ok(position);
    }
    @GetMapping
    public ResponseEntity<List<PositionResponse>> getAllPositions() {
        List<PositionResponse> positions = positionService.getAllPositions();
        return ResponseEntity.ok(positions);
    }
}
