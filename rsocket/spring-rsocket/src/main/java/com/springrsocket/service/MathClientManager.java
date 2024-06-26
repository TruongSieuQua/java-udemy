package com.springrsocket.service;

import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class MathClientManager {

    private Set<RSocketRequester> set = Collections.synchronizedSet(new HashSet<>());

    public void add(RSocketRequester rSocketRequester){
        rSocketRequester.rsocket()
                .onClose()
                .doFirst(() -> this.set.add(rSocketRequester))
                .doFinally(s -> {
                    System.out.println("MathClientManager-add doFinally");
                    this.set.remove(rSocketRequester);
                }).subscribe();
    }

//    @Scheduled(fixedRate = 1000)
//    public void print(){
//        System.out.println(set);
//    }

    public void notify(int i){
        Flux.fromIterable(set)
                .flatMap(r -> r.route("math.updates").data(i).send())
                .subscribe();
    }
}
