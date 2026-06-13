package ai.herald.clientmod.skill;

/** Lifecycle states for a long-running task. */
public enum SkillStatus {
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
