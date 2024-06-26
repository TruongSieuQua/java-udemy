package com.tjn.service;

import com.tjn.config.ServiceUrlConfig;
import com.tjn.dto.ForestResponse;
import com.tjn.dto.SensorResponse;
import com.tjn.dto.SprinklerDto;
import com.tjn.dto.UpdateForestStateDto;
import com.tjn.mapper.SprinklerMapper;
import com.tjn.model.Sprinkler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SprinklerService {
    private Map<Integer, Sprinkler> db;

    private static final Logger LOGGER = LoggerFactory.getLogger(SprinklerService.class);

    private final WebClient webClient;

    private final SprinklerJsonProducer sprinklerJsonProducer;

    private final ServiceUrlConfig serviceUrlConfig;

    private final SprinklerMapper sprinklerMapper;

    private final Sinks.Many<SensorResponse> sensorTemperatureSink = Sinks.many().replay().latest();

    private final Sinks.Many<SprinklerDto> sprinklerEventSink = Sinks.many().replay().latest();

    @PostConstruct
    private void init() {
        this.db = Map.of(
                1, new Sprinkler(1, "f1", false, 30d, 100d),
                2, new Sprinkler(2, "f2", false, 30d, 100d),
                3, new Sprinkler(3, "f3", false, 30d, 100d)
        );
        // publish
        autoUpdateSprinklerBaseOnTemperature();
        // consumer
        changeForestState();
    }

    private Sprinkler findSpringkerByForestName(String forestName) {
        Optional<Sprinkler> sprinkler = db.values().stream()
                .filter(s -> s.getForestName().equals(forestName))
                .findFirst();
        return sprinkler.orElse(null);
    }

    private boolean shouldUpdateSprinkler(SprinklerDto dto, Sprinkler sprinkler){
        return sprinkler.getState() != dto.state();
    }

    @RabbitListener(queues = {"${rabbitmq.queue.sensorTemperature.name}"})
    private void consumeSensorTemperatureMessage(SensorResponse res) {
        sensorTemperatureSink.tryEmitNext(res);
    }

//    @RabbitListener(queues = {"${rabbitmq.queue.actuatorState.name}"})
//    private void consumeSprinklerMessage(SprinklerDto res){
//        System.out.println("Receive Message: " + res);
//        sprinklerEventSink.tryEmitNext(res);
//    }

    private void autoUpdateSprinklerBaseOnTemperature() {
        sensorTemperatureSink
                .asFlux()
                .doOnNext((sr) -> {
                    Sprinkler sprinkler = this.findSpringkerByForestName(sr.forestName());

                    Sprinkler updatedSprinkler = sprinkler.makeCopy();

                    if (sr.temperature() > updatedSprinkler.getThreshold()) {
                        updatedSprinkler.setState(true);
                    } else if (sr.temperature() < updatedSprinkler.getCutOffThreshold()) {
                        updatedSprinkler.setState(false);
                    }
                    if(sprinkler.getState() != updatedSprinkler.getState()){
                        updateSprinkler(sprinkler.getId(), sprinklerMapper.toSprinklerDto(updatedSprinkler)).subscribe();
                    }
                }).subscribe();
    }

    private void changeForestState() {
        sprinklerEventSink
                .asFlux()
                .doOnNext(s -> {
                    System.out.println("changeForestState is called");
                    var updateForestDto = new UpdateForestStateDto(s.state() ? "extinguish" : "normal");
                    URI url = UriComponentsBuilder
                            .fromHttpUrl(serviceUrlConfig.forest())
                            .path("/forests/{forestName}")
                            .buildAndExpand(s.forestName())
                            .toUri();
                    webClient.post()
                            .uri(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(updateForestDto)
                            .retrieve()
                            .bodyToMono(ForestResponse.class)
                            .retry(3)
                            .doOnError(e -> {
                                System.err.println("Error during WebClient call after retries: " + e.getMessage());
                            }).subscribe();
                })
                .subscribe();
    }

    public Flux<SprinklerDto> sprinklerStream(){
        return sprinklerEventSink.asFlux();
    }

    public Mono<SprinklerDto> updateSprinkler(Integer id, SprinklerDto dto) {
        return Mono.fromSupplier(() -> this.db.get(id))
                .subscribeOn(Schedulers.boundedElastic()) // Offload blocking call to boundedElastic scheduler
                .flatMap(sprinkler -> {
                    if (shouldUpdateSprinkler(dto, sprinkler)) {
                        System.out.println("Update Sprinkler: " + dto);
                        sprinklerMapper.updateSprinklerFromDto(dto, sprinkler);
                    }
                    return Mono.just(sprinkler);
                })
                .map(sprinklerMapper::toSprinklerDto)
                .doOnNext(sprinklerJsonProducer::sendJsonMessage)
                .doOnNext(sprinklerEventSink::tryEmitNext);
    }

    public Mono<SprinklerDto> getSprinkler(Integer id){
        return Mono.fromSupplier(() -> db.get(id))
                .flatMap((s)->{
                    if(s == null){
                        return Mono.error(new RuntimeException("Sprinklers id = " + id + " is not found!"));
                    }
                    return Mono.just(sprinklerMapper.toSprinklerDto(s));
                });
    }

    public Mono<List<SprinklerDto>> getAllSprinklers(){
        return Mono.fromSupplier(() -> db.values().stream().map(sprinklerMapper::toSprinklerDto).collect(Collectors.toList()));
    }
}
