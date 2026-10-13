"""Reject Firebase builds that cannot register or share state with their widget."""
import argparse
import datetime
import json
from pathlib import Path
import plistlib
import subprocess
import shutil
import tempfile

TEAM = "43AWMS3YJX"
GROUP = "group.depollsoft.pitchperfect.private"
BUNDLES = {
    "PitchPerfect": "depollsoft.pitchperfect.private",
    "TagMaster": "depollsoft.tagmaster.private",
}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def read_plist(path):
    with path.open("rb") as source:
        return plistlib.load(source)


def inspect_bundles(root, app):
    apps = list(root.glob("*.app"))
    require(len(apps) == 1, "Expected exactly one containing app")
    bundle = apps[0]
    info = read_plist(bundle / "Info.plist")
    require(info["CFBundleIdentifier"] == BUNDLES[app], "Unexpected app bundle identifier")
    bundles = [bundle]
    if app == "PitchPerfect":
        widgets = [path for path in (bundle / "PlugIns").glob("*.appex")
                   if read_plist(path / "Info.plist")["CFBundleIdentifier"] == BUNDLES[app] + ".widget"]
        require(len(widgets) == 1, "Pitch Perfect widget extension is missing")
        widget = widgets[0]
        widget_info = read_plist(widget / "Info.plist")
        require(widget_info.get("NSExtension", {}).get("NSExtensionPointIdentifier") ==
                "com.apple.widgetkit-extension", "Extension is not registered as a WidgetKit widget")
        require(widget_info["CFBundleVersion"] == info["CFBundleVersion"], "App/widget build numbers differ")
        bundles.append(widget)
    return bundles


def check_entitlements(identifier, signed, profile, needs_group):
    expected_id = TEAM + "." + identifier
    require(signed.get("application-identifier") == expected_id,
            f"{identifier}: signed application identifier is missing or wrong")
    require(signed.get("com.apple.developer.team-identifier") == TEAM,
            f"{identifier}: signed team identifier is missing or wrong")
    grants = profile["Entitlements"]
    require(grants.get("application-identifier") == expected_id,
            f"{identifier}: provisioning profile is for a different app")
    require(profile.get("ProvisionedDevices"), f"{identifier}: not a device-installable ad hoc profile")
    require(profile["ExpirationDate"] > datetime.datetime.now(datetime.timezone.utc).replace(tzinfo=None),
            f"{identifier}: expired provisioning profile")
    require(not signed.get("get-task-allow", False), f"{identifier}: development signing is not allowed")
    if needs_group:
        require(GROUP in signed.get("com.apple.security.application-groups", []),
                f"{identifier}: signed binary has no private App Group entitlement")
        require(GROUP in grants.get("com.apple.security.application-groups", []),
                f"{identifier}: provisioning profile does not grant private App Group")


def check_same_devices(profiles):
    """iOS installs an app only if every bundle's profile lists the device.

    Match regenerates each ad hoc profile independently, so after a device is
    registered the app's profile can gain it while the widget's still lacks it.
    Such an IPA installs on older devices and fails on the new one with "its
    integrity could not be verified".
    """
    devices = {identifier: set(profile.get("ProvisionedDevices", []))
               for identifier, profile in profiles.items()}
    everyone = set().union(*devices.values()) if devices else set()
    for identifier, listed in sorted(devices.items()):
        missing = everyone - listed
        require(not missing, f"{identifier}: provisioning profile lacks {len(missing)} device(s) "
                             "another bundle's profile lists; regenerate it")


def read_profile(path):
    return plistlib.loads(subprocess.check_output(
        ["security", "cms", "-D", "-i", str(path)], stderr=subprocess.DEVNULL))


def verify_signed(bundles, app):
    results = []
    embedded = {}
    for bundle in bundles:
        info = read_plist(bundle / "Info.plist")
        subprocess.run(["codesign", "--verify", "--strict", str(bundle)], check=True,
                       capture_output=True)
        signed = plistlib.loads(subprocess.check_output(
            ["codesign", "-d", "--entitlements", ":-", str(bundle)], stderr=subprocess.DEVNULL))
        profile = read_profile(bundle / "embedded.mobileprovision")
        check_entitlements(info["CFBundleIdentifier"], signed, profile, app == "PitchPerfect")
        embedded[info["CFBundleIdentifier"]] = profile
        results.append({"bundle": info["CFBundleIdentifier"], "build": info["CFBundleVersion"],
                        "groups": signed.get("com.apple.security.application-groups", []),
                        "profile": profile["Name"],
                        "devices": len(profile.get("ProvisionedDevices", []))})
    check_same_devices(embedded)
    print(json.dumps(results, indent=2))


def prepare_archive(bundles, manifest, app):
    """Unsigned archives have no capability requests for exportArchive to retain.

    Seal each bundle with its matched profile's entitlements before Xcode does
    the distribution signing. These temporary signatures are ad hoc, not the
    signatures shipped to testers. Keep the app last so it seals the extension.
    """
    profiles = read_plist(manifest)
    check_same_devices({identifier: read_profile(path) for identifier, path in profiles.items()})
    # Frameworks in an unsigned archive need seals before the containing app.
    nested_code = list(bundles[0].rglob("*.framework")) + list(bundles[0].rglob("*.dylib"))
    for path in sorted(nested_code, key=lambda item: len(item.parts), reverse=True):
        subprocess.run(["codesign", "--force", "--sign", "-", str(path)], check=True)
    for bundle in reversed(bundles):
        identifier = read_plist(bundle / "Info.plist")["CFBundleIdentifier"]
        profile_path = Path(profiles[identifier])
        profile = read_profile(profile_path)
        entitlements = profile["Entitlements"].copy()
        # Match uses explicit App IDs. Expand the profile's keychain wildcard
        # to the app's own access group rather than requesting a wildcard.
        entitlements["keychain-access-groups"] = [
            value.replace("*", identifier)
            for value in entitlements.get("keychain-access-groups", [])
        ]
        check_entitlements(identifier, entitlements, profile, app == "PitchPerfect")
        shutil.copyfile(profile_path, bundle / "embedded.mobileprovision")
        with tempfile.TemporaryDirectory(prefix="firebase-entitlements-") as directory:
            path = Path(directory) / "entitlements.plist"
            with path.open("wb") as output:
                plistlib.dump(entitlements, output)
            subprocess.run(["codesign", "--force", "--sign", "-", "--entitlements", str(path),
                            "--generate-entitlement-der", str(bundle)], check=True)
        print(f"Prepared export entitlements for {identifier}")

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--ipa", type=Path)
    source.add_argument("--archive", type=Path)
    parser.add_argument("--app", required=True, choices=BUNDLES)
    parser.add_argument("--prepare-profiles", type=Path)
    parser.add_argument("--compare-profiles", type=Path,
                        help="exit 3 when the manifest's profiles list different devices")
    args = parser.parse_args()
    if args.compare_profiles:
        manifest = read_plist(args.compare_profiles)
        try:
            check_same_devices({identifier: read_profile(path) for identifier, path in manifest.items()})
        except ValueError as error:
            print(error)
            raise SystemExit(3)
        print("Profiles list the same devices")
        return
    if args.archive:
        bundles = inspect_bundles(args.archive / "Products" / "Applications", args.app)
        print("Archive contains: " + ", ".join(path.name for path in bundles))
        if args.prepare_profiles:
            prepare_archive(bundles, args.prepare_profiles, args.app)
    else:
        with tempfile.TemporaryDirectory(prefix="firebase-verify-") as directory:
            subprocess.run(["ditto", "-x", "-k", str(args.ipa), directory], check=True)
            verify_signed(inspect_bundles(Path(directory) / "Payload", args.app), args.app)


if __name__ == "__main__":
    main()
