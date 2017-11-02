package project.connect4;

import java.util.*;
import java.util.Map;

/**
 * Created by Philip on 10/31/2017.
 */

public class EventSystem {
    //The interface that hooks will be calling (A void/void function)
    public interface HookInterface{
        void poke();
    }

    //The main list of events
    private static Map<String,List<HookInterface>> events = new HashMap<>();

    EventSystem()
    {
        clearEvents();
    }

    //Clear events
    public static void clearEvents()
    {
        events = new HashMap<>();
    }

    //Add a new event name
    public static void initEvent(String name)
    {
        //events.putIfAbsent(name,new ArrayList<>());
        if (!events.containsKey(name))
            events.put(name,new ArrayList<>());
    }

    //Add a new hook to existing event name
    public static void addHook(String name, HookInterface hook)
    {
        if (events.containsKey(name))
            events.get(name).add(hook);
    }

    //Call all hooks attached to an event name
    public static void triggerEvent(String name)
    {
        //events.putIfAbsent(name,new ArrayList<>());
        if (events.containsKey(name))
        {
            List<HookInterface> hooks = events.get(name);
            for (int i = 0; i < hooks.size(); i++)
            {
                hooks.get(i).poke();
            }
        }
    }
}
