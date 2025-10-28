package com.pkg.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CallStepChainTest {

    @Test
    @DisplayName("startWith - should create chain with single step")
    void startWith_singleStep() {
        // Given
        CallStep<String, Integer> step = String::length;

        // When
        CallStepChain<String, Integer> chain = CallStepChain.startWith(step);

        // Then
        assertThat(chain).isNotNull();
        CallStep<String, Integer> result = chain.toCallStep();
        assertThat(result.operate("hello")).isEqualTo(5);
    }

    @Test
    @DisplayName("add - should chain two steps together")
    void add_twoSteps() {
        // Given
        CallStep<String, Integer> step1 = String::length;
        CallStep<Integer, String> step2 = num -> "Length: " + num;

        // When
        CallStepChain<String, String> chain = CallStepChain.startWith(step1)
                .add(step2);

        // Then
        CallStep<String, String> composedStep = chain.toCallStep();
        assertThat(composedStep.operate("hello")).isEqualTo("Length: 5");
    }

    @Test
    @DisplayName("add - should chain multiple steps together")
    void add_multipleSteps() {
        // Given
        CallStep<String, Integer> step1 = String::length;
        CallStep<Integer, Integer> step2 = num -> num * 2;
        CallStep<Integer, String> step3 = num -> "Result: " + num;

        // When
        CallStepChain<String, String> chain = CallStepChain.startWith(step1)
                .add(step2)
                .add(step3);

        // Then
        CallStep<String, String> composedStep = chain.toCallStep();
        assertThat(composedStep.operate("test")).isEqualTo("Result: 8");
    }

    @Test
    @DisplayName("add - should maintain correct type chain")
    void add_typeChaining() {
        // Given
        CallStep<Integer, String> step1 = Object::toString;
        CallStep<String, Integer> step2 = Integer::parseInt;
        CallStep<Integer, Boolean> step3 = num -> num > 50;

        // When
        CallStepChain<Integer, Boolean> chain = CallStepChain.startWith(step1)
                .add(step2)
                .add(step3);

        // Then
        CallStep<Integer, Boolean> composedStep = chain.toCallStep();
        assertThat(composedStep.operate(100)).isTrue();
        assertThat(composedStep.operate(30)).isFalse();
    }

    @Test
    @DisplayName("toCallStep - should return composed step that can be reused")
    void toCallStep_reusable() {
        // Given
        CallStep<String, Integer> step1 = String::length;
        CallStep<Integer, Integer> step2 = num -> num * 3;
        CallStepChain<String, Integer> chain = CallStepChain.startWith(step1)
                .add(step2);

        // When
        CallStep<String, Integer> composedStep = chain.toCallStep();

        // Then - should be reusable multiple times
        assertThat(composedStep.operate("a")).isEqualTo(3);
        assertThat(composedStep.operate("ab")).isEqualTo(6);
        assertThat(composedStep.operate("abc")).isEqualTo(9);
    }

    @Test
    @DisplayName("add - should handle complex object transformations")
    void add_complexObjectTransformation() {
        // Given
        CallStep<String, Person> step1 = name -> new Person(name, 0);
        CallStep<Person, Person> step2 = person -> new Person(person.name(), 25);
        CallStep<Person, String> step3 = person -> person.name() + " is " + person.age();

        // When
        CallStepChain<String, String> chain = CallStepChain.startWith(step1)
                .add(step2)
                .add(step3);

        // Then
        CallStep<String, String> composedStep = chain.toCallStep();
        assertThat(composedStep.operate("Alice")).isEqualTo("Alice is 25");
        assertThat(composedStep.operate("Bob")).isEqualTo("Bob is 25");
    }

    @Test
    @DisplayName("add - should handle identity transformation")
    void add_identityTransformation() {
        // Given
        CallStep<String, String> step1 = s -> s.toUpperCase();
        CallStep<String, String> step2 = s -> s; // identity

        // When
        CallStepChain<String, String> chain = CallStepChain.startWith(step1)
                .add(step2);

        // Then
        CallStep<String, String> composedStep = chain.toCallStep();
        assertThat(composedStep.operate("hello")).isEqualTo("HELLO");
    }

    @Test
    @DisplayName("startWith and add - should create linear pipeline")
    void startWithAndAdd_linearPipeline() {
        // Given - create a data processing pipeline
        CallStep<String, String> trim = String::trim;
        CallStep<String, String> toLowerCase = String::toLowerCase;
        CallStep<String, String> removeSpaces = s -> s.replace(" ", "");
        CallStep<String, Integer> getLength = String::length;

        // When
        CallStepChain<String, Integer> chain = CallStepChain.startWith(trim)
                .add(toLowerCase)
                .add(removeSpaces)
                .add(getLength);

        // Then
        CallStep<String, Integer> pipeline = chain.toCallStep();
        assertThat(pipeline.operate("  Hello World  ")).isEqualTo(10);
        assertThat(pipeline.operate("  JAVA  ")).isEqualTo(4);
    }

    @Test
    @DisplayName("add - should work with different numeric types")
    void add_numericTypeConversions() {
        // Given
        CallStep<Integer, Long> step1 = Integer::longValue;
        CallStep<Long, Double> step2 = Long::doubleValue;
        CallStep<Double, String> step3 = d -> String.format("%.2f", d);

        // When
        CallStepChain<Integer, String> chain = CallStepChain.startWith(step1)
                .add(step2)
                .add(step3);

        // Then
        CallStep<Integer, String> composedStep = chain.toCallStep();
        assertThat(composedStep.operate(42)).isEqualTo("42.00");
    }

    @Test
    @DisplayName("add - should maintain execution order")
    void add_executionOrder() {
        // Given
        StringBuilder executionLog = new StringBuilder();
        CallStep<String, String> step1 = s -> {
            executionLog.append("1");
            return s + "A";
        };
        CallStep<String, String> step2 = s -> {
            executionLog.append("2");
            return s + "B";
        };
        CallStep<String, String> step3 = s -> {
            executionLog.append("3");
            return s + "C";
        };

        // When
        CallStepChain<String, String> chain = CallStepChain.startWith(step1)
                .add(step2)
                .add(step3);

        // Then
        CallStep<String, String> composedStep = chain.toCallStep();
        String result = composedStep.operate("Start:");
        assertThat(result).isEqualTo("Start:ABC");
        assertThat(executionLog.toString()).isEqualTo("123");
    }

    // Helper record for testing
    record Person(String name, int age) {}
}
