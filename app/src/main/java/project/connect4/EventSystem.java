package project.connect4;

import java.util.*;
import java.util.Map;

/**
 * Created by Philip on 10/31/2017.
 */

public class EventSystem {
    public interface HookInterface{
        void poke();
    }
    private static Map<String,List<HookInterface>> events = new HashMap<>();

    EventSystem()
    {
        clearEvents();
    }

    public static void clearEvents()
    {
        events = new HashMap<>();
    }

    public static void initEvent(String name)
    {
        //events.putIfAbsent(name,new ArrayList<>());
        if (!events.containsKey(name))
            events.put(name,new ArrayList<>());
    }
    public static void addHook(String name, HookInterface hook)
    {
        if (events.containsKey(name))
            events.get(name).add(hook);
    }
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
