package com.stater.statemachine.java;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class TestDoor {
    private enum States {
        CLOSE, AJAR, OPEN
    }

    private static class DoorFSMContext implements Context {
        private Integer degreeOfOpening = 100;

        DoorFSMContext() {
        }

        DoorFSMContext(Integer degreeOfOpening) {
            this.degreeOfOpening = degreeOfOpening;
        }

        public Integer getDegreeOfOpening() {
            return degreeOfOpening;
        }

        public void setDegreeOfOpening(int degreeOfOpening) {
            this.degreeOfOpening = degreeOfOpening;
        }
    }

    private static class TypesDoorStateMachine extends StaterStateMachine<States, DoorFSMContext> {
        public TypesDoorStateMachine() {
            super(List.of(new Transition<>("preOpen", States.CLOSE, States.AJAR, ctx -> true, ctx -> ctx.setDegreeOfOpening(1)), new Transition<>("preClose", States.OPEN, States.AJAR, ctx -> true, ctx -> ctx.setDegreeOfOpening(99)), new Transition<>("open", States.AJAR, States.OPEN, ctx -> ctx.getDegreeOfOpening() >= 99, ctx -> ctx.setDegreeOfOpening(100)), new Transition<>("close", States.AJAR, States.CLOSE, ctx -> ctx.getDegreeOfOpening() <= 1, ctx -> ctx.setDegreeOfOpening(0)), new Transition<>("ajarPlus", States.AJAR, States.AJAR, ctx -> ctx.getDegreeOfOpening() >= 1 && ctx.getDegreeOfOpening() <= 98, ctx -> ctx.setDegreeOfOpening(ctx.getDegreeOfOpening() + 1)), new Transition<>("ajarMinus", States.AJAR, States.AJAR, ctx -> ctx.getDegreeOfOpening() >= 2 && ctx.getDegreeOfOpening() <= 99, ctx -> ctx.setDegreeOfOpening(ctx.getDegreeOfOpening() - 1))), new DoorFSMContext(), States.OPEN, Set.of(States.OPEN, States.CLOSE, States.AJAR), new HashMap<>(), new ArrayList<>(), new HashMap<>(), new ArrayList<>(), new HashMap<>(), new ArrayList<>(), null);
        }

        public TypesDoorStateMachine(List<Transition<States, DoorFSMContext>> transitions, DoorFSMContext context, States startState, Set<States> states, Map<String, List<TransitionMiddleware<DoorFSMContext>>> transitionMiddlewares, List<TransitionNameMiddleware<DoorFSMContext>> transitionAllMiddlewares, Map<String, List<Event<DoorFSMContext>>> transitionCallbacks, List<NameEvent<DoorFSMContext>> transitionAllCallbacks, Map<States, List<Event<DoorFSMContext>>> stateCallbacks, List<StateEvent<States, DoorFSMContext>> stateAllCallbacks, ContextJsonAdapter<DoorFSMContext> contextJsonAdapter) {
            super(transitions, context, startState, states, transitionMiddlewares, transitionAllMiddlewares, transitionCallbacks, transitionAllCallbacks, stateCallbacks, stateAllCallbacks, contextJsonAdapter);
        }

        public void ajarPlus() {
            this.transition("ajarPlus");
        }

        public void ajarMinus() {
            transition("ajarMinus");
        }

        public void preOpen() {
            transition("preOpen");
        }

        public void preClose() {
            transition("preClose");
        }

        public void open() {
            transition("open");
        }

        public void close() {
            transition("close");
        }
    }

    private final StateMachineFactory<States, DoorFSMContext> typedDoorFactory = TypesDoorStateMachine::new;


    private void testDoor(StaterStateMachine<States, DoorFSMContext> door) {
        assertEquals(door.getState(), States.OPEN);
        assertEquals(door.getContext().degreeOfOpening, 100);
        door.transition("preClose");
        assertEquals(door.getState(), States.AJAR);
        assertEquals(door.getContext().degreeOfOpening, 99);
        while (door.getContext().degreeOfOpening > 1) {
            door.transition("ajarMinus");
            assertEquals(door.getState(), States.AJAR);
        }
        assertEquals(door.getContext().degreeOfOpening, 1);
        door.transition("close");
        assertEquals(door.getContext().degreeOfOpening, 0);
        assertEquals(door.getState(), States.CLOSE);
        door.transition("preOpen");
        assertEquals(door.getContext().degreeOfOpening, 1);
        assertEquals(door.getState(), States.AJAR);
        door.transition("ajarPlus");
        assertEquals(door.getState(), States.AJAR);
        assertEquals(door.getContext().degreeOfOpening, 2);
        while (door.getContext().degreeOfOpening < 99) {
            door.transition("ajarPlus");
            assertEquals(door.getState(), States.AJAR);
        }
        door.transition("open");
        assertEquals(door.getState(), States.OPEN);
        assertEquals(door.getContext().degreeOfOpening, 100);
    }

    private void typedTestDoor(TypesDoorStateMachine door) {
        assertEquals(door.getState(), States.OPEN);
        assertEquals(door.getContext().degreeOfOpening, 100);
        door.preClose();
        assertEquals(door.getState(), States.AJAR);
        assertEquals(door.getContext().degreeOfOpening, 99);
        while (door.getContext().degreeOfOpening > 1) {
            door.ajarMinus();
            assertEquals(door.getState(), States.AJAR);
        }
        assertEquals(door.getContext().degreeOfOpening, 1);
        door.close();
        assertEquals(door.getContext().degreeOfOpening, 0);
        assertEquals(door.getState(), States.CLOSE);
        door.preOpen();
        assertEquals(door.getContext().degreeOfOpening, 1);
        assertEquals(door.getState(), States.AJAR);
        door.ajarPlus();
        assertEquals(door.getState(), States.AJAR);
        assertEquals(door.getContext().degreeOfOpening, 2);
        while (door.getContext().degreeOfOpening < 99) {
            door.ajarPlus();
            assertEquals(door.getState(), States.AJAR);
        }
        door.open();
        assertEquals(door.getState(), States.OPEN);
        assertEquals(door.getContext().degreeOfOpening, 100);
    }

    @Test
    public void testSimpleBuild() {
        final TypesDoorStateMachine doorFSM = new TypesDoorStateMachine();
        testDoor(doorFSM);
        typedTestDoor(doorFSM);
    }

    @Test
    public void testBuilder() {
        StaterStateMachine<States, DoorFSMContext> doorFSM = new StaterStateMachineBuilder<States, DoorFSMContext>()
                .addTransition("preOpen", States.CLOSE, States.AJAR, ctx -> true, context -> context.degreeOfOpening = 1)
                .addTransition("preClose", States.OPEN, States.AJAR, ctx -> true, context -> context.degreeOfOpening = 99)
                .addTransition("open", States.AJAR, States.OPEN, ctx -> ctx.degreeOfOpening >= 99, ctx -> ctx.degreeOfOpening = 100)
                .addTransition("close", States.AJAR, States.CLOSE, ctx -> ctx.degreeOfOpening <= 1, ctx -> ctx.degreeOfOpening = 0)
                .addTransition("ajarPlus", States.AJAR, States.AJAR, ctx -> 1 <= ctx.degreeOfOpening && ctx.degreeOfOpening <= 98, ctx -> ctx.degreeOfOpening++)
                .addTransition("ajarMinus", States.AJAR, States.AJAR, ctx -> 2 <= ctx.degreeOfOpening && ctx.degreeOfOpening <= 99, ctx -> ctx.degreeOfOpening--)
                .setContext(new DoorFSMContext())
                .setStartState(States.OPEN)
                .build();

        testDoor(doorFSM);
    }

    private StaterStateMachineBuilder<States, DoorFSMContext> structureBuild() {
        return new StaterStateMachineBuilder<States, DoorFSMContext>().addTransition("preOpen", States.CLOSE, States.AJAR).addTransition("preClose", States.OPEN, States.AJAR).addTransition("open", States.AJAR, States.OPEN).addTransition("close", States.AJAR, States.CLOSE).addTransition("ajarPlus", States.AJAR, States.AJAR).addTransition("ajarMinus", States.AJAR, States.AJAR);
    }

    private StaterStateMachineBuilder<States, DoorFSMContext> eventsBuild(StaterStateMachineBuilder<States, DoorFSMContext> builder) {
        return builder.setTransitionEvent("preOpen", ctx -> ctx.degreeOfOpening = 1).setTransitionEvent("preClose", ctx -> ctx.degreeOfOpening = 99).setTransitionCondition("open", ctx -> ctx.degreeOfOpening >= 99).setTransitionEvent("open", ctx -> ctx.degreeOfOpening = 100).setTransitionCondition("close", ctx -> ctx.degreeOfOpening <= 1).setTransitionEvent("close", ctx -> ctx.degreeOfOpening = 0).setTransitionCondition("ajarPlus", ctx -> 1 <= ctx.degreeOfOpening && ctx.degreeOfOpening <= 98).setTransitionEvent("ajarPlus", ctx -> ctx.degreeOfOpening++).setTransitionCondition("ajarMinus", ctx -> 2 <= ctx.degreeOfOpening && ctx.degreeOfOpening <= 99).setTransitionEvent("ajarMinus", ctx -> ctx.degreeOfOpening--);
    }

    @Test
    public void testBuilder2() {
        StaterStateMachine<States, DoorFSMContext> doorFSM = eventsBuild(structureBuild()).setContext(new DoorFSMContext()).setStartState(States.OPEN).build();

        testDoor(doorFSM);
    }

    @Test
    public void testAutoTransition() {
        StaterStateMachine<States, DoorFSMContext> doorFSM = eventsBuild(structureBuild()).setContext(new DoorFSMContext()).setStartState(States.OPEN).build();

        doorFSM.autoTransition();
        assertEquals(doorFSM.getState(), States.AJAR);
    }

    @Test
    public void testBuilderFactory() {
        StaterStateMachine<States, DoorFSMContext> doorFSM = eventsBuild(structureBuild()).setContext(new DoorFSMContext()).setStartState(States.OPEN).setFactory(typedDoorFactory).build();


        testDoor(doorFSM);
        assertInstanceOf(TypesDoorStateMachine.class, doorFSM);
        typedTestDoor((TypesDoorStateMachine) doorFSM);
    }

    @Test
    public void testJsonSchema() throws Exception {
        StaterStateMachine<States, DoorFSMContext> validDoorFSM = eventsBuild(structureBuild()).setContext(new DoorFSMContext()).setStartState(States.OPEN).build();

        testDoor(validDoorFSM);

        String jsonSchema = validDoorFSM.toJsonSchema();
        StaterStateMachine<States, DoorFSMContext> doorFSM = eventsBuild(new StaterStateMachineBuilder<States, DoorFSMContext>().fromJsonSchema(jsonSchema, States::valueOf)).setContext(new DoorFSMContext()).setFactory(typedDoorFactory).build();
        assertEquals(jsonSchema, doorFSM.toJsonSchema());

        testDoor(doorFSM);

        assertInstanceOf(TypesDoorStateMachine.class, doorFSM);
        typedTestDoor((TypesDoorStateMachine) doorFSM);
    }

    @Test
    public void testStringGeneric() throws Exception {
        StaterStateMachine<States, DoorFSMContext> validDoorFSM = eventsBuild(structureBuild()).setContext(new DoorFSMContext()).setStartState(States.OPEN).build();
        testDoor(validDoorFSM);
        String jsonSchema = validDoorFSM.toJsonSchema();
        StaterStateMachine<String, DoorFSMContext> stringDoorFSM = new StaterStateMachineBuilder<String, DoorFSMContext>().fromJsonSchema(jsonSchema, it -> it).setContext(new DoorFSMContext()).build();
        assertEquals(stringDoorFSM.getState(), "OPEN");
    }

    @Test
    public void testJsonDump() throws Exception {
        class JsonConverter implements ContextJsonAdapter<DoorFSMContext> {
            @Override
            public String toJson(DoorFSMContext context) {
                return context.degreeOfOpening.toString();
            }

            @Override
            public DoorFSMContext fromJson(String json) {
                return new DoorFSMContext(Integer.parseInt(json));
            }
        }

        StaterStateMachine<States, DoorFSMContext> validDoorFSM = eventsBuild(structureBuild()).setContext(new DoorFSMContext()).setStartState(States.OPEN).setContextJsonAdapter(new JsonConverter()).build();
        assertEquals(validDoorFSM.getState(), States.OPEN);
        assertEquals(validDoorFSM.getContext().degreeOfOpening, 100);
        String dump = validDoorFSM.toJson();
        validDoorFSM.transition("preClose");
        assertEquals(validDoorFSM.getState(), States.AJAR);
        assertEquals(validDoorFSM.getContext().degreeOfOpening, 99);
        validDoorFSM.fromJson(dump, States::valueOf);
        assertEquals(validDoorFSM.getState(), States.OPEN);
        assertEquals(validDoorFSM.getContext().degreeOfOpening, 100);
    }

    @Test
    public void testMiddlewareAndCallbacks() {
        AtomicReference<Integer> transitionMiddleware = new AtomicReference<>(0);
        AtomicReference<Integer> transitionAllMiddleware = new AtomicReference<>(0);
        AtomicReference<Integer> subscribeOnTransition = new AtomicReference<>(0);
        AtomicReference<Integer> subscribeOnAllTransition = new AtomicReference<>(0);
        AtomicReference<Integer> subscribeOnState = new AtomicReference<>(0);
        AtomicReference<Integer> subscribeOnAllState = new AtomicReference<>(0);

        StaterStateMachine<States, DoorFSMContext> validDoorFSM = eventsBuild(structureBuild()).setContext(new DoorFSMContext()).setStartState(States.OPEN).transitionMiddleware("open", (context, next) -> {
            transitionMiddleware.getAndSet(transitionMiddleware.get() + 1);
            next.accept(context);
        }).transitionAllMiddleware((name, context, next) -> {
            transitionAllMiddleware.getAndSet(transitionAllMiddleware.get() + 1);
            next.accept(name, context);
        }).subscribeOnTransition("open", a -> subscribeOnTransition.getAndSet(subscribeOnTransition.get() + 1)).subscribeOnAllTransition((a, b) -> subscribeOnAllTransition.getAndSet(subscribeOnAllTransition.get() + 1)).subscribeOnState(States.AJAR, a -> subscribeOnState.getAndSet(subscribeOnState.get() + 1)).subscribeOnAllState((a, b) -> subscribeOnAllState.getAndSet(subscribeOnAllState.get() + 1)).build();
        testDoor(validDoorFSM);
        assertEquals(transitionMiddleware.get(), 1);
        assertEquals(transitionAllMiddleware.get(), 200);
        assertEquals(subscribeOnTransition.get(), 1);
        assertEquals(subscribeOnAllTransition.get(), 200);
        assertEquals(subscribeOnState.get(), 198);
        assertEquals(subscribeOnAllState.get(), 200);
    }
}