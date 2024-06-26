package com.tjn.stock.controller;

import com.tjn.stock.dto.StockTickDto;
import com.tjn.stock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
public class StockController {

    @Autowired
    private StockService stockService;

    @MessageMapping("stock.ticks")
    public Flux<StockTickDto> stockPrice(){
        return this.stockService.getStockPrice();
    }

}
