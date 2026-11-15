# Conversational model serving

Stream responses for multi-turn conversations under a fixed inference budget.

## Worked design

Append the user turn with a stable request ID. Assemble context within a token budget, admit work to a bounded model queue, and route it to an inference worker. Stream numbered output chunks through a session channel to the connected gateway.

Persist the final assistant turn and terminal state. A reconnect can resume at a known chunk or fetch the final turn. Reject or degrade requests whose deadlines exceed available capacity.
