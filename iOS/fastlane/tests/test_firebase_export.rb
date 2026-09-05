require "tmpdir"
require "fileutils"
require "plist"

# Exercise the real lane without credentials, Xcode, or Fastlane side effects.
module SharedValues
  MATCH_PROVISIONING_PROFILE_MAPPING = :profiles
end
module UI
  def self.user_error!(message) = raise(message)
  def self.success(_message); end
end

def default_platform(_platform); end
def platform(_name) = yield
def before_all; end
def desc(_text); end
def lane(name, &block)
  ($lanes ||= {})[name] = block
end
def app_store_connect_api_key(**_options) = :test_key

def lane_context = { profiles: $profiles }
def match(**options)
  $profiles = options.fetch(:app_identifier).to_h { |id| [id, "match AdHoc #{id} replacement-123"] }
  $profiles.each_key { |id| ENV["sigh_#{id}_adhoc_profile-path"] = "/test/#{id}.mobileprovision" }
end

def sh(*args)
  $commands << args
  if args.first == "xcodebuild"
    raise "Unexpected source build" unless args[1] == "-exportArchive"
    options = Plist.parse_xml(args[args.index("-exportOptionsPlist") + 1])
    raise "Stale profile names" unless options.fetch("provisioningProfiles") == $profiles
    raise "Build number may change" unless options.fetch("manageAppVersionAndBuildNumber") == false
    destination = args[args.index("-exportPath") + 1]
    FileUtils.mkdir_p(destination)
    File.write(File.join(destination, "export.ipa"), "test signed IPA")
  elsif args.include?("--ipa") && $reject_ipa
    raise "invalid signed entitlements"
  end
end

load File.expand_path("../Fastfile", __dir__)
%w[APP_STORE_CONNECT_API_KEY_ID APP_STORE_CONNECT_API_ISSUER_ID APP_STORE_CONNECT_API_KEY_CONTENT].each do |key|
  ENV[key] = "test-only"
end
Object.send(:remove_const, :IOS_ROOT)
Dir.mktmpdir("firebase-lane-test-") do |directory|
  Object.const_set(:IOS_ROOT, directory)
  %w[PitchPerfect TagMaster].each do |app|
    $commands = []
    $reject_ipa = false
    $lanes.fetch(:export_firebase_private).call(app: app, archive: directory)
    raise "IPA not copied" unless File.exist?(File.join(directory, "build", "#{app}-private.ipa"))
    expected_count = app == "PitchPerfect" ? 2 : 1
    raise "Incorrect profile coverage" unless $profiles.length == expected_count
    raise "Archive wasn't checked first" unless $commands.first.include?("--archive")
    raise "IPA wasn't verified" unless $commands.last.include?("--ipa")
    FileUtils.rm_rf(File.join(directory, "build"))
    $reject_ipa = true
    begin
      $lanes.fetch(:export_firebase_private).call(app: app, archive: directory)
      raise "Invalid IPA was accepted"
    rescue RuntimeError => error
      raise unless error.message == "invalid signed entitlements"
    end
    raise "Invalid IPA was published" if File.exist?(File.join(directory, "build", "#{app}-private.ipa"))
  end
end
puts "PASS: both apps export directly with exact profiles; invalid IPAs are rejected"
