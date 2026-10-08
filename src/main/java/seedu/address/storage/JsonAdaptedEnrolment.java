package seedu.address.storage;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Section;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.enrolment.Team;

/** Represents the human-editable JSON form of one enrolment. */
class JsonAdaptedEnrolment {
    private final String module;
    private final String semester;
    private final String section;
    private final String team;

    /** Creates the JSON adapter; omitted or null optional affiliations represent absence. */
    public JsonAdaptedEnrolment(String module, String semester, String section, String team) {
        this.module = module;
        this.semester = semester;
        this.section = section;
        this.team = team;
    }

    /** Reads JSON strings without coercing numbers, booleans, arrays, or objects into valid-looking labels. */
    @JsonCreator
    public JsonAdaptedEnrolment(@JsonProperty("module") JsonNode module, @JsonProperty("semester") JsonNode semester,
            @JsonProperty("section") JsonNode section, @JsonProperty("team") JsonNode team) {
        this(readString(module), readString(semester), readString(section), readString(team));
    }

    /** Converts a validated enrolment to its canonical stored representation. */
    public JsonAdaptedEnrolment(Enrolment source) {
        module = source.getModuleCode().value;
        semester = source.getSemester().value;
        section = source.getSection().map(value -> value.value).orElse(null);
        team = source.getTeam().map(value -> value.value).orElse(null);
    }

    private static String readString(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw new IllegalArgumentException("Enrolment fields must be strings or null optional affiliations.");
        }
        return node.textValue();
    }

    /** Returns a validated enrolment without silently correcting non-canonical stored values. */
    public Enrolment toModelType() throws IllegalValueException {
        if (module == null || semester == null) {
            throw new IllegalValueException("Enrolment module and semester are required.");
        }
        try {
            ModuleCode modelModule = new ModuleCode(module);
            Semester modelSemester = new Semester(semester);
            Optional<Section> modelSection = section == null ? Optional.empty() : Optional.of(new Section(section));
            Optional<Team> modelTeam = team == null ? Optional.empty() : Optional.of(new Team(team));
            if (!module.equals(modelModule.value) || !semester.equals(modelSemester.value)
                    || modelSection.isPresent() && !section.equals(modelSection.get().value)
                    || modelTeam.isPresent() && !team.equals(modelTeam.get().value)) {
                throw new IllegalValueException("Stored enrolment values must already use their canonical format.");
            }
            return new Enrolment(modelModule, modelSemester, modelSection, modelTeam);
        } catch (IllegalArgumentException e) {
            throw new IllegalValueException(e.getMessage());
        }
    }
}
