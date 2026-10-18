package seedu.address.commons.util;

/**
 * Chooses the JavaFX rendering pipeline default before JavaFX initialises.
 *
 * <p>{@code prism.order} is an implementation-specific JavaFX system property. The bundled JavaFX 17.0.7 reads it
 * once, when its Prism settings initialise, and otherwise tries the OpenGL ES2 pipeline first on Linux. That
 * pipeline's Mesa driver can use a shader cache outside the application folder.
 */
public final class RenderingDefaults {
    static final String PRISM_ORDER_PROPERTY = "prism.order";
    static final String LINUX_PRISM_ORDER = "sw";

    private RenderingDefaults() {
    }

    /**
     * Selects the software pipeline on Linux unless {@code prism.order} was supplied explicitly.
     * Other operating systems keep JavaFX's defaults. Must run before JavaFX starts.
     *
     * @return true if the Linux default was applied.
     */
    public static boolean configurePipeline() {
        return configurePipeline(System.getProperty("os.name"));
    }

    static boolean configurePipeline(String osName) {
        if (!isLinux(osName) || System.getProperty(PRISM_ORDER_PROPERTY) != null) {
            return false;
        }
        System.setProperty(PRISM_ORDER_PROPERTY, LINUX_PRISM_ORDER);
        return true;
    }

    /** Uses the same check as the bundled JavaFX platform detection. */
    static boolean isLinux(String osName) {
        return osName != null && osName.startsWith("Linux");
    }
}
