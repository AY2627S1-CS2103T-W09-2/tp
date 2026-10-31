package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;

/**
 * A UI component that displays the result of a command execution.
 */
public class ResultDisplay extends UiPart<Region> {

    /** Lines shown without scrolling: a six-line deletion preview, with room for one of its lines to wrap. */
    private static final int VISIBLE_LINES = 7;

    private static final String FXML = "ResultDisplay.fxml";

    @FXML
    private TextArea resultDisplay;

    /**
     * Creates the result area. Its height is measured from its font rather than fixed in pixels, because line height
     * differs between platforms. The text area's own row count is not used, because it measures lines more tightly
     * than the text area draws them.
     */
    public ResultDisplay() {
        super(FXML);
        resultDisplay.fontProperty().addListener(unused -> fitVisibleLines());
        resultDisplay.insetsProperty().addListener(unused -> fitVisibleLines());
        resultDisplay.skinProperty().addListener(unused -> {
            if (resultDisplay.lookup(".scroll-pane") instanceof ScrollPane scroll) {
                scroll.insetsProperty().addListener(unusedInsets -> fitVisibleLines());
                scroll.skinProperty().addListener(unusedSkin -> {
                    if (resultDisplay.lookup(".content") instanceof Region content) {
                        content.insetsProperty().addListener(unusedContentInsets -> fitVisibleLines());
                    }
                    fitVisibleLines();
                });
            }
        });
    }

    public void setFeedbackToUser(String feedbackToUser) {
        requireNonNull(feedbackToUser);
        resultDisplay.setText(feedbackToUser);
    }

    /**
     * Fixes the result area's height to {@link #VISIBLE_LINES} lines of its font plus its frame. Runs again whenever
     * the font or the theme's padding changes, and does nothing until the scroll pane and its text content exist.
     */
    private void fitVisibleLines() {
        if (!(resultDisplay.lookup(".scroll-pane") instanceof Region scroll)
                || !(resultDisplay.lookup(".content") instanceof Region content)) {
            return;
        }
        Text lines = new Text("X" + "\nX".repeat(VISIBLE_LINES - 1));
        lines.setFont(resultDisplay.getFont());
        // The bounds type the text area draws its text with.
        lines.setBoundsType(TextBoundsType.LOGICAL_VERTICAL_CENTER);
        double height = Math.ceil(lines.getLayoutBounds().getHeight()
                + verticalInsets(resultDisplay) + verticalInsets(scroll) + verticalInsets(content));
        resultDisplay.setMinHeight(height);
        resultDisplay.setPrefHeight(height);
        resultDisplay.setMaxHeight(height);
    }

    private static double verticalInsets(Region region) {
        return region.snappedTopInset() + region.snappedBottomInset();
    }
}
