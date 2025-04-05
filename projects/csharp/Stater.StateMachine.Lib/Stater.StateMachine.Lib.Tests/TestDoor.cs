namespace Stater.StateMachine.Lib.Tests;

public class TestDoor
{
    private enum States
    {
        CLOSE,
        AJAR,
        OPEN
    }

    private class DoorFsmContext : IContext
    {
        public int DegreeOfOpening { get; set; } = 100;

        public DoorFsmContext()
        {
        }

        public DoorFsmContext(int degreeOfOpening)
        {
            DegreeOfOpening = degreeOfOpening;
        }

        public int GetDegreeOfOpening()
        {
            return DegreeOfOpening;
        }

        public void SetDegreeOfOpening(int degreeOfOpening)
        {
            DegreeOfOpening = degreeOfOpening;
        }
    }

    private class TypesDoorStateMachine : StaterStateMachine<States, DoorFsmContext>
    {
        public TypesDoorStateMachine()
            : base(
                new List<Transition<States, DoorFsmContext>>
                {
                    new("preOpen", States.CLOSE, States.AJAR, _ => true,
                        ctx => ctx.SetDegreeOfOpening(1)),
                    new("preClose", States.OPEN, States.AJAR, _ => true,
                        ctx => ctx.SetDegreeOfOpening(99)),
                    new("open", States.AJAR, States.OPEN,
                        ctx => ctx.GetDegreeOfOpening() >= 99, ctx => ctx.SetDegreeOfOpening(100)),
                    new("close", States.AJAR, States.CLOSE,
                        ctx => ctx.GetDegreeOfOpening() <= 1, ctx => ctx.SetDegreeOfOpening(0)),
                    new("ajarPlus", States.AJAR, States.AJAR,
                        ctx => ctx.GetDegreeOfOpening() >= 1 && ctx.GetDegreeOfOpening() <= 98,
                        ctx => ctx.SetDegreeOfOpening(ctx.GetDegreeOfOpening() + 1)),
                    new("ajarMinus", States.AJAR, States.AJAR,
                        ctx => ctx.GetDegreeOfOpening() >= 2 && ctx.GetDegreeOfOpening() <= 99,
                        ctx => ctx.SetDegreeOfOpening(ctx.GetDegreeOfOpening() - 1))
                },
                new DoorFsmContext(),
                States.OPEN,
                new HashSet<States> { States.OPEN, States.CLOSE, States.AJAR },
                new Dictionary<string, List<TransitionMiddleware<DoorFsmContext>>>(),
                new List<TransitionNameMiddleware<DoorFsmContext>>(),
                new Dictionary<string, List<Event<DoorFsmContext>>>(),
                new List<NameEvent<DoorFsmContext>>(),
                new Dictionary<States, List<Event<DoorFsmContext>>>(),
                new List<StateEvent<States, DoorFsmContext>>(),
                null
            )
        {
        }

        public TypesDoorStateMachine(
            List<Transition<States, DoorFsmContext>> transitions,
            DoorFsmContext context,
            States startState,
            HashSet<States> states,
            Dictionary<string, List<TransitionMiddleware<DoorFsmContext>>> transitionMiddlewares,
            List<TransitionNameMiddleware<DoorFsmContext>> transitionAllMiddlewares,
            Dictionary<string, List<Event<DoorFsmContext>>> transitionCallbacks,
            List<NameEvent<DoorFsmContext>> transitionAllCallbacks,
            Dictionary<States, List<Event<DoorFsmContext>>> stateCallbacks,
            List<StateEvent<States, DoorFsmContext>> stateAllCallbacks,
            IContextJsonAdapter<DoorFsmContext> contextJsonAdapter
        ) : base(
            transitions,
            context,
            startState,
            states,
            transitionMiddlewares,
            transitionAllMiddlewares,
            transitionCallbacks,
            transitionAllCallbacks,
            stateCallbacks,
            stateAllCallbacks,
            contextJsonAdapter
        )
        {
        }

        public void AjarPlus()
        {
            Transition("ajarPlus");
        }

        public void AjarMinus()
        {
            Transition("ajarMinus");
        }

        public void PreOpen()
        {
            Transition("preOpen");
        }

        public void PreClose()
        {
            Transition("preClose");
        }

        public void Open()
        {
            Transition("open");
        }

        public void Close()
        {
            Transition("close");
        }
    }


    private readonly StateMachineFactory<States, DoorFsmContext> _typedDoorFactory = (
        transitionsA,
        contextA,
        startStateA,
        statesA,
        transitionMiddlewaresA,
        transitionAllMiddlewaresA,
        transitionCallbacksA,
        transitionAllCallbacksA,
        stateCallbacksA,
        stateAllCallbacksA,
        contextJsonAdapterA
    ) => new TypesDoorStateMachine(
        transitionsA,
        contextA,
        startStateA,
        statesA,
        transitionMiddlewaresA,
        transitionAllMiddlewaresA,
        transitionCallbacksA,
        transitionAllCallbacksA,
        stateCallbacksA,
        stateAllCallbacksA,
        contextJsonAdapterA
    );

    private static void TestDoorSchema(StaterStateMachine<States, DoorFsmContext> door)
    {
        Assert.Equal(States.OPEN, door.GetState());
        Assert.Equal(100, door.GetContext().DegreeOfOpening);
        door.Transition("preClose");
        Assert.Equal(States.AJAR, door.GetState());
        Assert.Equal(99, door.GetContext().DegreeOfOpening);
        while (door.GetContext().DegreeOfOpening > 1)
        {
            door.Transition("ajarMinus");
            Assert.Equal(States.AJAR, door.GetState());
        }

        Assert.Equal(1, door.GetContext().DegreeOfOpening);
        door.Transition("close");
        Assert.Equal(0, door.GetContext().DegreeOfOpening);
        Assert.Equal(States.CLOSE, door.GetState());
        door.Transition("preOpen");
        Assert.Equal(1, door.GetContext().DegreeOfOpening);
        Assert.Equal(States.AJAR, door.GetState());
        door.Transition("ajarPlus");
        Assert.Equal(States.AJAR, door.GetState());
        Assert.Equal(2, door.GetContext().DegreeOfOpening);
        while (door.GetContext().DegreeOfOpening < 99)
        {
            door.Transition("ajarPlus");
            Assert.Equal(States.AJAR, door.GetState());
        }

        door.Transition("open");
        Assert.Equal(States.OPEN, door.GetState());
        Assert.Equal(100, door.GetContext().DegreeOfOpening);
    }

    private static void TypedTestDoor(TypesDoorStateMachine door)
    {
        Assert.Equal(States.OPEN, door.GetState());
        Assert.Equal(100, door.GetContext().DegreeOfOpening);
        door.PreClose();
        Assert.Equal(States.AJAR, door.GetState());
        Assert.Equal(99, door.GetContext().DegreeOfOpening);
        while (door.GetContext().DegreeOfOpening > 1)
        {
            door.AjarMinus();
            Assert.Equal(States.AJAR, door.GetState());
        }

        Assert.Equal(1, door.GetContext().DegreeOfOpening);
        door.Close();
        Assert.Equal(0, door.GetContext().DegreeOfOpening);
        Assert.Equal(States.CLOSE, door.GetState());
        door.PreOpen();
        Assert.Equal(1, door.GetContext().DegreeOfOpening);
        Assert.Equal(States.AJAR, door.GetState());
        door.AjarPlus();
        Assert.Equal(States.AJAR, door.GetState());
        Assert.Equal(2, door.GetContext().DegreeOfOpening);
        while (door.GetContext().DegreeOfOpening < 99)
        {
            door.AjarPlus();
            Assert.Equal(States.AJAR, door.GetState());
        }

        door.Open();
        Assert.Equal(States.OPEN, door.GetState());
        Assert.Equal(100, door.GetContext().DegreeOfOpening);
    }

    [Fact]
    public void TestSimpleBuild()
    {
        var doorFsm = new TypesDoorStateMachine();
        TestDoorSchema(doorFsm);
        TypedTestDoor(doorFsm);
    }

    [Fact]
    public void TestBuilder()
    {
        var doorFsm = new StaterStateMachineBuilder<States, DoorFsmContext>()
            .AddTransition("preOpen", States.CLOSE, States.AJAR, _ => true, context => context.DegreeOfOpening = 1)
            .AddTransition("preClose", States.OPEN, States.AJAR, _ => true, context => context.DegreeOfOpening = 99)
            .AddTransition("open", States.AJAR, States.OPEN, ctx => ctx.DegreeOfOpening >= 99,
                ctx => ctx.DegreeOfOpening = 100)
            .AddTransition("close", States.AJAR, States.CLOSE, ctx => ctx.DegreeOfOpening <= 1,
                ctx => ctx.DegreeOfOpening = 0)
            .AddTransition("ajarPlus", States.AJAR, States.AJAR,
                ctx => 1 <= ctx.DegreeOfOpening && ctx.DegreeOfOpening <= 98, ctx => ctx.DegreeOfOpening++)
            .AddTransition("ajarMinus", States.AJAR, States.AJAR,
                ctx => 2 <= ctx.DegreeOfOpening && ctx.DegreeOfOpening <= 99, ctx => ctx.DegreeOfOpening--)
            .SetContext(new DoorFsmContext())
            .SetStartState(States.OPEN)
            .Build();

        TestDoorSchema(doorFsm);
    }

    private static StaterStateMachineBuilder<States, DoorFsmContext> StructureBuild()
    {
        return new StaterStateMachineBuilder<States, DoorFsmContext>()
            .AddTransition("preOpen", States.CLOSE, States.AJAR).AddTransition("preClose", States.OPEN, States.AJAR)
            .AddTransition("open", States.AJAR, States.OPEN).AddTransition("close", States.AJAR, States.CLOSE)
            .AddTransition("ajarPlus", States.AJAR, States.AJAR).AddTransition("ajarMinus", States.AJAR, States.AJAR);
    }

    private static StaterStateMachineBuilder<States, DoorFsmContext> EventsBuild(
        StaterStateMachineBuilder<States, DoorFsmContext> builder)
    {
        return builder.SetTransitionEvent("preOpen", ctx => ctx.DegreeOfOpening = 1)
            .SetTransitionEvent("preClose", ctx => ctx.DegreeOfOpening = 99)
            .SetTransitionCondition("open", ctx => ctx.DegreeOfOpening >= 99)
            .SetTransitionEvent("open", ctx => ctx.DegreeOfOpening = 100)
            .SetTransitionCondition("close", ctx => ctx.DegreeOfOpening <= 1)
            .SetTransitionEvent("close", ctx => ctx.DegreeOfOpening = 0)
            .SetTransitionCondition("ajarPlus", ctx => 1 <= ctx.DegreeOfOpening && ctx.DegreeOfOpening <= 98)
            .SetTransitionEvent("ajarPlus", ctx => ctx.DegreeOfOpening++)
            .SetTransitionCondition("ajarMinus", ctx => 2 <= ctx.DegreeOfOpening && ctx.DegreeOfOpening <= 99)
            .SetTransitionEvent("ajarMinus", ctx => ctx.DegreeOfOpening--);
    }

    [Fact]
    public void TestBuilder2()
    {
        var doorFsm = EventsBuild(StructureBuild()).SetContext(new DoorFsmContext()).SetStartState(States.OPEN).Build();

        TestDoorSchema(doorFsm);
    }

    [Fact]
    public void TestAutoTransition()
    {
        var doorFsm = EventsBuild(StructureBuild()).SetContext(new DoorFsmContext()).SetStartState(States.OPEN).Build();

        doorFsm.AutoTransition();
        Assert.Equal(States.AJAR, doorFsm.GetState());
    }

    [Fact]
    public void TestBuilderFactory()
    {
        var doorFsm = EventsBuild(StructureBuild()).SetContext(new DoorFsmContext()).SetStartState(States.OPEN)
            .SetFactory(_typedDoorFactory).Build();


        TestDoorSchema(doorFsm);
        Assert.True(doorFsm is TypesDoorStateMachine);
        TypedTestDoor((TypesDoorStateMachine)doorFsm);
    }

    [Fact]
    public void TestJsonSchema()
    {
        var validDoorFsm = EventsBuild(StructureBuild()).SetContext(new DoorFsmContext()).SetStartState(States.OPEN)
            .Build();

        TestDoorSchema(validDoorFsm);

        var jsonSchema = validDoorFsm.ToJsonSchema();
        var doorFsm = EventsBuild(new StaterStateMachineBuilder<States, DoorFsmContext>()
                .FromJsonSchema(jsonSchema,
                    v =>
                    {
                        States value;
                        Enum.TryParse(v, out value);
                        return value;
                    }))
            .SetContext(new DoorFsmContext())
            .SetFactory(_typedDoorFactory)
            .Build();
        Assert.Equal(jsonSchema, doorFsm.ToJsonSchema());

        TestDoorSchema(doorFsm);

        Assert.True(doorFsm is TypesDoorStateMachine);
        TypedTestDoor((TypesDoorStateMachine)doorFsm);
    }

    [Fact]
    public void TestStringGeneric()
    {
        var validDoorFsm = EventsBuild(StructureBuild()).SetContext(new DoorFsmContext())
            .SetStartState(States.OPEN)
            .Build();
        TestDoorSchema(validDoorFsm);
        var jsonSchema = validDoorFsm.ToJsonSchema();
        var stringDoorFsm = new StaterStateMachineBuilder<string, DoorFsmContext>()
            .FromJsonSchema(jsonSchema, v =>
            {
                States value;
                Enum.TryParse(v, out value);
                return value.ToString();
            })
            .SetContext(new DoorFsmContext()).Build();
        Assert.Equal("OPEN", stringDoorFsm.GetState());
    }

    private class JsonConverter : IContextJsonAdapter<DoorFsmContext>
    {
        public string ToJson(DoorFsmContext context)
        {
            return context.DegreeOfOpening.ToString();
        }

        public DoorFsmContext FromJson(string json)
        {
            return new DoorFsmContext(int.Parse(json));
        }
    }

    [Fact]
    public void TestJsonDump()
    {
        StaterStateMachine<States, DoorFsmContext> validDoorFsm = EventsBuild(StructureBuild())
            .SetContext(new DoorFsmContext()).SetStartState(States.OPEN).SetContextJsonAdapter(new JsonConverter())
            .Build();
        Assert.Equal(States.OPEN, validDoorFsm.GetState());
        Assert.Equal(100, validDoorFsm.GetContext().DegreeOfOpening);
        var dump = validDoorFsm.ToJson();
        validDoorFsm.Transition("preClose");
        Assert.Equal(States.AJAR, validDoorFsm.GetState());
        Assert.Equal(99, validDoorFsm.GetContext().DegreeOfOpening);
        validDoorFsm.FromJson(dump, v =>
        {
            States value;
            Enum.TryParse(v, out value);
            return value;
        });
        Assert.Equal(States.OPEN, validDoorFsm.GetState());
        Assert.Equal(100, validDoorFsm.GetContext().DegreeOfOpening);
    }

    [Fact]
    public void TestMiddlewareAndCallbacks()
    {
        var transitionMiddleware = 0;
        var transitionAllMiddleware = 0;
        var subscribeOnTransition = 0;
        var subscribeOnAllTransition = 0;
        var subscribeOnState = 0;
        var subscribeOnAllState = 0;

        var validDoorFsm = EventsBuild(StructureBuild())
            .SetContext(new DoorFsmContext()).SetStartState(States.OPEN).TransitionMiddleware("open",
                (context, next) =>
                {
                    transitionMiddleware++;
                    next(context);
                })
            .TransitionAllMiddleware((name, context, next) =>
            {
                transitionAllMiddleware++;
                next(name, context);
            })
            .SubscribeOnTransition("open", _ => subscribeOnTransition++)
            .SubscribeOnAllTransition((_, _) => subscribeOnAllTransition++)
            .SubscribeOnState(States.AJAR, _ => subscribeOnState++)
            .SubscribeOnAllState((_, _) => subscribeOnAllState++)
            .Build();
        TestDoorSchema(validDoorFsm);
        Assert.Equal(1, transitionMiddleware);
        Assert.Equal(200, transitionAllMiddleware);
        Assert.Equal(1, subscribeOnTransition);
        Assert.Equal(200, subscribeOnAllTransition);
        Assert.Equal(198, subscribeOnState);
        Assert.Equal(200, subscribeOnAllState);
    }
}