#!/usr/bin/env python3
p = "claims-data/service/src/bddTest/java/uk/gov/justice/laa/dstew/payments/claimsdata/bdd/steps/AmendmentsRequestContractSteps.java"
s = open(p).read()

old = '''  // NOTE: "no outbound PDA call was made" is defined by AmendmentPdaTriggerSteps as a
  // log-only spec-guard step (final verification is owned by DSTEW-1773). Reusing it here via
  // Cucumber's cross-class step registry — do NOT redefine. The DS1751_2 assertions on
  // "no persistence was attempted for this claim" (defined below) already cover the observable
  // side-effect this scenario cares about.

  @Then("no outbound FSP call was made")
  public void noOutboundFspCallWasMade() {
    step(
        "Asserting no outbound FSP call was made \u2014 inferred from the same transactional invariant "
            + "as the PDA assertion",
        () -> {
          assertVersionUnchanged();
          assertNoClaimAmendmentRow();
        });
  }

'''

new = '''  // NOTE: "no outbound PDA call was made" is defined by AmendmentPdaTriggerSteps and
  // "no outbound FSP call was made" is defined by AmendmentsEligibilityGateSteps (both on main
  // via PRs #449/#450/#452). DSTEW-1751 used to carry local log-only redefinitions that inferred
  // "no outbound call" from the transactional invariant (version unchanged + no amendment row);
  // those are strictly weaker than the real Mockito verify now on main, so the local copies have
  // been removed post-rebase and DSTEW-1751 scenarios now use the stronger main-owned steps.
  // The "no persistence was attempted for this claim" step below still covers the transactional
  // invariant, which is what the DS1751_* scenarios care about.

'''

assert old in s, "old block not found verbatim"
s = s.replace(old, new)
open(p, "w").write(s)
print("OK")

