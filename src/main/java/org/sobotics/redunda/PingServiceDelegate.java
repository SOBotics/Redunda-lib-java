package org.sobotics.redunda;

public interface PingServiceDelegate {
	/**
	 * Tells the delegate, that the status was changed
	 * 
	 * @param newStatus The new status
	 * */
	void standbyStatusChanged(boolean newStatus);
}
