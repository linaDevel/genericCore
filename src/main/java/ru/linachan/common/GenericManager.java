package ru.linachan.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;

public abstract class GenericManager<T> {

    protected final GenericCore core = GenericCore.getInstance();
    protected Map<Class<? extends T>, T> objects;

    protected final static Logger logger = LoggerFactory.getLogger(GenericManager.class);

    public void setUp() {
        objects = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private Class<T> getGenericTypeClass() {
        return (Class) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isAbstract(Class<? extends T> targetClass) {
        return targetClass != null && Modifier.isAbstract(targetClass.getModifiers());
    }

    public void discover() {
        core.discover(getGenericTypeClass()).stream()
            .filter(object -> !objects.containsKey(object))
            .filter(object -> !isAbstract(object))
            .forEach(object -> {
                objects.put(object, null);
                onDiscover(object);
            });

    }

    public <V extends T> V get(Class<V> objectClass) {
        return objectClass.cast(objects.getOrDefault(objectClass, null));
    }

    protected abstract void onDiscover(Class<? extends T> object);
    public abstract void onShutDown();
}
