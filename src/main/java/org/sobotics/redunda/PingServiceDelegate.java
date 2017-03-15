package org.sobotics.redunda;

public interface PingServiceDelegate {
	/**
	 * Tells the delegate, that the status was changed
	 * */
	void standbyStatusChanged(boolean newStatus);
}
