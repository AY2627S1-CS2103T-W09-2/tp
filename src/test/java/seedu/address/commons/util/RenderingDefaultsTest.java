package seedu.address.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.commons.util.RenderingDefaults.LINUX_PRISM_ORDER;
import static seedu.address.commons.util.RenderingDefaults.PRISM_ORDER_PROPERTY;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RenderingDefaultsTest {
    private String originalPrismOrder;

    @BeforeEach
    public void clearPrismOrder() {
        originalPrismOrder = System.getProperty(PRISM_ORDER_PROPERTY);
        System.clearProperty(PRISM_ORDER_PROPERTY);
    }

    @AfterEach
    public void restorePrismOrder() {
        if (originalPrismOrder == null) {
            System.clearProperty(PRISM_ORDER_PROPERTY);
        } else {
            System.setProperty(PRISM_ORDER_PROPERTY, originalPrismOrder);
        }
    }

    @Test
    public void configurePipeline_linuxWithoutExplicitOrder_selectsSoftwarePipeline() {
        assertTrue(RenderingDefaults.configurePipeline("Linux"));
        assertEquals(LINUX_PRISM_ORDER, System.getProperty(PRISM_ORDER_PROPERTY));
    }

    @Test
    public void configurePipeline_linuxWithExplicitOrder_preservesExplicitOrder() {
        for (String explicitOrder : new String[] {"es2", ""}) {
            System.setProperty(PRISM_ORDER_PROPERTY, explicitOrder);
            assertFalse(RenderingDefaults.configurePipeline("Linux"));
            assertEquals(explicitOrder, System.getProperty(PRISM_ORDER_PROPERTY));
        }
    }

    @Test
    public void configurePipeline_windowsAndMac_leaveJavaFxDefaults() {
        assertFalse(RenderingDefaults.configurePipeline("Windows 11"));
        assertFalse(RenderingDefaults.configurePipeline("Mac OS X"));
        assertNull(System.getProperty(PRISM_ORDER_PROPERTY));

        System.setProperty(PRISM_ORDER_PROPERTY, "es2");
        assertFalse(RenderingDefaults.configurePipeline("Windows 11"));
        assertFalse(RenderingDefaults.configurePipeline("Mac OS X"));
        assertEquals("es2", System.getProperty(PRISM_ORDER_PROPERTY));
    }

    @Test
    public void configurePipeline_repeatedOnLinux_keepsSoftwarePipeline() {
        assertTrue(RenderingDefaults.configurePipeline("Linux"));
        assertFalse(RenderingDefaults.configurePipeline("Linux"));
        assertEquals(LINUX_PRISM_ORDER, System.getProperty(PRISM_ORDER_PROPERTY));
    }

    @Test
    public void configurePipeline_currentJvm_appliesDefaultOnlyOnLinux() {
        boolean isLinux = RenderingDefaults.isLinux(System.getProperty("os.name"));
        assertEquals(isLinux, RenderingDefaults.configurePipeline());
        assertEquals(isLinux ? LINUX_PRISM_ORDER : null, System.getProperty(PRISM_ORDER_PROPERTY));
    }

    @Test
    public void isLinux_matchesJavaFxPlatformCheck() {
        assertTrue(RenderingDefaults.isLinux("Linux"));
        assertFalse(RenderingDefaults.isLinux("linux"));
        assertFalse(RenderingDefaults.isLinux("Windows 11"));
        assertFalse(RenderingDefaults.isLinux("Mac OS X"));
        assertFalse(RenderingDefaults.isLinux(null));
    }
}
