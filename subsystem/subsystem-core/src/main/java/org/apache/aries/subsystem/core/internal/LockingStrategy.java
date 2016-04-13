package org.apache.aries.subsystem.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemException;

public class LockingStrategy {
	private static final int TRY_LOCK_TIME = 30000;
	private static final TimeUnit TRY_LOCK_TIME_UNIT = TimeUnit.MILLISECONDS;
	
	/*
	 * A mutual exclusion lock used when acquiring the state change locks of
	 * a collection of subsystems in order to prevent cycle deadlocks.
	 */
	private static final ReentrantLock lock = new ReentrantLock();
	/*
	 * Used when the state change lock of a subsystem cannot be acquired. All
	 * other state change locks are released while waiting. The condition is met
	 * whenever the state change lock of one or more subsystems is released.
	 */
	private static final Condition condition = lock.newCondition();
	
	/*
	 * Allow only one of the following operations to be executing at the same 
	 * time.
	 * 
	 * (1) Install
	 * (2) Install Dependencies
	 * (3) Uninstall
	 * 
	 * Allow any number of the following operations to be executing at the same
	 * time.
	 * 
	 * (1) Resolve
	 * (2) Start
	 * (3) Stop
	 */
	private static final ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();
	
	private static final ThreadLocal<Map<Subsystem.State, Set<BasicSubsystem>>> local = new ThreadLocal<Map<Subsystem.State, Set<BasicSubsystem>>>() {
		@Override
		protected Map<Subsystem.State, Set<BasicSubsystem>> initialValue() {
			return new HashMap<Subsystem.State, Set<BasicSubsystem>>();
		}
	};
	
	public static void lock() {
		try {
			if (!lock.tryLock(TRY_LOCK_TIME, TRY_LOCK_TIME_UNIT)) {
				throw new SubsystemException("Unable to acquire the global mutual exclusion lock in time.");
			}
		}
		catch (InterruptedException e) {
			throw new SubsystemException(e);
		}
	}
	
	public static void unlock() {
		lock.unlock();
	}
	
	public static void lock(Collection<BasicSubsystem> subsystems) {
		Collection<BasicSubsystem> locked = new ArrayList<BasicSubsystem>(subsystems.size());
		try {
			while (locked.size() < subsystems.size()) {
				for (BasicSubsystem subsystem : subsystems) {
					if (!subsystem.stateChangeLock().tryLock()) {
						unlock(locked);
						locked.clear();
						if (!LockingStrategy.condition.await(TRY_LOCK_TIME, TimeUnit.SECONDS)) {
							throw new SubsystemException("Unable to acquire the state change lock in time: " + subsystem);
						}
						break;
					}
					locked.add(subsystem);
				}
			}
		}
		catch (InterruptedException e) {
			unlock(locked);
			throw new SubsystemException(e);
		}
	}
	
	public static void unlock(Collection<BasicSubsystem> subsystems) {
		for (BasicSubsystem subsystem : subsystems) {
			subsystem.stateChangeLock().unlock();
		}
		signalAll();
	}
	
	private static void signalAll() {
		lock();
		try {
			condition.signalAll();
		}
		finally {
			unlock();
		}
	}
	
	public static boolean set(Subsystem.State state, BasicSubsystem subsystem) {
		Map<Subsystem.State, Set<BasicSubsystem>> map = local.get();
		Set<BasicSubsystem> subsystems = map.get(state);
		if (subsystems == null) {
			subsystems = new HashSet<BasicSubsystem>();
			map.put(state, subsystems);
			local.set(map);
		}
		if (subsystems.contains(subsystem)) {
			return false;
		}
		subsystems.add(subsystem);
		return true;
	}
	
	public static void unset(Subsystem.State state, BasicSubsystem subsystem) {
		Map<Subsystem.State, Set<BasicSubsystem>> map = local.get();
		Set<BasicSubsystem> subsystems = map.get(state);
		if (subsystems != null) {
			subsystems.remove(subsystem);
		}
	}
	
	public static void readLock() {
		try {
			if (!rwlock.readLock().tryLock(TRY_LOCK_TIME, TRY_LOCK_TIME_UNIT)) {
				throw new SubsystemException("Unable to acquire the global read lock in time.");
			}
		}
		catch (InterruptedException e) {
			throw new SubsystemException(e);
		}
	}
	
	public static void readUnlock() {
		rwlock.readLock().unlock();
	}
	
	public static void writeLock() {
		try {
			if (!rwlock.writeLock().tryLock(TRY_LOCK_TIME, TRY_LOCK_TIME_UNIT)) {
				throw new SubsystemException("Unable to acquire the global write lock in time.");
			}
		}
		catch (InterruptedException e) {
			throw new SubsystemException(e);
		}
	}
	
	public static void writeUnlock() {
		rwlock.writeLock().unlock();
	}
}
