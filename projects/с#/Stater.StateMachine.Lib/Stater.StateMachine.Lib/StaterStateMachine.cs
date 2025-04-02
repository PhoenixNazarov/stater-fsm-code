using Newtonsoft.Json;

namespace Stater.StateMachine.Lib;

public interface IContext
{
}

public class EmptyContext : IContext;

public interface IContextJsonAdapter<C> where C : IContext
{
    string ToJson(C context);
    C FromJson(string json);
}

public delegate void Event<C>(C context) where C : IContext;

public delegate void NameEvent<C>(string name, C context) where C : IContext;

public delegate void StateEvent<T, C>(T state, C context) where C : IContext;

public delegate void TransitionMiddleware<C>(C context, Action<C> next) where C : IContext;

public delegate void TransitionNameMiddleware<C>(string name, C context, Action<string, C> next) where C : IContext;

public delegate StaterStateMachine<T, C> StateMachineFactory<T, C>(List<Transition<T, C>> transitions, C context,
    T startState, HashSet<T> states, Dictionary<String, List<TransitionMiddleware<C>>> transitionMiddlewares,
    List<TransitionNameMiddleware<C>> transitionAllMiddlewares, Dictionary<String, List<Event<C>>> transitionCallbacks,
    List<NameEvent<C>> transitionAllCallbacks, Dictionary<T, List<Event<C>>> stateCallbacks,
    List<StateEvent<T, C>> stateAllCallbacks, IContextJsonAdapter<C> contextJsonAdapter) where C : IContext;

public class Transition<T, C>(string name, T start, T end, Predicate<C>? condition = null, Event<C>? eEvent = null)
    where C : IContext
{
    public string Name { get; } = name;
    public T Start { get; } = start;
    public T End { get; } = end;

    [System.Text.Json.Serialization.JsonIgnore]
    public Predicate<C>? Condition { get; } = condition;

    [System.Text.Json.Serialization.JsonIgnore]
    public Event<C>? Event { get; } = eEvent;
}

public class JsonSchema<T>
(
    List<T> states,
    T startState,
    List<Transition<T, EmptyContext>> transitions
)
{
    public List<T> States { get; } = states;
    public T StartState { get; } = startState;
    public List<Transition<T, EmptyContext>> Transitions { get; } = transitions;
}

public class JsonState<T>(
    T state,
    string context
)
{
    public T State { get; } = state;
    public string Context { get; } = context;
}

public abstract class StaterStateMachine<T, C> where C : IContext
{
    private C _context;
    private readonly T _startState;
    private readonly HashSet<T> _states;
    private T _state;
    private readonly List<Transition<T, C>> _transitions;
    private readonly Dictionary<T, List<Transition<T, C>>> _transitionsGroupedStart = new();
    private readonly Dictionary<string, Transition<T, C>> _transitionsByName = new();
    private readonly Dictionary<string, List<TransitionMiddleware<C>>> _transitionMiddlewares = new();
    private readonly List<TransitionNameMiddleware<C>> _transitionAllMiddlewares = new();
    private readonly Dictionary<string, List<Event<C>>> _transitionCallbacks = new();
    private readonly List<NameEvent<C>> _transitionAllCallbacks = new();
    private readonly Dictionary<T, List<Event<C>>> _stateCallbacks = new();
    private readonly List<StateEvent<T, C>> _stateAllCallbacks = new();
    private readonly IContextJsonAdapter<C> _contextJsonAdapter;
    private bool _enableEvents = true;

    public StaterStateMachine(List<Transition<T, C>> transitions, C context, T startState, HashSet<T> states,
        Dictionary<string, List<TransitionMiddleware<C>>> transitionMiddlewares,
        List<TransitionNameMiddleware<C>> transitionAllMiddlewares,
        Dictionary<string, List<Event<C>>> transitionCallbacks, List<NameEvent<C>> transitionAllCallbacks,
        Dictionary<T, List<Event<C>>> stateCallbacks, List<StateEvent<T, C>> stateAllCallbacks,
        IContextJsonAdapter<C> contextJsonAdapter)
    {
        _transitions = transitions;
        _context = context;
        _startState = startState;
        _states = states;
        _state = startState;
        _transitionMiddlewares = transitionMiddlewares;
        _transitionAllMiddlewares = transitionAllMiddlewares;
        _transitionCallbacks = transitionCallbacks;
        _transitionAllCallbacks = transitionAllCallbacks;
        _stateCallbacks = stateCallbacks;
        _stateAllCallbacks = stateAllCallbacks;
        _contextJsonAdapter = contextJsonAdapter;

        foreach (var transition in transitions)
        {
            if (!_transitionsGroupedStart.ContainsKey(transition.Start))
                _transitionsGroupedStart[transition.Start] = new List<Transition<T, C>>();
            _transitionsGroupedStart[transition.Start].Add(transition);
            _transitionsByName[transition.Name] = transition;
        }
    }

    public StaterStateMachine(List<Transition<T, C>> transitions, C context, T startState)
    {
        _transitions = transitions;
        _context = context;
        _startState = startState;
        _states = new HashSet<T>(transitions.Select(t => t.Start).Concat(transitions.Select(t => t.End)));
        _state = startState;

        foreach (var transition in transitions)
        {
            if (!_transitionsGroupedStart.ContainsKey(transition.Start))
                _transitionsGroupedStart[transition.Start] = new List<Transition<T, C>>();
            _transitionsGroupedStart[transition.Start].Add(transition);
            _transitionsByName[transition.Name] = transition;
        }
    }

    public T GetState() => _state;
    public C GetContext() => _context;

    public void Transition(string name)
    {
        if (!_transitionsByName.TryGetValue(name, out var transition))
            throw new InvalidOperationException($"Transition not found: {name}");

        if (!EqualityComparer<T>.Default.Equals(_state, transition.Start))
            throw new InvalidOperationException(
                $"Start state does not match transition's start state: {transition.Start}");

        if (transition.Condition != null && !transition.Condition(_context))
            throw new InvalidOperationException($"Condition returned false for transition {name}");

        void ConditionHandler()
        {
            if (transition.Condition != null && !transition.Condition(_context))
                throw new InvalidOperationException($"Condition returned false for transition {name}");
        }

        var middlewareEnumerator = _transitionMiddlewares.TryGetValue(name, out var middlewares)
            ? middlewares.GetEnumerator()
            : Enumerable.Empty<TransitionMiddleware<C>>().GetEnumerator();

        var allMiddlewareEnumerator = _transitionAllMiddlewares.GetEnumerator();

        if (_enableEvents)
            Next(name, _context);

        _state = transition.End;
        if (!_enableEvents)
            return;

        transition.Event?.Invoke(_context);
        if (_transitionCallbacks.TryGetValue(name, out var events))
            foreach (var evt in events)
                evt(_context);

        foreach (var evt in _transitionAllCallbacks)
            evt(name, _context);

        if (_stateCallbacks.TryGetValue(_state, out var stateEvents))
            foreach (var evt in stateEvents)
                evt(_context);

        foreach (var evt in _stateAllCallbacks)
            evt(_state, _context);
        return;

        void InternalNext(C ctx)
        {
            if (middlewareEnumerator.MoveNext())
                middlewareEnumerator.Current(ctx, InternalNext);
            else
                ConditionHandler();
        }

        void Next(string transitionName, C ctx)
        {
            if (allMiddlewareEnumerator.MoveNext())
                allMiddlewareEnumerator.Current(transitionName, ctx, Next);
            else
                InternalNext(ctx);
        }
    }

    public void AutoTransition()
    {
        if (_transitionsGroupedStart.TryGetValue(_state, out var possibleTransitions))
        {
            foreach (var transition in possibleTransitions)
            {
                try
                {
                    Transition(transition.Name);
                    return;
                }
                catch
                {
                    // ignored
                }
            }
        }
    }

    public string ToJsonSchema() => JsonConvert.SerializeObject(new JsonSchema<T>(
        _states.OrderBy(s => s?.ToString(), StringComparer.CurrentCulture).ToList(), 
        _startState,
        _transitions.Select(x => new Transition<T, EmptyContext>(x.Name, x.Start, x.End)).ToList()));

    public string ToJson()
    {
        if (_contextJsonAdapter == null)
            throw new InvalidOperationException("ContextJsonAdapter is not set");
        return JsonConvert.SerializeObject(new JsonState<T>(_state, _contextJsonAdapter.ToJson(_context)));
    }

    public void FromJson(string json, Func<string, T> stateConverter)
    {
        var jsonState = JsonConvert.DeserializeObject<JsonState<T>>(json);
        _state = stateConverter(jsonState?.State?.ToString() ?? string.Empty);
        _context = _contextJsonAdapter.FromJson(jsonState!.Context);
    }

    public void EnableEvents() => _enableEvents = true;
    public void DisableEvents() => _enableEvents = false;
}

internal class BaseFsm<T, C> : StaterStateMachine<T, C> where C : IContext
{
    public BaseFsm(
        List<Transition<T, C>> transitions,
        C context,
        T startState,
        HashSet<T> states,
        Dictionary<string, List<TransitionMiddleware<C>>> transitionMiddlewares,
        List<TransitionNameMiddleware<C>> transitionAllMiddlewares,
        Dictionary<string, List<Event<C>>> transitionCallbacks,
        List<NameEvent<C>> transitionAllCallbacks,
        Dictionary<T, List<Event<C>>> stateCallbacks,
        List<StateEvent<T, C>> stateAllCallbacks,
        IContextJsonAdapter<C> contextJsonAdapter
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

    public BaseFsm(List<Transition<T, C>> transitions, C context, T startState)
        : base(transitions, context, startState)
    {
    }
}

public class StaterStateMachineBuilder<T, C> where C : IContext
{
    private readonly Dictionary<string, Transition<T, C>> transitions = new();
    private T state;
    private readonly HashSet<T> states = new();
    private C context;

    private readonly Dictionary<string, List<TransitionMiddleware<C>>> transitionMiddlewares = new();
    private readonly List<TransitionNameMiddleware<C>> transitionAllMiddlewares = new();

    private readonly Dictionary<string, List<Event<C>>> transitionCallbacks = new();
    private readonly List<NameEvent<C>> transitionAllCallbacks = new();

    private readonly Dictionary<T, List<Event<C>>> stateCallbacks = new();
    private readonly List<StateEvent<T, C>> stateAllCallbacks = new();
    private IContextJsonAdapter<C> contextJsonAdapter;

    private StateMachineFactory<T, C> factory =
    (transitions, context, state, states, middlewares, allMiddlewares, callbacks, allCallbacks, stateCallbacks,
        stateAllCallbacks, adapter) => new BaseFsm<T, C>(transitions, context, state, states, middlewares,
        allMiddlewares, callbacks, allCallbacks, stateCallbacks, stateAllCallbacks, adapter);

    public StaterStateMachineBuilder<T, C> AddTransition(string name, T start, T end, Predicate<C> condition,
        Event<C> action)
    {
        states.Add(start);
        states.Add(end);
        transitions[name] = new Transition<T, C>(name, start, end, condition, action);
        return this;
    }

    public StaterStateMachineBuilder<T, C> AddTransition(string name, T start, T end, Predicate<C> condition) =>
        AddTransition(name, start, end, condition, _ => { });

    public StaterStateMachineBuilder<T, C> AddTransition(string name, T start, T end, Event<C> eEvent) =>
        AddTransition(name, start, end, _ => true, eEvent);

    public StaterStateMachineBuilder<T, C> AddTransition(string name, T start, T end) =>
        AddTransition(name, start, end, _ => true, _ => { });

    public StaterStateMachineBuilder<T, C> AddState(T state)
    {
        states.Add(state);
        return this;
    }

    public StaterStateMachineBuilder<T, C> SetTransitionCondition(string name, Predicate<C> condition)
    {
        if (!transitions.ContainsKey(name)) throw new InvalidOperationException($"Transition not found: {name}");
        var transition = transitions[name];
        transitions[name] = new Transition<T, C>(name, transition.Start, transition.End, condition, transition.Event);
        return this;
    }

    public StaterStateMachineBuilder<T, C> SetTransitionEvent(string name, Event<C> eEvent)
    {
        if (!transitions.ContainsKey(name)) throw new InvalidOperationException($"Transition not found: {name}");
        var transition = transitions[name];
        transitions[name] = new Transition<T, C>(name, transition.Start, transition.End, transition.Condition, eEvent);
        return this;
    }

    public StaterStateMachineBuilder<T, C> TransitionMiddleware(string name, TransitionMiddleware<C> middleware)
    {
        if (!transitionMiddlewares.ContainsKey(name)) transitionMiddlewares[name] = new List<TransitionMiddleware<C>>();
        transitionMiddlewares[name].Add(middleware);
        return this;
    }

    public StaterStateMachineBuilder<T, C> TransitionAllMiddleware(TransitionNameMiddleware<C> middleware)
    {
        transitionAllMiddlewares.Add(middleware);
        return this;
    }

    public StaterStateMachineBuilder<T, C> SubscribeOnTransition(string name, Event<C> callback)
    {
        if (!transitionCallbacks.ContainsKey(name)) transitionCallbacks[name] = new List<Event<C>>();
        transitionCallbacks[name].Add(callback);
        return this;
    }

    public StaterStateMachineBuilder<T, C> SubscribeOnAllTransition(NameEvent<C> callback)
    {
        transitionAllCallbacks.Add(callback);
        return this;
    }

    public StaterStateMachineBuilder<T, C> SubscribeOnState(T state, Event<C> callback)
    {
        if (!stateCallbacks.ContainsKey(state)) stateCallbacks[state] = new List<Event<C>>();
        stateCallbacks[state].Add(callback);
        return this;
    }

    public StaterStateMachineBuilder<T, C> SubscribeOnAllState(StateEvent<T, C> callback)
    {
        stateAllCallbacks.Add(callback);
        return this;
    }

    public StaterStateMachineBuilder<T, C> SetStartState(T state)
    {
        this.state = state;
        return this;
    }

    public StaterStateMachineBuilder<T, C> SetContext(C context)
    {
        this.context = context;
        return this;
    }

    public StaterStateMachineBuilder<T, C> FromJsonSchema(string schema, Func<string, T> stateConverter)
    {
        var schemaObject = JsonConvert.DeserializeObject<JsonSchema<T>>(schema);
        foreach (var s in schemaObject.States) AddState(stateConverter(s.ToString()));
        foreach (var t in schemaObject.Transitions)
            AddTransition(t.Name, stateConverter(t.Start.ToString()), stateConverter(t.End.ToString()));
        SetStartState(stateConverter(schemaObject.StartState.ToString()));
        return this;
    }

    public StaterStateMachineBuilder<T, C> SetFactory(StateMachineFactory<T, C> factory)
    {
        this.factory = factory;
        return this;
    }

    public StaterStateMachineBuilder<T, C> SetContextJsonAdapter(IContextJsonAdapter<C> adapter)
    {
        this.contextJsonAdapter = adapter;
        return this;
    }

    public StaterStateMachine<T, C> Build()
    {
        if (context == null) throw new InvalidOperationException("Context must be set");
        if (state == null && transitions.Count > 0) state = transitions.Values.First().Start;
        return factory(new List<Transition<T, C>>(transitions.Values), context, state, states, transitionMiddlewares,
            transitionAllMiddlewares, transitionCallbacks, transitionAllCallbacks, stateCallbacks, stateAllCallbacks,
            contextJsonAdapter);
    }
}