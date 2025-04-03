using Newtonsoft.Json;

namespace Stater.StateMachine.Lib;

public interface IContext;

public class EmptyContext : IContext;

public interface IContextJsonAdapter<TC> where TC : IContext
{
    string ToJson(TC context);
    TC FromJson(string json);
}

public delegate void Event<in TC>(TC context) where TC : IContext;

public delegate void NameEvent<in TC>(string name, TC context) where TC : IContext;

public delegate void StateEvent<T, in TC>(T state, TC context) where TC : IContext;

public delegate void TransitionMiddleware<TC>(TC context, Action<TC> next) where TC : IContext;

public delegate void TransitionNameMiddleware<TC>(string name, TC context, Action<string, TC> next) where TC : IContext;

public delegate StaterStateMachine<T, TC> StateMachineFactory<T, TC>(List<Transition<T, TC>> transitions, TC context,
    T startState, HashSet<T> states, Dictionary<String, List<TransitionMiddleware<TC>>> transitionMiddlewares,
    List<TransitionNameMiddleware<TC>> transitionAllMiddlewares, Dictionary<string, List<Event<TC>>> transitionCallbacks,
    List<NameEvent<TC>> transitionAllCallbacks, Dictionary<T, List<Event<TC>>> stateCallbacks,
    List<StateEvent<T, TC>> stateAllCallbacks, IContextJsonAdapter<TC> contextJsonAdapter) where TC : IContext where T : notnull;

public class Transition<T, TC>(string name, T start, T end, Predicate<TC>? condition = null, Event<TC>? eEvent = null)
    where TC : IContext
{
    public string Name { get; } = name;
    public T Start { get; } = start;
    public T End { get; } = end;

    [System.Text.Json.Serialization.JsonIgnore]
    public Predicate<TC>? Condition { get; } = condition;

    [System.Text.Json.Serialization.JsonIgnore]
    public Event<TC>? Event { get; } = eEvent;
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

public abstract class StaterStateMachine<T, TC> where TC : IContext where T : notnull
{
    private TC _context;
    private readonly T _startState;
    private readonly HashSet<T> _states;
    private T _state;
    private readonly List<Transition<T, TC>> _transitions;
    private readonly Dictionary<T, List<Transition<T, TC>>> _transitionsGroupedStart = new();
    private readonly Dictionary<string, Transition<T, TC>> _transitionsByName = new();
    private readonly Dictionary<string, List<TransitionMiddleware<TC>>> _transitionMiddlewares = new();
    private readonly List<TransitionNameMiddleware<TC>> _transitionAllMiddlewares = new();
    private readonly Dictionary<string, List<Event<TC>>> _transitionCallbacks = new();
    private readonly List<NameEvent<TC>> _transitionAllCallbacks = new();
    private readonly Dictionary<T, List<Event<TC>>> _stateCallbacks = new();
    private readonly List<StateEvent<T, TC>> _stateAllCallbacks = new();
    private readonly IContextJsonAdapter<TC> _contextJsonAdapter;
    private bool _enableEvents = true;

    public StaterStateMachine(List<Transition<T, TC>> transitions, TC context, T startState, HashSet<T> states,
        Dictionary<string, List<TransitionMiddleware<TC>>> transitionMiddlewares,
        List<TransitionNameMiddleware<TC>> transitionAllMiddlewares,
        Dictionary<string, List<Event<TC>>> transitionCallbacks, List<NameEvent<TC>> transitionAllCallbacks,
        Dictionary<T, List<Event<TC>>> stateCallbacks, List<StateEvent<T, TC>> stateAllCallbacks,
        IContextJsonAdapter<TC> contextJsonAdapter)
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
                _transitionsGroupedStart[transition.Start] = new List<Transition<T, TC>>();
            _transitionsGroupedStart[transition.Start].Add(transition);
            _transitionsByName[transition.Name] = transition;
        }
    }

    public StaterStateMachine(List<Transition<T, TC>> transitions, TC context, T startState)
    {
        _transitions = transitions;
        _context = context;
        _startState = startState;
        _states = new HashSet<T>(transitions.Select(t => t.Start).Concat(transitions.Select(t => t.End)));
        _state = startState;

        foreach (var transition in transitions)
        {
            if (!_transitionsGroupedStart.ContainsKey(transition.Start))
                _transitionsGroupedStart[transition.Start] = new List<Transition<T, TC>>();
            _transitionsGroupedStart[transition.Start].Add(transition);
            _transitionsByName[transition.Name] = transition;
        }
    }

    public T GetState() => _state;
    public TC GetContext() => _context;

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
            : Enumerable.Empty<TransitionMiddleware<TC>>().GetEnumerator();

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

        void InternalNext(TC ctx)
        {
            if (middlewareEnumerator.MoveNext())
                middlewareEnumerator.Current(ctx, InternalNext);
            else
                ConditionHandler();
        }

        void Next(string transitionName, TC ctx)
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

public class StaterStateMachineBuilder<T, TC> where TC : IContext
{
    private readonly Dictionary<string, Transition<T, TC>> _transitions = new();
    private T _state;
    private readonly HashSet<T> _states = new();
    private TC _context;

    private readonly Dictionary<string, List<TransitionMiddleware<TC>>> _transitionMiddlewares = new();
    private readonly List<TransitionNameMiddleware<TC>> _transitionAllMiddlewares = new();

    private readonly Dictionary<string, List<Event<TC>>> _transitionCallbacks = new();
    private readonly List<NameEvent<TC>> _transitionAllCallbacks = new();

    private readonly Dictionary<T, List<Event<TC>>> _stateCallbacks = new();
    private readonly List<StateEvent<T, TC>> _stateAllCallbacks = new();
    private IContextJsonAdapter<TC> _contextJsonAdapter;

    private StateMachineFactory<T, TC> _factory =
    (transitions, context, state, states, middlewares, allMiddlewares, callbacks, allCallbacks, stateCallbacks,
        stateAllCallbacks, adapter) => new BaseFsm<T, TC>(transitions, context, state, states, middlewares,
        allMiddlewares, callbacks, allCallbacks, stateCallbacks, stateAllCallbacks, adapter);

    public StaterStateMachineBuilder<T, TC> AddTransition(string name, T start, T end, Predicate<TC> condition,
        Event<TC> action)
    {
        _states.Add(start);
        _states.Add(end);
        _transitions[name] = new Transition<T, TC>(name, start, end, condition, action);
        return this;
    }

    public StaterStateMachineBuilder<T, TC> AddTransition(string name, T start, T end, Predicate<TC> condition) =>
        AddTransition(name, start, end, condition, _ => { });

    public StaterStateMachineBuilder<T, TC> AddTransition(string name, T start, T end, Event<TC> eEvent) =>
        AddTransition(name, start, end, _ => true, eEvent);

    public StaterStateMachineBuilder<T, TC> AddTransition(string name, T start, T end) =>
        AddTransition(name, start, end, _ => true, _ => { });

    public StaterStateMachineBuilder<T, TC> AddState(T state)
    {
        _states.Add(state);
        return this;
    }

    public StaterStateMachineBuilder<T, TC> SetTransitionCondition(string name, Predicate<TC> condition)
    {
        if (!_transitions.ContainsKey(name)) throw new InvalidOperationException($"Transition not found: {name}");
        var transition = _transitions[name];
        _transitions[name] = new Transition<T, TC>(name, transition.Start, transition.End, condition, transition.Event);
        return this;
    }

    public StaterStateMachineBuilder<T, TC> SetTransitionEvent(string name, Event<TC> eEvent)
    {
        if (!_transitions.ContainsKey(name)) throw new InvalidOperationException($"Transition not found: {name}");
        var transition = _transitions[name];
        _transitions[name] = new Transition<T, TC>(name, transition.Start, transition.End, transition.Condition, eEvent);
        return this;
    }

    public StaterStateMachineBuilder<T, TC> TransitionMiddleware(string name, TransitionMiddleware<TC> middleware)
    {
        if (!_transitionMiddlewares.ContainsKey(name)) _transitionMiddlewares[name] = new List<TransitionMiddleware<TC>>();
        _transitionMiddlewares[name].Add(middleware);
        return this;
    }

    public StaterStateMachineBuilder<T, TC> TransitionAllMiddleware(TransitionNameMiddleware<TC> middleware)
    {
        _transitionAllMiddlewares.Add(middleware);
        return this;
    }

    public StaterStateMachineBuilder<T, TC> SubscribeOnTransition(string name, Event<TC> callback)
    {
        if (!_transitionCallbacks.ContainsKey(name)) _transitionCallbacks[name] = new List<Event<TC>>();
        _transitionCallbacks[name].Add(callback);
        return this;
    }

    public StaterStateMachineBuilder<T, TC> SubscribeOnAllTransition(NameEvent<TC> callback)
    {
        _transitionAllCallbacks.Add(callback);
        return this;
    }

    public StaterStateMachineBuilder<T, TC> SubscribeOnState(T state, Event<TC> callback)
    {
        if (!_stateCallbacks.ContainsKey(state)) _stateCallbacks[state] = new List<Event<TC>>();
        _stateCallbacks[state].Add(callback);
        return this;
    }

    public StaterStateMachineBuilder<T, TC> SubscribeOnAllState(StateEvent<T, TC> callback)
    {
        _stateAllCallbacks.Add(callback);
        return this;
    }

    public StaterStateMachineBuilder<T, TC> SetStartState(T state)
    {
        this._state = state;
        return this;
    }

    public StaterStateMachineBuilder<T, TC> SetContext(TC context)
    {
        this._context = context;
        return this;
    }

    public StaterStateMachineBuilder<T, TC> FromJsonSchema(string schema, Func<string, T> stateConverter)
    {
        var schemaObject = JsonConvert.DeserializeObject<JsonSchema<T>>(schema);
        foreach (var s in schemaObject.States) AddState(stateConverter(s.ToString()));
        foreach (var t in schemaObject.Transitions)
            AddTransition(t.Name, stateConverter(t.Start.ToString()), stateConverter(t.End.ToString()));
        SetStartState(stateConverter(schemaObject.StartState.ToString()));
        return this;
    }

    public StaterStateMachineBuilder<T, TC> SetFactory(StateMachineFactory<T, TC> factory)
    {
        _factory = factory;
        return this;
    }

    public StaterStateMachineBuilder<T, TC> SetContextJsonAdapter(IContextJsonAdapter<TC> adapter)
    {
        _contextJsonAdapter = adapter;
        return this;
    }

    public StaterStateMachine<T, TC> Build()
    {
        if (_context == null) throw new InvalidOperationException("Context must be set");
        if (_state == null && _transitions.Count > 0) _state = _transitions.Values.First().Start;
        return _factory(new List<Transition<T, TC>>(_transitions.Values), _context, _state, _states, _transitionMiddlewares,
            _transitionAllMiddlewares, _transitionCallbacks, _transitionAllCallbacks, _stateCallbacks, _stateAllCallbacks,
            _contextJsonAdapter);
    }
}