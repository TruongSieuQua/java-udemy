package org.example.sec11;

import org.example.utils.Util;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class Lec06SinkReplay {
	public static void main(String[] args) {

		// handle through which we would push items
		Sinks.Many<Object> sink = Sinks.many().replay().all();

		// handle through which subscribers will receive items
		Flux<Object> flux = sink.asFlux();


		sink.tryEmitNext("hi");
		sink.tryEmitNext("how are you");

		flux.subscribe(Util.subscriber("sam"));
		flux.subscribe(Util.subscriber("mike"));
		sink.tryEmitNext("?");
		flux.subscribe(Util.subscriber("jake"));

		sink.tryEmitNext("new msg");

	}
}
