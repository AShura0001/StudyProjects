package lavs.ui;

import javafx.fxml.FXML;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Checks every screen's layout against its controller without starting JavaFX: each
 * {@code onAction="#name"} must have a matching method, and each {@code @FXML} field
 * in the controller must have a control with that {@code fx:id}. A mismatch makes a
 * button do nothing or a screen fail with a NullPointerException when it opens.
 */
class ScreenLayoutTest {

    private static final Path VIEWS = Path.of("src", "main", "resources", "lavs", "ui", "view");

    @TestFactory
    Stream<DynamicTest> everyScreenMatchesItsController() throws IOException {
        List<Path> layouts;
        try (Stream<Path> files = Files.list(VIEWS)) {
            layouts = files.filter(p -> p.toString().endsWith(".fxml")).sorted().toList();
        }
        assertFalse(layouts.isEmpty(), "no layouts found in " + VIEWS);
        return layouts.stream().map(layout -> DynamicTest.dynamicTest(layout.getFileName().toString(),
                () -> check(layout)));
    }

    private static void check(Path layout) throws Exception {
        Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(layout.toFile()).getDocumentElement();
        String controllerName = root.getAttribute("fx:controller");
        if (controllerName.isEmpty()) {
            return;
        }
        Class<?> controller = Class.forName(controllerName, false, ScreenLayoutTest.class.getClassLoader());

        Set<String> ids = new HashSet<>();
        List<String> handlers = new ArrayList<>();
        collect(root, ids, handlers);

        List<String> problems = new ArrayList<>();
        for (String handler : handlers) {
            if (!hasMethod(controller, handler)) {
                problems.add("no method " + handler + "() for an onAction/on... handler");
            }
        }
        for (Class<?> type = controller; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.isAnnotationPresent(FXML.class) && !ids.contains(field.getName())) {
                    problems.add("no control with fx:id=\"" + field.getName() + "\" for the @FXML field");
                }
            }
        }
        assertEquals(List.of(), problems, layout.getFileName() + " against " + controller.getSimpleName());
    }

    private static void collect(Node node, Set<String> ids, List<String> handlers) {
        if (node instanceof Element element) {
            NamedNodeMap attributes = element.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                String name = attribute.getNodeName();
                String value = attribute.getNodeValue();
                if (name.equals("fx:id")) {
                    ids.add(value);
                } else if (name.startsWith("on") && value.startsWith("#")) {
                    handlers.add(value.substring(1));
                }
            }
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            collect(children.item(i), ids, handlers);
        }
    }

    private static boolean hasMethod(Class<?> controller, String name) {
        for (Class<?> type = controller; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() <= 1) {
                    return true;
                }
            }
        }
        return false;
    }
}
