package org.girino.frac.android.foss;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

/**
 * Help / About copy in strings.xml (issue #22).
 * Plain JUnit — Robolectric runs with includeAndroidResources=false in this project.
 */
public class InfoScreenTest {

    private static String stringsXml() throws IOException {
        Path path = Path.of("src/main/res/values/strings.xml");
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    @Test
    public void helpBody_containsUsageSections() throws IOException {
        String xml = stringsXml();
        assertTrue(xml.contains("name=\"help_body\""));
        assertTrue(xml.contains("Pan"));
        assertTrue(xml.contains("Export"));
        assertTrue(xml.contains("Privacy"));
    }

    @Test
    public void aboutBodyHtml_containsGalAndLicenseUrl() throws IOException {
        String xml = stringsXml();
        assertTrue(xml.contains("name=\"about_body_html\""));
        assertTrue(xml.contains("Girino Anarchist License (GAL)"));
        assertTrue(xml.contains("Copyright"));
        assertTrue(xml.contains("Girino Vey"));
        assertTrue(xml.contains("license.girino.org"));
        assertTrue(xml.contains("github.com/girino/android-mandelbrot-set"));
    }
}
