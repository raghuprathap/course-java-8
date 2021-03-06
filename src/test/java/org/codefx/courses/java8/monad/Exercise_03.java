package org.codefx.courses.java8.monad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Disabled
public class Exercise_03 {

	/*
	 * NOTE: This test class has a `@Disabled` annotation that you need to remove.
	 *
	 * TASK: Much like exercise 02, but this time the function already produces a
	 *       `Lazy`. Using the method from the previous exercise would give you a
	 *       `Lazy<Lazy<Resource>>`. Reduce that to one box by implementing a
	 *       corresponding method.
	 *
	 *       (Have a look at `Optional` and `Stream` for a fitting method name.)
	 *
	 *       To test your implementation, implement the two wrapper methods
	 *       `create` and `apply` below. Then you can run the tests.
	 */

	// API WRAPPER

	private static <T> Lazy<T> create(Supplier<T> generator) {
		return null;
	}

	private static <T> T getResource(Lazy<T> lazy) {
		return null;
	}

	private static <T, U> Lazy<U> apply(Lazy<T> lazy, Function<T, Lazy<U>> function) {
		return null;
	}

	// TESTS

	private static final String SUPPLIER_RESULT = "result";
	private static final Integer FUNCTION_RESULT = SUPPLIER_RESULT.length();
	private final LongAdder functionExecutions = new LongAdder();

	private Lazy<String> lazy;

	@BeforeEach
	void resetLazyAndExecutionCounter() {
		lazy = create(createSupplier());
		functionExecutions.reset();
	}

	@Test
	void applyNullFunction() {
		assertThrows(
				NullPointerException.class,
				() -> apply(lazy, null),
				"A null function should not be legal");
	}

	@Test
	void applyNullReturningFunction() {
		Lazy<Integer> mappedLazy = apply(lazy, __ -> null);

		assertThrows(
				NullPointerException.class,
				() -> getResource(mappedLazy),
				"A function that returns null should not be legal");
	}

	@Test
	void applyDoesNotExecuteFunction() {
		apply(lazy, createFunction());

		assertThat(functionExecutions)
				.describedAs("The function should not be executed during creation")
				.extracting(LongAdder::sum)
				.isEqualTo(0L);
	}

	@Test
	void getReturnsMappedResult() {
		Lazy<Integer> mappedLazy = apply(lazy, createFunction());

		Integer result = getResource(mappedLazy);

		assertThat(result)
				.describedAs("Lazy::get should return the function's result")
				.isEqualTo(FUNCTION_RESULT);
	}

	@Test
	void getExecutesFunctionOnce() {
		Lazy<Integer> mappedLazy = apply(lazy, createFunction());

		getResource(mappedLazy);

		assertThat(functionExecutions)
				.describedAs("The first call to Lazy::get should execute the function once")
				.extracting(LongAdder::sum)
				.isEqualTo(1L);
	}

	@Test
	void repeatedGetExecutesFunctionOnce() {
		Lazy<Integer> mappedLazy = apply(lazy, createFunction());

		for (int i = 0; i < 8; i++)
			getResource(mappedLazy);

		assertThat(functionExecutions)
				.describedAs("Repeatedly calling Lazy::get should not execute the function more than once")
				.extracting(LongAdder::sum)
				.isEqualTo(1L);
	}

	@Test
	void multiThreadedGetExecutesFunctionOnce() throws Exception {
		Lazy<Integer> mappedLazy = apply(lazy, createFunction());
		ExecutorService executor = Executors.newFixedThreadPool(8);
		List<Future<Integer>> futures = new ArrayList<>();

		for (int i = 0; i < 16; i++) {
			Future<Integer> getResult = executor.submit(() -> getResource(mappedLazy));
			futures.add(getResult);
		}

		for (Future<Integer> future : futures)
			future.get();

		assertThat(functionExecutions)
				.describedAs("Concurrently calling Lazy::get should not execute the function more than once")
				.extracting(LongAdder::sum)
				.isEqualTo(1L);
	}

	@ParameterizedTest
	@ValueSource(strings = { "test", "", "long string" })
	void rightIdentity(String resource) {
		// of(v).apply(v -> v)
		Lazy<String> applyIdentity = apply(create(() -> resource), v -> create(() -> v));

		assertThat(getResource(applyIdentity)).isEqualTo(resource);
	}

	@ParameterizedTest
	@CsvSource({ "test, 4", "'', 0", "long string, 11" })
	void leftIdentity(String resource, Integer resourceLength) {
		Function<String, Lazy<Integer>> length = string -> create(() -> string.length());
		// of(v).flatMap(f)
		Lazy<Integer> createThenApply = apply(create(() -> resource), length);
		// f.apply(v)
		Lazy<Integer> apply = length.apply(resource);

		assertThat(getResource(createThenApply)).isEqualTo(resourceLength);
		assertThat(getResource(apply)).isEqualTo(resourceLength);
	}

	@ParameterizedTest
	@CsvSource({ "test, 4", "'', 0", "long string, 11" })
	void associativity(String resource, Integer resourceLength) {
		Function<String, Lazy<String>> duplicate = string -> create(() -> string + string);
		Function<String, Lazy<Integer>> length = string -> create(() -> string.length());

		// of(v).flatMap(f).flatMap(g)
		Lazy<Integer> mappedIndividually = apply(apply(create(() -> resource), duplicate), length);
		// of(v).flatMap(f.andThen(g))
		Lazy<Integer> mappedComposed = apply(create(() -> resource), v -> apply(duplicate.apply(v), length));

		assertThat(getResource(mappedIndividually)).isEqualTo(resourceLength * 2);
		assertThat(getResource(mappedComposed)).isEqualTo(resourceLength * 2);
	}

	private static Supplier<String> createSupplier() {
		return () -> SUPPLIER_RESULT;
	}

	private Function<String, Lazy<Integer>> createFunction() {
		return string -> {
			functionExecutions.increment();
			return create(() -> string.length());
		};
	}

}
