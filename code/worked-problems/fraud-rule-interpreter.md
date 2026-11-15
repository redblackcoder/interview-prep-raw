# Fraud rule interpreter

Apply ordered accept and block rules over string and Boolean attributes.

## Worked solution

Tokenize quoted literals as indivisible values so an operator inside a string cannot split a rule. Parse the leading action and condition. A primary is a Boolean field, field equality, or a parenthesized expression. Combine primaries under AND and then combine those groups under OR.

Evaluate equality case-sensitively and short-circuit Boolean nodes. Walk rules in input order and return immediately on a true predicate. Keep the no-match action configurable. A literal such as "rock AND roll" remains one value even inside a nested predicate.
