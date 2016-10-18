package ru.linachan.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class GenericDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(GenericDispatcher.class);

    @SuppressWarnings("unchecked")
    public static <H, I extends Map, O extends Map> void dispatch(H handler, I request, O response) {
        String action = (String) request.get("action");

        response.put("action", action);

        if (action != null) {
            for (Method method: handler.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(GenericMethod.class)) {
                    GenericMethod methodData = method.getAnnotation(GenericMethod.class);
                    if (methodData.value().equals(action)) {
                        try {
                            method.invoke(handler, request, response);
                        } catch (InvocationTargetException e) {
                            Throwable t = e.getTargetException();
                            logger.error(
                                "Unable to invoke handler method: [{}]: {}",
                                t.getClass().getSimpleName(), t.getMessage()
                            );

                            response.put("success", false);
                            response.put("notify", true);
                            response.put("message", String.format("InvocationError: %s", t.getMessage()));
                        } catch (IllegalAccessException e) {
                            logger.error(
                                "Requested method is not available: [{}]: {}",
                                e.getClass().getSimpleName(), e.getMessage()
                            );

                            response.put("success", false);
                            response.put("notify", true);
                            response.put("message", String.format("AccessError: %s", e.getMessage()));
                        }

                        return;
                    }
                }
            }

            response.put("success", false);
            response.put("notify", true);
            response.put("message",
                String.format(
                    "HandlerError: handler for action '%s' not found in %s", action, handler.getClass().getSimpleName()
                )
            );
        } else {
            response.put("success", false);
            response.put("notify", true);
            response.put("message", "action not specified");
        }
    }
}
