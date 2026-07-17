package com.contactfront.engine.model;

/**
 * Union of everything that can sit in the delayed-order queue.
 *
 * <p>{@link Action} is the player-facing sealed set (Milestone 1). The engine's
 * AI also queues strikes (artillery/CAS) that are not player actions; those are
 * {@link EnemyStrike}. Both are typed — there is no untyped dict in the pipeline,
 * which is the whole point of the seal.
 */
public sealed interface Command permits Action, EnemyStrike {
}
