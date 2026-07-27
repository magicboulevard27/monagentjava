# Approval Policies and Emergency Access

## Normal Approval Flow

- Use the standard approval workflow for rollback, restart, scaling, configuration change, and failover actions.
- Require the appropriate operator or approver role before execution.
- Record the approval decision in the audit log.

## Emergency Access

- Emergency access is reserved for time-sensitive operational recovery.
- Use the minimum role and minimum scope required for the action.
- Capture why the emergency path was used and who approved it.

## Safety Rules

- Do not bypass approval for risky production actions unless the policy explicitly allows it.
- Keep emergency use rare and documented.
- Review repeated emergency access as an operational issue.
