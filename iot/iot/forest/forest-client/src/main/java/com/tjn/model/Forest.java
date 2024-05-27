package com.tjn.model;

import lombok.Data;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.util.Random;

@Data
public class Forest {
    private String name;
    private String state;
    private Double temperature;
    private double baseTemperature;

    private final Random random = new Random();

    public Forest(String name, double baseTemperature) {
        this.name = name;
        this.state = "normal";  // Default state is 'normal'
        this.baseTemperature = baseTemperature;
        this.temperature = baseTemperature;
    }

    public void changeState(String newState) {
        if (newState.equals("normal") || newState.equals("fired") || newState.equals("extinguish")) {
            this.state = newState;
        } else {
            throw new IllegalArgumentException("Invalid state. State must be 'normal', 'fired', or 'extinguish'.");
        }
    }

    public Flux<Double> temperatureStream() {
        return Flux.create(emitter -> {
            while (true) {
                double newTemperature = calculateTemperature();
                emitter.next(newTemperature);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    emitter.error(e);
                    break;
                }
            }
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    private double calculateTemperature() {
        switch (state) {
            case "normal":
                temperature = baseTemperature + random.nextDouble() * 4 - 2;
                break;
            case "fired":
                temperature += random.nextDouble() * 3 + 1;
                break;
            case "extinguish":
                temperature -= random.nextDouble() * 3 + 1;
                if (temperature < baseTemperature - 2) {
                    temperature = baseTemperature - 2;
                } else if (temperature > baseTemperature + 2) {
                    temperature = baseTemperature + 2;
                }
                break;
        }
        return temperature;
    }

    public static void main(String[] args) throws InterruptedException {
        Forest forest = new Forest("Amazon", 25.0);
        forest.setState("fired");
        forest.temperatureStream()
                .doOnNext(temp -> System.out.println("Forest State: " + forest.state + ", Temperature: " + temp))
                .subscribe();

       Flux.just(0)
               .delayElements(Duration.ofSeconds(5000))
               .doOnComplete(()-> forest.setState("extinguish")).subscribe();
    }
}