import copy
import datetime
from pathlib import Path
import plistlib
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import verify_firebase_ipa as verifier


class SigningTests(unittest.TestCase):
    def setUp(self):
        self.identifier = verifier.BUNDLES["PitchPerfect"] + ".widget"
        self.signed = {
            "application-identifier": verifier.TEAM + "." + self.identifier,
            "com.apple.developer.team-identifier": verifier.TEAM,
            "com.apple.security.application-groups": [verifier.GROUP],
        }
        self.profile = {
            "Entitlements": copy.deepcopy(self.signed),
            "ProvisionedDevices": ["test-device"],
            "ExpirationDate": datetime.datetime.now() + datetime.timedelta(days=1),
        }

    def check(self):
        verifier.check_entitlements(self.identifier, self.signed, self.profile, True)

    def test_valid_shared_group(self):
        self.check()

    def test_missing_signed_group(self):
        del self.signed["com.apple.security.application-groups"]
        with self.assertRaisesRegex(ValueError, "signed binary has no private App Group"):
            self.check()

    def test_profile_does_not_grant_group(self):
        self.profile["Entitlements"]["com.apple.security.application-groups"] = []
        with self.assertRaisesRegex(ValueError, "profile does not grant"):
            self.check()

    def test_wrong_profile(self):
        self.profile["Entitlements"]["application-identifier"] += ".wrong"
        with self.assertRaisesRegex(ValueError, "different app"):
            self.check()

    def test_expired_profile(self):
        self.profile["ExpirationDate"] = datetime.datetime(2000, 1, 1)
        with self.assertRaisesRegex(ValueError, "expired"):
            self.check()

    def test_empty_signature_from_old_widget(self):
        self.signed = {}
        with self.assertRaisesRegex(ValueError, "signed application identifier"):
            self.check()

    def test_missing_widget_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            app = Path(directory) / "pitchperfect.app"
            app.mkdir()
            with (app / "Info.plist").open("wb") as output:
                plistlib.dump({"CFBundleIdentifier": verifier.BUNDLES["PitchPerfect"],
                               "CFBundleVersion": "123"}, output)
            with self.assertRaisesRegex(ValueError, "widget extension is missing"):
                verifier.inspect_bundles(Path(directory), "PitchPerfect")


if __name__ == "__main__":
    unittest.main()
