package com.example.paymentapi.payment;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {
	private final AtomicBoolean available = new AtomicBoolean(true);

	@PostMapping(path = "/")
	public ResponseEntity<Void> payment(@RequestBody JsonNode body) throws InterruptedException {
		if (!available.get()) {
			Thread.sleep(1000);
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
		}
		if (!body.has("price")) {
			return ResponseEntity.badRequest().build();
		}
		final BigDecimal price = body.get("price").decimalValue();
		if (price.compareTo(BigDecimal.valueOf(100)) > 0) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		else {
			return ResponseEntity.status(HttpStatus.ACCEPTED).build();
		}
	}

	@PatchMapping(path = "/disable")
	public boolean disable() {
		this.available.set(false);
		return this.available.get();
	}

	@PatchMapping(path = "/enable")
	public boolean enable() {
		this.available.set(true);
		return this.available.get();
	}
}
