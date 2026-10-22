package seedu.address.ui;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

/** Opt-in JavaFX check that direct card construction preserves the previous FXML layout and complete content. */
public class PersonCardAcceptance {
    private static int assertions;

    public static void main(String[] args) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.startup(() -> {
            try {
                verify(false);
                verify(true);
                done.complete(null);
            } catch (Throwable failure) {
                done.completeExceptionally(failure);
            }
        });
        try {
            done.get(30, TimeUnit.SECONDS);
            System.out.println("PASS card layout/content assertions: " + assertions);
        } finally {
            Platform.exit();
        }
    }

    private static void verify(boolean isFullProfile) throws Exception {
        PersonBuilder builder = new PersonBuilder().withName("Student With A Long Name").withSample(isFullProfile);
        if (isFullProfile) {
            builder.withTelegram("Long_Contact").withGitHub("some-repo").withTags("zeta", "alpha")
                    .withRemark("A complete remark")
                    .withEnrolments(PersonCardTest.enrolment("CS2113T", "AY26/27 S2", null, null),
                            PersonCardTest.enrolment("CS2103T", "AY26/27 S1", "T12", "SEED"));
        }
        Person person = builder.build();
        Map<String, String> expected = Map.of(
                "id", "500. ", "name", "Student With A Long Name", "email", "NUS email: amy.bee@u.nus.edu",
                "telegram", isFullProfile ? "Telegram: Long_Contact" : "Telegram: Not provided",
                "github", isFullProfile ? "GitHub: some-repo" : "GitHub: Not provided",
                "sample", "Fictional sample", "remark", isFullProfile ? "A complete remark" : "",
                "enrolments", isFullProfile ? "Enrolments: 2\nCS2103T | AY26/27 S1 | Section: T12 | Team: SEED"
                        + "\nCS2113T | AY26/27 S2 | Section: Not assigned | Team: Not assigned" : "Enrolments: none");
        FXMLLoader loader = new FXMLLoader(PersonCardAcceptance.class.getResource("/view/PersonCard/legacy.fxml"));
        Region legacy = loader.load();
        expected.forEach((id, text) -> ((Label) loader.getNamespace().get(id)).setText(text));
        Label sample = (Label) loader.getNamespace().get("sample");
        sample.setVisible(isFullProfile);
        sample.setManaged(isFullProfile);
        if (isFullProfile) {
            FlowPane tags = (FlowPane) loader.getNamespace().get("tags");
            tags.getChildren().addAll(new Label("alpha"), new Label("zeta"));
        }
        PersonCard card = new PersonCard(person, 500);
        same(person, card.person, "profile identity");
        compare(legacy, card.getRoot());
    }

    private static void compare(Node expected, Node actual) {
        same(expected.getClass(), actual.getClass(), "node type");
        same(expected.getId(), actual.getId(), "CSS id");
        same(expected.getStyleClass(), actual.getStyleClass(), "style classes");
        same(expected.isVisible(), actual.isVisible(), "visibility");
        same(expected.isManaged(), actual.isManaged(), "layout participation");
        same(HBox.getHgrow(expected), HBox.getHgrow(actual), "horizontal growth");
        same(GridPane.getColumnIndex(expected), GridPane.getColumnIndex(actual), "grid column");
        if (expected instanceof Region left && actual instanceof Region right) {
            same(List.of(left.getMinWidth(), left.getMinHeight(), left.getPrefWidth(), left.getPrefHeight(),
                    left.getMaxWidth(), left.getMaxHeight(), left.getPadding()),
                    List.of(right.getMinWidth(), right.getMinHeight(), right.getPrefWidth(), right.getPrefHeight(),
                    right.getMaxWidth(), right.getMaxHeight(), right.getPadding()), "region dimensions");
        }
        if (expected instanceof Label left && actual instanceof Label right) {
            same(left.getText(), right.getText(), "complete label text");
            same(left.isWrapText(), right.isWrapText(), "text wrapping");
        }
        if (expected instanceof HBox left && actual instanceof HBox right) {
            same(left.getSpacing(), right.getSpacing(), "heading spacing");
            same(left.getAlignment(), right.getAlignment(), "heading alignment");
        }
        if (expected instanceof VBox left && actual instanceof VBox right) {
            same(left.getSpacing(), right.getSpacing(), "content spacing");
            same(left.getAlignment(), right.getAlignment(), "content alignment");
        }
        if (expected instanceof GridPane left && actual instanceof GridPane right) {
            same(left.getColumnConstraints().toString(), right.getColumnConstraints().toString(), "column constraints");
        }
        if (expected instanceof Parent left && actual instanceof Parent right) {
            same(left.getChildrenUnmodifiable().size(), right.getChildrenUnmodifiable().size(), "child count");
            for (int i = 0; i < left.getChildrenUnmodifiable().size(); i++) {
                compare(left.getChildrenUnmodifiable().get(i), right.getChildrenUnmodifiable().get(i));
            }
        }
    }

    private static void same(Object expected, Object actual, String description) {
        assertions++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(description + ": expected " + expected + ", got " + actual);
        }
    }
}
