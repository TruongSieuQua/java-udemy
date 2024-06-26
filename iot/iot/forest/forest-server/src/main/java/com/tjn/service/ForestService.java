package com.tjn.service;

import com.tjn.dto.ForestDto;
import com.tjn.dto.ForestResponse;
import com.tjn.dto.UpdateForestStateDto;
import com.tjn.mapper.ForestMapper;
import com.tjn.model.Forest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForestService {

    private final ForestMapper forestMapper;

    private Map<String, Forest> db;

    private final Sinks.Many<Forest> forestSink = Sinks.many().replay().latest();

    @PostConstruct
    public void init() {
        Forest one = new Forest("f1", 30);
        Forest two = new Forest("f2", 20);
        Forest three = new Forest("f3", 40);
        this.db = Map.of(
                one.getName(), one,
                two.getName(), two,
                three.getName(), three
        );
    }

    public Flux<ForestResponse> getTemperatureStream(String forestName) {
        Forest forest = this.db.get(forestName);
        if (forest == null) {
            return Flux.error(new IllegalArgumentException("Forest not found: " + forestName));
        }
        return forest.temperatureStream()
                .map(temp -> new ForestResponse(forest.getName(), forest.getTemperature()))
                .onErrorResume(e -> {
                    System.err.println("Error occurred: " + e.getMessage());
                    return Flux.empty();
                });
    }

    public Mono<List<ForestDto>> getAllForest(){
        return Mono.just(db.values().stream()
                .map(forestMapper::toForestDto)
                .collect(Collectors.toList()));
    }

//    public Mono<ForestDto> changeState(String forestName, UpdateForestStateDto req) {
//        return Mono.fromSupplier(() -> {
//            Forest forest = this.db.get(forestName);
//            if (forest == null) {
//                throw new IllegalArgumentException("Forest not found: " + forestName);
//            }
//            System.out.printf("\n\n%s\n\n", "Receive "+ req);
//            forest.changeState(req.state());
//            return forestMapper.toForestDto(forest);
//        });
//    }

    public Mono<ForestDto> changeState(String forestName, UpdateForestStateDto req) {
        return Mono.fromSupplier(() -> this.db.get(forestName))
                .flatMap(forest -> {
                    if(forest == null){
                        return Mono.error(new IllegalArgumentException("Forest not found: " + forestName));
                    }
                    if (!forest.getState().equals(req.state())){
                        forest.changeState(req.state());
                        forestSink.tryEmitNext(forest);
                    }
                    return Mono.just(forest);
                })
                .map(forestMapper::toForestDto);
    }

    public Flux<ForestDto> forestStream(){
        return forestSink.asFlux().map(forestMapper::toForestDto);
    }
}
