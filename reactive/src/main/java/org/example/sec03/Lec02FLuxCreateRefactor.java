package org.example.sec03;

import org.example.sec03.helper.NameProducer;
import org.example.utils.Util;
import reactor.core.publisher.Flux;

public class Lec02FLuxCreateRefactor {
	public static void main(String[] args) {
		NameProducer nameProducer = new NameProducer();

		Flux.create(nameProducer)
			.subscribe(Util.subscriber());

		Runnable runnable = nameProducer::produce;

		for (int i = 0; i < 10; i++) {
			new Thread(runnable).start();
		}

		Util.sleepSeconds(2);

	}
}
