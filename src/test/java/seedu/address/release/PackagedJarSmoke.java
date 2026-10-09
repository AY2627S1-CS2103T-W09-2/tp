package seedu.address.release;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import seedu.address.MainApp;
import seedu.address.model.Model;
import seedu.address.model.person.Person;

/**
 * Opt-in packaged-classpath smoke check. Run seed and restart in separate JVMs in a disposable directory.
 * The plain main class permits a JDK without preinstalled JavaFX to load JavaFX from the tested JAR.
 */
public final class PackagedJarSmoke {
    private PackagedJarSmoke() {
    }

    /** Runs one production startup, command-box scenario and clean shutdown. */
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !(args[0].equals("seed") || args[0].equals("restart"))) {
            throw new IllegalArgumentException("Use seed or restart in a clean disposable working directory.");
        }
        String phase = args[0];
        Path file = Path.of("data", "addressbook.json");
        if (phase.equals("seed") && Files.exists(file)) {
            throw new IllegalStateException("Seed refuses to overwrite an existing roster.");
        }
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            Platform.runLater(() -> {
                SmokeApp app = new SmokeApp();
                Stage stage = new Stage();
                try {
                    app.init();
                    app.start(stage);
                    check(stage.isShowing(), "production window opens");
                    if (phase.equals("seed")) {
                        check(app.roster().getAddressBook().getPersonList().isEmpty(), "startup is empty");
                        submit(stage, "student add /name Alex Demo /email E9000001@U.NUS.EDU "
                                + "/telegram @Alex_Demo /github alex-demo");
                        check(feedback(stage).equals("Added student: Alex Demo."), "creation succeeds");
                    } else {
                        check(app.roster().getAddressBook().getPersonList().size() == 1, "new process reloads profile");
                        Person person = app.roster().getAddressBook().getPersonList().getFirst();
                        check(person.getName().fullName.equals("Alex Demo")
                                && person.getEmail().value.equals("e9000001@u.nus.edu")
                                && person.getTelegram().orElseThrow().value.equals("Alex_Demo")
                                && person.getGitHub().orElseThrow().value.equals("alex-demo")
                                && !person.isSample(), "complete persisted profile reloads");
                    }
                    byte[] saved = Files.readAllBytes(file);
                    submit(stage, "find e9000001");
                    check(feedback(stage).equals("1 student found for \"e9000001\"."),
                            "representative search succeeds");
                    check(Arrays.equals(saved, Files.readAllBytes(file)), "search preserves saved bytes");
                    WritableImage snapshot = stage.getScene().snapshot(null);
                    BufferedImage image = new BufferedImage((int) snapshot.getWidth(), (int) snapshot.getHeight(),
                            BufferedImage.TYPE_INT_ARGB);
                    for (int y = 0; y < image.getHeight(); y++) {
                        for (int x = 0; x < image.getWidth(); x++) {
                            image.setRGB(x, y, snapshot.getPixelReader().getArgb(x, y));
                        }
                    }
                    ImageIO.write(image, "png", Path.of("smoke-" + phase + ".png").toFile());
                    submit(stage, "exit");
                    check(!stage.isShowing(), "exit hides production window");
                    app.stop();
                    check(Arrays.equals(saved, Files.readAllBytes(file)), "shutdown preserves roster bytes");
                    System.out.println("PASS packaged JAR " + phase + "; " + System.getProperty("java.runtime.version")
                            + "; " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
                } catch (Exception | Error e) {
                    failure.set(e);
                    stage.close();
                } finally {
                    Platform.exit();
                    finished.countDown();
                }
            });
        });
        if (!finished.await(30, TimeUnit.SECONDS)) {
            throw new AssertionError("Timed out waiting for smoke check");
        }
        if (failure.get() != null) {
            throw new AssertionError("Packaged JAR smoke failed", failure.get());
        }
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static String feedback(Stage stage) {
        return ((TextArea) stage.getScene().lookup("#resultDisplay")).getText();
    }

    private static void submit(Stage stage, String command) {
        System.out.println("COMMAND " + command);
        TextField input = (TextField) stage.getScene().lookup("#commandTextField");
        input.setText(command);
        input.fireEvent(new ActionEvent());
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
        System.out.println("RESULT " + feedback(stage));
    }

    private static class SmokeApp extends MainApp {
        Model roster() {
            return model;
        }
    }
}
