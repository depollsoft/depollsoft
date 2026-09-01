import SwiftUI
import FirebaseAuth
import FirebaseFunctions

struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var toggleNotes = DPSettingsModel.sharedInstance.toggleNotes
    @State private var wakeLock = DPSettingsModel.sharedInstance.wakeLock
    @State private var isLoggedIn = Auth.auth().currentUser != nil
    @State private var showLogin = false
    @State private var showDeleteConfirmation = false
    @State private var isProcessing = false
    @State private var errorMessage: String?
    @State private var isRefreshing = false

    var body: some View {
        NavigationView {
            ZStack {
                Form {
                    Section("Settings") {
                        Toggle(isOn: $toggleNotes) {
                            VStack(alignment: .leading) {
                                Text("Toggle Notes")
                                Text("Play until pressed again")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                        .onChange(of: toggleNotes) { _, value in
                            guard !isRefreshing else { return }
                            DPSettingsModel.sharedInstance.toggleNotes = value
                        }

                        Toggle(isOn: $wakeLock) {
                            VStack(alignment: .leading) {
                                Text("Wake Lock")
                                Text("Prevent device from sleeping")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                        .onChange(of: wakeLock) { _, value in
                            guard !isRefreshing else { return }
                            DPSettingsModel.sharedInstance.wakeLock = value
                        }
                    }

                    Section {
                        if isLoggedIn {
                            Button("Log out", action: logOut)
                            Button("Delete Account") { showDeleteConfirmation = true }
                                .foregroundColor(.red)
                        } else {
                            Button("Log in") { showLogin = true }
                        }
                    } footer: {
                        Text("Log in to back up and synchronize your song list and settings.")
                    }
                }
                .disabled(isProcessing)

                if isProcessing {
                    Color.black.opacity(0.08).ignoresSafeArea()
                    ProgressView("Deleting account...")
                        .padding()
                        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
                        .accessibilityIdentifier("AccountProgress")
                }
            }
            .safeAreaInset(edge: .top, spacing: 0) {
                PitchPerfectBannerAd()
                    .frame(height: 50)
                    .accessibilityIdentifier("BannerAd")
            }
            .navigationBarTitle("Settings", displayMode: .inline)
            .navigationBarItems(trailing: Button("Done") { dismiss() })
            .sheet(isPresented: $showLogin, onDismiss: refreshAuth) {
                LoginView {
                    showLogin = false
                    refreshAuth()
                }
            }
            .confirmationDialog(
                "Delete Account \(DPSettingsModel.sharedInstance.userString)",
                isPresented: $showDeleteConfirmation,
                titleVisibility: .visible
            ) {
                Button("Delete Account", role: .destructive, action: deleteAccount)
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("This deletes your account and cloud data. Locally saved songs and preferences remain on this device.")
            }
            .alert("Account Error", isPresented: Binding(
                get: { errorMessage != nil },
                set: { if !$0 { errorMessage = nil } }
            )) {
                Button("OK", role: .cancel) { errorMessage = nil }
            } message: {
                Text(errorMessage ?? "Please try again.")
            }
            .onReceive(NotificationCenter.default.publisher(for: DPSettingsModel.settingsChangedNotificationName)) { _ in
                refreshSettings()
            }
        }
        .navigationViewStyle(.stack)
        .onAppear {
            refreshSettings()
            refreshAuth()
        }
    }

    private func refreshSettings() {
        isRefreshing = true
        toggleNotes = DPSettingsModel.sharedInstance.toggleNotes
        wakeLock = DPSettingsModel.sharedInstance.wakeLock
        DispatchQueue.main.async { isRefreshing = false }
    }

    private func refreshAuth() {
        isLoggedIn = Auth.auth().currentUser != nil
    }

    private func logOut() {
        do {
            try Auth.auth().signOut()
            refreshAuth()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func deleteAccount() {
        guard Auth.auth().currentUser != nil else {
            refreshAuth()
            return
        }
        isProcessing = true
        Functions.functions().httpsCallable("deleteUser").call { _, error in
            DispatchQueue.main.async {
                isProcessing = false
                if let error {
                    errorMessage = error.localizedDescription
                    return
                }
                do {
                    try Auth.auth().signOut()
                    refreshAuth()
                } catch {
                    errorMessage = error.localizedDescription
                }
            }
        }
    }
}
