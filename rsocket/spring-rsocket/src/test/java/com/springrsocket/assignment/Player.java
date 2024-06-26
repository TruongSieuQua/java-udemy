package com.springrsocket.assignment;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.function.Consumer;

public class Player {

    private final Sinks.Many<Integer> sink = Sinks.many().unicast().onBackpressureBuffer();

    private int lower = 0;
    private int upper = 100;
    private int mid = 0;
    private int attemps = 0;

    // Tao flux cho request channel
    public Flux<Integer> guesses(){
        return this.sink.asFlux();
    }

    public void play(){
        this.emit();
    }

    //
    public Consumer<GuessNumberResponse> receives(){
        return this::processResponse;
    }

    private void processResponse(GuessNumberResponse numberResponse) {
        attemps++;
        System.out.println(attemps + " : " + mid + " : " + numberResponse);

        if (GuessNumberResponse.EQUAL.equals(numberResponse)) {
            this.sink.tryEmitComplete();
            return;
        } else if (GuessNumberResponse.GREATER.equals(numberResponse)) {
            lower = mid;
        } else if (GuessNumberResponse.LESSER.equals(numberResponse)) {
            upper = mid;
        }

        this.emit();
    }


    private void emit() {
        mid = lower + (upper - lower) / 2;
        this.sink.tryEmitNext(mid);
    }
}


