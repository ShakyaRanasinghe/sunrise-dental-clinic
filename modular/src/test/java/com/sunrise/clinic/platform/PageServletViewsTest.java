package com.sunrise.clinic.platform;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Every view name a servlet renders must resolve to a file.
 *
 * <p>Written after a real failure. {@code PageServlet.fail} rendered {@code "error"} while
 * the view was {@code shared/error.jsp}, so the forward missed - and a missing view is not
 * an exception, it is a silent 404 from the container. Every page-level error therefore
 * answered "That address does not exist" whatever had actually happened: a 403 looked like
 * a missing page, and a database failure during this step's own testing looked like one
 * too, which sent the diagnosis down the wrong path for a while.</p>
 *
 * <p>Nothing else catches this. The compiler cannot check a string, and the servlet tests
 * do not run a container. Reading the source is cheap and finds it exactly.</p>
 */
class PageServletViewsTest {

    private static final Path SOURCE = Path.of("src/main/java");
    private static final Path VIEWS = Path.of("src/main/webapp/WEB-INF/jsp");

    /** {@code render(request, response, "some/view")} */
    private static final Pattern RENDERED =
            Pattern.compile("render\\(\\s*request,\\s*response,\\s*\"([^\"]+)\"");

    @Test
    void everyRenderedViewExists() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String view : renderedViewNames()) {
            if (!Files.exists(VIEWS.resolve(view + ".jsp"))) {
                missing.add(view + ".jsp");
            }
        }
        assertEquals(List.of(), missing, "these view names resolve to no file");
    }

    @Test
    void viewNamesAreRelativeAndCarryNoExtension() throws IOException {
        // render() prepends /WEB-INF/jsp/ and appends .jsp, so a leading slash or a
        // trailing .jsp produces a path that cannot resolve.
        for (String view : renderedViewNames()) {
            assertFalse(view.startsWith("/"), view + " must not start with a slash");
            assertFalse(view.endsWith(".jsp"), view + " must not carry an extension");
        }
    }

    @Test
    void theSourceScanFoundSomething() {
        // A guard on the test itself: if the pattern stopped matching, the two tests
        // above would pass by finding nothing at all.
        assertFalse(renderedViewNamesQuietly().isEmpty(),
                "the scan matched no render() call, so it is no longer testing anything");
    }

    private static List<String> renderedViewNames() throws IOException {
        List<String> names = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCE)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher matcher = RENDERED.matcher(Files.readString(file));
                while (matcher.find()) {
                    names.add(matcher.group(1));
                }
            }
        }
        return names;
    }

    private static List<String> renderedViewNamesQuietly() {
        try {
            return renderedViewNames();
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
