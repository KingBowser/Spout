package org.spout.api.component.entity;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.spout.api.Client;
import org.spout.api.Server;
import org.spout.api.entity.Player;
import org.spout.api.event.ProtocolEvent;
import org.spout.api.geo.discrete.Point;
import org.spout.api.protocol.ClientSession;
import org.spout.api.protocol.Message;
import org.spout.api.protocol.ServerSession;
import org.spout.api.protocol.Session;

/**
 * The networking behind {@link org.spout.api.entity.Player}s. This component holds the {@link Session} which is the connection
 * the Player has to the server.
 */
public abstract class PlayerNetworkComponent extends NetworkComponent {
	private AtomicReference<Session> session = new AtomicReference<>(null);

	@Override
	public final void onAttached() {
		if (!(getOwner() instanceof Player)) {
			throw new IllegalStateException("The PlayerNetworkComponent may only be given to Players");
		}
	}

	/**
	 * Returns the {@link Session} representing the connection to the server.
	 * @return The session
	 */
	public final Session getSession() {
		return session.get();
	}

	/**
	 * Sets the session this Player has to the server.
	 * @param session The session to the server
	 */
	public final void setSession(Session session) {
		if (getEngine() instanceof Client && !(session instanceof ClientSession)) {
			throw new IllegalStateException("The client may only have a ClientSession");
		}

		if (getEngine() instanceof Server && !(session instanceof ServerSession)) {
			throw new IllegalStateException("The server may only have a ServerSession");
		}

		if (!this.session.compareAndSet(null, session)) {
			throw new IllegalStateException("Once set, the session may not be re-set until a new connection is made");
		}
	}

	/**
	 * Calls a {@link ProtocolEvent} for all {@link Player}s within sync distance of the owning Player.
	 * <p/>
	 * This method also sends the protocol event to the owning Player as well.
	 *
	 * @param event to send
	 */
	public void callProtocolEvent(final ProtocolEvent event) {
		callProtocolEvent(event, false);
	}

	/**
	 * Calls a {@link ProtocolEvent} for all {@link Player}s within sync distance of the owning Player.
	 *
	 * @param event to send
	 * @param ignoreOwner True to ignore the owning Player, false to include
	 */
	public void callProtocolEvent(final ProtocolEvent event, final boolean ignoreOwner) {
		final List<Player> players = getOwner().getWorld().getPlayers();
		final Point position = getOwner().getPhysics().getPosition();
		final List<Message> messages = getEngine().getEventManager().callEvent(event).getMessages();

		for (final Player player : players) {
			if (ignoreOwner && getOwner() == player) {
				continue;
			}
			final Point otherPosition = player.getPhysics().getPosition();
			//TODO: Verify this math
			if (position.subtract(otherPosition).fastLength() > getOwner().getNetwork().getSyncDistance()) {
				continue;
			}
			for (final Message message : messages) {
				player.getNetwork().getSession().send(false, message);
			}
		}
	}

	/**
	 * Calls a {@link ProtocolEvent} for all {@link Player}s provided.
	 *
	 * @param event to send
	 * @param players to send to
	 */
	public void callProtocolEvent(final ProtocolEvent event, final Player... players) {
		final List<Message> messages = getEngine().getEventManager().callEvent(event).getMessages();
		for (final Player player : players) {
			for (final Message message : messages) {
				player.getNetwork().getSession().send(false, message);
			}
		}
	}
}
