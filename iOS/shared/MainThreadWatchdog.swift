//
//  MainThreadWatchdog.swift
//  Shared by the pitchperfectTests and tagmasterTests bundles.
//
//  On CI the hosted test app's main thread sometimes stops for 30 s or more
//  inside whichever test is running: a different test each time, on main as
//  well as on branches, from 17 s to over 3 minutes into the run, and never
//  locally. The test log shows only the silence. While a bundle runs, this
//  prints the main thread's stack to the log whenever it has gone five seconds
//  without answering, and again while it stays stuck, so a stall names its
//  cause: the same frames each time for a wait, moving ones for busy work.
//

import Darwin
import Foundation

enum MainThreadWatchdog {
    /// How long the main thread may go without answering before its stack is printed.
    static let stallSeconds: TimeInterval = 5
    /// How often a stall that goes on is printed again, and how many times in all.
    static let repeatSeconds: TimeInterval = 10
    static let reportsPerStall = 3

    private static let state = WatchdogState()

    /// Starts watching the main thread; call it from the main thread. Later calls do nothing.
    static func start() {
        precondition(Thread.isMainThread, "the watchdog watches the thread that starts it")
        guard state.begin(watching: pthread_self()) else { return }
        // The signal handler may not allocate, so its buffer exists before any signal.
        _ = watchdogFrames
        signal(SIGUSR2, printWatchdogStack)
        let thread = Thread {
            while true {
                if state.askAgain() {
                    DispatchQueue.main.async { state.answered() }
                }
                Thread.sleep(forTimeInterval: 0.5)
                if let stall = state.stallToReport() {
                    let silence = String(format: "%.1f", stall.silence)
                    let header = "MainThreadWatchdog: the main thread has not answered for \(silence) s (\(Date())); its stack:\n"
                    FileHandle.standardError.write(Data(header.utf8))
                    pthread_kill(stall.thread, SIGUSR2)
                }
            }
        }
        thread.name = "MainThreadWatchdog"
        thread.qualityOfService = .userInteractive
        thread.start()
    }
}

private let watchdogFrameCapacity: Int32 = 128
nonisolated(unsafe) private let watchdogFrames =
    UnsafeMutablePointer<UnsafeMutableRawPointer?>.allocate(capacity: Int(watchdogFrameCapacity))

/// SIGUSR2's handler, run on the stalled main thread: prints its stack without allocating.
private func printWatchdogStack(_: Int32) {
    let count = backtrace(watchdogFrames, watchdogFrameCapacity)
    backtrace_symbols_fd(watchdogFrames, count, STDERR_FILENO)
    let end: StaticString = "MainThreadWatchdog: end of stack\n"
    _ = write(STDERR_FILENO, end.utf8Start, end.utf8CodeUnitCount)
}

/// When the main thread last answered, and how much of its current stall has been printed.
private final class WatchdogState: @unchecked Sendable {
    private let lock = NSLock()
    private var mainThread: pthread_t?
    private var lastAnswer = Date()
    private var waitingForAnswer = false
    private var reports = 0

    func begin(watching thread: pthread_t) -> Bool {
        lock.withLock {
            guard mainThread == nil else { return false }
            mainThread = thread
            lastAnswer = Date()
            return true
        }
    }

    /// Whether to ask the main thread again: not while a question is still unanswered.
    func askAgain() -> Bool {
        lock.withLock {
            guard !waitingForAnswer else { return false }
            waitingForAnswer = true
            return true
        }
    }

    func answered() {
        lock.withLock {
            lastAnswer = Date()
            waitingForAnswer = false
            reports = 0
        }
    }

    /// The main thread and how long it has been silent, when a stall is due to be printed.
    func stallToReport() -> (thread: pthread_t, silence: TimeInterval)? {
        lock.withLock {
            let silence = Date().timeIntervalSince(lastAnswer)
            let due = MainThreadWatchdog.stallSeconds + Double(reports) * MainThreadWatchdog.repeatSeconds
            guard let mainThread, reports < MainThreadWatchdog.reportsPerStall, silence >= due else { return nil }
            reports += 1
            return (mainThread, silence)
        }
    }
}
