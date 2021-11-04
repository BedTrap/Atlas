package me.bedtrapteam.addon.utils;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;

public class StrafeUtils
{
    public static <T> T getPrivateValue(final Class clazz, final Object object, final String... names) {
        final Field field = getField(clazz, names);
        field.setAccessible(true);
        try {
            return (T)field.get(object);
        }
        catch (IllegalAccessException ex) {
            return null;
        }
    }

    public static <T> boolean setPrivateValue(final Class clazz, final Object object, final T value, final String... names) {
        final Field field = getField(clazz, names);
        field.setAccessible(true);
        try {
            field.set(object, value);
        }
        catch (IllegalAccessException ignored) {
            return false;
        }
        return true;
    }

    public static Field getField(final Class clazz, final String... names) {
        Field field = null;
        for (final String name : names) {
            if (field != null) {
                break;
            }
            try {
                field = clazz.getDeclaredField(name);
            }
            catch (NoSuchFieldException ex) {}
        }
        return field;
    }

    public static <T> T callPrivateMethod(final Class<?> clazz, final Object object, final String[] names, final Object... args) {
        final Class<?>[] classes = (Class<?>[])new Class[0];
        for (int i = 0; i < args.length; ++i) {
            classes[i] = args[i].getClass();
        }
        final Method method = getMethod(clazz, names, classes);
        method.setAccessible(true);
        try {
            return (T)method.invoke(object, args);
        }
        catch (IllegalAccessException | InvocationTargetException ex2) {
            ReflectiveOperationException e = null;
            e.printStackTrace();
            return null;
        }
    }

    public static Method getMethod(final Class<?> clazz, final String[] names, final Class<?>[] args) {
        Method method = null;
        for (final String name : names) {
            if (method != null) {
                break;
            }
            try {
                method = clazz.getDeclaredMethod(name, args);
            }
            catch (NoSuchMethodException ex) {}
        }
        return method;
    }
}
