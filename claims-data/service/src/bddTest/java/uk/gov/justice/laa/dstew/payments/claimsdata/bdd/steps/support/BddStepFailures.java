package uk.gov.justice.laa.dstew.payments.claimsdata.bdd.steps.support;

/**
 * Wrapper utility that turns every step definition into a two-line safety net:
 *
 * <ol>
 *   <li>the actual work (assertion, HTTP call, fixture seeding), and
 *   <li>a friendly failure message the SDET reading the Cucumber HTML report can act on without
 *       reading Java stack traces.
 * </ol>
 *
 * <p>When a step body throws, the wrapper rethrows an {@link AssertionError} in the form
 *
 * <pre>[BDD step failed] &lt;contextDescription&gt; — &lt;friendly cause summary&gt;</pre>
 *
 * with the original throwable chained ({@code initCause}) so the JUnit XML still carries the full
 * stack trace for the deep-debug case. AssertJ {@link AssertionError}s already carry a useful
 * message from the {@code .as("...")} description — the wrapper prefixes them with the same
 * contextual sentence so the "which step" and "which assertion" contexts stay side by side.
 *
 * <p>Both overloads use functional interfaces that allow {@code throws Exception}, so step lambdas
 * calling e.g. {@link
 * uk.gov.justice.laa.dstew.payments.claimsdata.bdd.steps.support.BddApiStepSupport#getClaimHistory(java.util.UUID)}
 * (which declares {@link java.io.IOException}) don't force each step method to swallow-then-rethrow
 * the checked exception.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @When("I request the claim history timeline")
 * public void iRequestTheClaimHistoryTimeline() {
 *   BddStepFailures.step(
 *       "Requesting claim history timeline for claim " + currentClaimId,
 *       () -> lastHistoryResponse = api.getClaimHistory(currentClaimId));
 * }
 * }</pre>
 *
 * <p>Standing rule (enforced by convention across the {@code bdd/steps} package): every step
 * definition wraps its body in {@link #step(String, ThrowingRunnable)} (or the {@link
 * ThrowingSupplier} overload) so a failing assertion or thrown exception surfaces in the JUnit XML
 * / Cucumber HTML report as {@code [BDD step failed] <context> — <cause>} rather than an opaque
 * stack trace lacking scenario context.
 */
public final class BddStepFailures {

  private BddStepFailures() {
    // Utility class - no instances.
  }

  /** Runnable that may throw any exception, including checked types. */
  @FunctionalInterface
  public interface ThrowingRunnable {
    void run() throws Exception;
  }

  /** Supplier that may throw any exception, including checked types. */
  @FunctionalInterface
  public interface ThrowingSupplier<T> {
    T get() throws Exception;
  }

  /**
   * Executes {@code body} and, on any failure, rethrows an {@link AssertionError} whose first line
   * describes {@code contextDescription}.
   *
   * @param contextDescription a plain-English sentence starting with the verb + noun of what the
   *     step is doing (e.g. "Verifying AMENDMENT metadata field 'requested_by_code' equals
   *     'PROVIDER' for claim {claimId}"). Include the scenario-scoped identifiers that make the
   *     failure diagnosable at a glance.
   * @param body the actual step work — assertions, HTTP calls, DB seeding.
   */
  public static void step(String contextDescription, ThrowingRunnable body) {
    try {
      body.run();
    } catch (Exception | AssertionError cause) {
      throw rethrowAsAssertion(contextDescription, cause);
    }
  }

  /**
   * Same as {@link #step(String, ThrowingRunnable)} but for step bodies that need to return a value
   * (e.g. capturing a seeded row into a local variable while still benefiting from the wrapper).
   */
  public static <T> T step(String contextDescription, ThrowingSupplier<T> body) {
    try {
      return body.get();
    } catch (Exception | AssertionError cause) {
      throw rethrowAsAssertion(contextDescription, cause);
    }
  }

  private static AssertionError rethrowAsAssertion(String contextDescription, Throwable cause) {
    String prefix = "[BDD step failed] " + contextDescription + " — ";
    String message;
    if (cause instanceof AssertionError) {
      // Preserve AssertJ's original .as("...") description verbatim; the wrapper only prepends
      // the outer step context.
      message = prefix + safeMessage(cause);
    } else {
      message = prefix + friendlyCauseSummary(cause);
    }
    AssertionError wrapped = new AssertionError(message);
    wrapped.initCause(cause);
    return wrapped;
  }

  private static String friendlyCauseSummary(Throwable cause) {
    String type = cause.getClass().getSimpleName();
    String message = safeMessage(cause);
    if (message.isBlank()) {
      return "unexpected " + type + " (no message provided by the underlying exception).";
    }
    return "unexpected " + type + ": " + message;
  }

  private static String safeMessage(Throwable t) {
    String m = t.getMessage();
    return m == null ? "" : m;
  }
}
