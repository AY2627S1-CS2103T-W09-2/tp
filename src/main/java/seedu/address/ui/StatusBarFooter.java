package seedu.address.ui;

import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

/**
 * A UI for the status bar that is displayed at the footer of the application.
 */
public class StatusBarFooter extends UiPart<Region> {

    static final String DELETION_PENDING = "Deletion pending";

    private static final String FXML = "StatusBarFooter.fxml";

    @FXML
    private Label saveLocationStatus;

    @FXML
    private Label deletionStatus;

    /**
     * Creates a {@code StatusBarFooter} with the given {@code Path}.
     */
    public StatusBarFooter(Path saveLocation) {
        this(saveLocation, false);
    }

    /** Creates a persistent storage status and file-location display. */
    public StatusBarFooter(Path saveLocation, boolean isReadOnly) {
        super(FXML);
        saveLocationStatus.setText((isReadOnly ? "Storage unavailable — read-only recovery | " : "Storage available | ")
                + Paths.get(".").resolve(saveLocation));
    }

    /** Shows {@code Deletion pending} while a deletion preview awaits confirmation, and nothing otherwise. */
    public void setDeletionPending(boolean isDeletionPending) {
        deletionStatus.setText(isDeletionPending ? DELETION_PENDING : "");
    }

}
