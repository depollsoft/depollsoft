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
//  The stack is read from the watchdog's own thread: it suspends the main
//  thread, copies the return addresses off its frame chain, and resumes it
//  before looking any of them up. Nothing runs on the main thread, and nothing
//  in that window allocates or takes a lock the main thread could be holding.
//

import Darwin
import Foundation

enum MainThreadWatchdog {
    /// How long the main thread may go without answering before its stack is printed.
    static let stallSeconds: TimeInterval = 5
    /// How often a stall that goes on is printed again, and how many times in all.
    static let repeatSeconds: TimeInterval = 10
    static let reportsPerStall = 3
    /// The most frames printed for one stack.
    static let frameCapacity = 128

    private static let state = WatchdogState()

    /// Starts watching the main thread; call it from the main thread. Later calls do nothing.
    static func start() {
        precondition(Thread.isMainThread, "the watchdog watches the thread that starts it")
        guard state.begin() else { return }
        let main = SuspendableThread(pthread_self())
        let thread = Thread {
            // Allocated up front: while the main thread is suspended nothing may allocate.
            let frames = UnsafeMutablePointer<UnsafeMutableRawPointer?>.allocate(capacity: frameCapacity)
            while true {
                if state.askAgain() {
                    DispatchQueue.main.async { state.answered() }
                }
                Thread.sleep(forTimeInterval: 0.5)
                guard let silence = state.stallToReport() else { continue }
                let count = main.copyStack(into: frames, capacity: frameCapacity)
                let header = "MainThreadWatchdog: the main thread has not answered for "
                    + "\(String(format: "%.1f", silence)) s (\(Date())); its stack:\n"
                FileHandle.standardError.write(Data(header.utf8))
                backtrace_symbols_fd(frames, Int32(count), STDERR_FILENO)
                FileHandle.standardError.write(Data("MainThreadWatchdog: end of stack\n".utf8))
            }
        }
        thread.name = "MainThreadWatchdog"
        thread.qualityOfService = .userInteractive
        thread.start()
    }
}

/// A thread whose stack another thread can copy: its Mach port and the bounds of its stack.
private struct SuspendableThread: @unchecked Sendable {
    let port: thread_act_t
    let stackLow: UInt
    let stackHigh: UInt

    init(_ thread: pthread_t) {
        port = pthread_mach_thread_np(thread)
        stackHigh = UInt(bitPattern: pthread_get_stackaddr_np(thread))
        stackLow = stackHigh - UInt(pthread_get_stacksize_np(thread))
    }

    /// Suspends the thread, copies its program counter and the return addresses on its frame
    /// chain into `frames`, and resumes it. Between suspending and resuming, the thread may hold
    /// the allocator's or the loader's lock, so this only reads registers and stack memory.
    func copyStack(into frames: UnsafeMutablePointer<UnsafeMutableRawPointer?>, capacity: Int) -> Int {
        guard thread_suspend(port) == KERN_SUCCESS else { return 0 }
        defer { thread_resume(port) }
        guard let registers = registers() else { return 0 }
        var count = 0
        frames[count] = UnsafeMutableRawPointer(bitPattern: registers.pc)
        count += 1
        // A function stopped before it saved the link register has its caller only there.
        if registers.lr != 0 {
            frames[count] = UnsafeMutableRawPointer(bitPattern: registers.lr)
            count += 1
        }
        // Each frame record is the caller's frame pointer, then the return address. Stay on the
        // thread's stack, and walk only upward, so a torn record ends the walk.
        var fp = registers.fp
        while count < capacity, fp % 8 == 0, fp >= stackLow, fp + 16 <= stackHigh,
              let record = UnsafePointer<UInt>(bitPattern: fp) {
            let caller = record[0]
            let returnAddress = record[1]
            guard returnAddress != 0 else { break }
            frames[count] = UnsafeMutableRawPointer(bitPattern: returnAddress)
            count += 1
            guard caller > fp else { break }
            fp = caller
        }
        return count
    }

    private func registers() -> (pc: UInt, lr: UInt, fp: UInt)? {
#if arch(arm64)
        var state = arm_thread_state64_t()
        var count = mach_msg_type_number_t(MemoryLayout<arm_thread_state64_t>.size / MemoryLayout<natural_t>.size)
        let result = withUnsafeMutablePointer(to: &state) {
            $0.withMemoryRebound(to: natural_t.self, capacity: Int(count)) {
                thread_get_state(port, thread_state_flavor_t(ARM_THREAD_STATE64), $0, &count)
            }
        }
        guard result == KERN_SUCCESS else { return nil }
        return (UInt(state.__pc), UInt(state.__lr), UInt(state.__fp))
#elseif arch(x86_64)
        var state = x86_thread_state64_t()
        var count = mach_msg_type_number_t(MemoryLayout<x86_thread_state64_t>.size / MemoryLayout<natural_t>.size)
        let result = withUnsafeMutablePointer(to: &state) {
            $0.withMemoryRebound(to: natural_t.self, capacity: Int(count)) {
                thread_get_state(port, thread_state_flavor_t(x86_THREAD_STATE64), $0, &count)
            }
        }
        guard result == KERN_SUCCESS else { return nil }
        return (UInt(state.__rip), 0, UInt(state.__rbp))
#else
        return nil
#endif
    }
}

/// When the main thread last answered, and how much of its current stall has been printed.
private final class WatchdogState: @unchecked Sendable {
    private let lock = NSLock()
    private var started = false
    private var lastAnswer = Date()
    private var waitingForAnswer = false
    private var reports = 0

    func begin() -> Bool {
        lock.withLock {
            guard !started else { return false }
            started = true
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

    /// How long the main thread has been silent, when a stall is due to be printed.
    func stallToReport() -> TimeInterval? {
        lock.withLock {
            let silence = Date().timeIntervalSince(lastAnswer)
            let due = MainThreadWatchdog.stallSeconds + Double(reports) * MainThreadWatchdog.repeatSeconds
            guard reports < MainThreadWatchdog.reportsPerStall, silence >= due else { return nil }
            reports += 1
            return silence
        }
    }
}
