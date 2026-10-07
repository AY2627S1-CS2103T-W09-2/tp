package seedu.address.model.enrolment;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/** Represents an immutable module-semester context, owned by the containing student profile. */
public final class Enrolment {
    public static final Comparator<Enrolment> DISPLAY_ORDER = Comparator
            .comparingInt((Enrolment enrolment) -> enrolment.getSemester().getStartYear())
            .thenComparingInt(enrolment -> enrolment.getSemester().getSemesterNumber())
            .thenComparing(enrolment -> enrolment.getModuleCode().value);

    private final ModuleCode moduleCode;
    private final Semester semester;
    private final Optional<Section> section;
    private final Optional<Team> team;

    /** Creates an enrolment with no assigned section or team. */
    public Enrolment(ModuleCode moduleCode, Semester semester) {
        this(moduleCode, semester, Optional.empty(), Optional.empty());
    }

    /** Creates an enrolment with explicit optional affiliations; no display labels are stored. */
    public Enrolment(ModuleCode moduleCode, Semester semester, Optional<Section> section, Optional<Team> team) {
        requireAllNonNull(moduleCode, semester, section, team);
        this.moduleCode = moduleCode;
        this.semester = semester;
        this.section = section;
        this.team = team;
    }

    public ModuleCode getModuleCode() {
        return moduleCode;
    }

    public Semester getSemester() {
        return semester;
    }

    public Optional<Section> getSection() {
        return section;
    }

    public Optional<Team> getTeam() {
        return team;
    }

    /** Returns whether the module-semester key matches, independently of affiliations. */
    public boolean hasSameKey(Enrolment other) {
        return other != null && moduleCode.equals(other.moduleCode) && semester.equals(other.semester);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Enrolment enrolment && hasSameKey(enrolment)
                && section.equals(enrolment.section) && team.equals(enrolment.team);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleCode, semester, section, team);
    }

    @Override
    public String toString() {
        return moduleCode + " | " + semester + " | " + section + " | " + team;
    }
}
