# Run the actual Fastlane lanes with store/build operations replaced by recorders.
require 'tmpdir'
require 'fileutils'
require 'json'

module UI
  def self.user_error!(message) = raise(message)
  def self.success(_message); end
end
module SharedValues
  MATCH_PROVISIONING_PROFILE_MAPPING = :profiles
end
module Spaceship
  module ConnectAPI
    class App
      def self.find(identifier)
        $seen_identifier = identifier
        new
      end
      def get_builds(**_options) = $existing_build ? [Struct.new(:app_version).new('2.0.9')] : []
      def get_app_store_versions(**options)
        $version_query = options
        [$store_version].compact
      end
    end
  end
end

def platform(name)
  $platform = name
  yield
end
def desc(_text); end
def lane(name, &block) = ($lanes ||= {})[[$platform, name]] = block

def sh(*args)
  $calls << [:validate, args]
  raise 'Rejected capture' if $reject_capture && args.include?('--validate-only')
end
def app_store_connect_api_key(**_options) = :test_key
def lane_context = {profiles: $profiles}
def match(**options)
  $calls << [:match, options]
  $profiles = options[:app_identifier].to_h { |id| [id, "match AppStore #{id}"] }
end
def update_code_signing_settings(**options) = $calls << [:sign, options]
def build_app(**options)
  $calls << [:build_ios, options]
  '/test/app.ipa'
end
def upload_to_app_store(**options) = $calls << [:upload_ios, options]
def gradle(**options) = $calls << [:build_android, options]
def upload_to_play_store(**options) = $calls << [:upload_android, options]
def assert(value, message)
  raise(message) unless value
end

load File.expand_path('../production.rb', __dir__)

def play_release(state, code = 1800000000, track = 'production')
  artifact = Struct.new(:version_code).new(code)
  Struct.new(:track, :active_artifacts, :release_lifecycle_state)
    .new(track, [artifact], "RELEASE_LIFECYCLE_STATE_#{state}")
end

def production_play_releases(package, key)
  $play_query = [package, key]
  $play_releases || ($existing_build ? [play_release('PUBLISHED')] : [])
end

source_root = RELEASE_ROOT
Object.send(:remove_const, :RELEASE_ROOT)
Dir.mktmpdir('production-lane-test-') do |directory|
  Object.const_set(:RELEASE_ROOT, directory)
  FileUtils.mkdir_p(File.join(directory, 'scripts/release'))
  FileUtils.cp(File.join(source_root, 'scripts/release/apps.json'), File.join(directory, 'scripts/release/apps.json'))
  %w[APP_STORE_CONNECT_API_KEY_ID APP_STORE_CONNECT_API_ISSUER_ID APP_STORE_CONNECT_API_KEY_CONTENT
     PLAY_SERVICE_ACCOUNT_JSON_PATH ANDROID_UPLOAD_KEYSTORE_PATH ANDROID_UPLOAD_KEYSTORE_PASSWORD
     ANDROID_UPLOAD_KEY_ALIAS ANDROID_UPLOAD_KEY_PASSWORD].each { |key| ENV[key] = 'test-only' }
  ENV['RELEASE_ASSETS'] = File.join(directory, 'assets')
  %w[tagmaster pitchperfect].each do |app|
    FileUtils.mkdir_p(File.join(directory, 'releases', app))
    File.write(File.join(directory, 'releases', app, 'release.json'), JSON.generate({
      platforms: {ios: {version: '2.0.9', build: 1800000000}, android: {version: '5.2.9', build: 1800000000}}
    }))
    project = File.join(directory, 'iOS', app, "#{app}.xcodeproj")
    FileUtils.mkdir_p(project)
    File.write(File.join(project, 'project.pbxproj'), 'original project')
    [[app, "#{app}.entitlements"], ['PitchPerfectWidget', 'PitchPerfectWidget.entitlements']].each do |target, filename|
      path = File.join(directory, 'iOS', app, target)
      FileUtils.mkdir_p(path)
      File.write(File.join(path, filename), '<string>development</string>')
    end
    [false, true].each do |existing|
      $existing_build = existing
      $store_version = nil
      $play_releases = nil
      $reject_capture = false
      $calls = []
      $lanes.fetch([:ios, :deploy_production]).call(app: app)
      assert($seen_identifier == "depollsoft.#{app}", 'Wrong store app')
      assert($version_query == {filter: {versionString: '2.0.9', platform: 'IOS'}, includes: 'build'}, 'Wrong review version query')
      upload = $calls.assoc(:upload_ios).last
      assert(upload[:app_version] == '2.0.9' && upload[:build_number] == '1800000000', 'Wrong iOS version')
      assert(upload[:skip_binary_upload] == existing, 'Retry did not reuse build')
      assert(upload[:submit_for_review] && upload[:automatic_release] && upload[:overwrite_screenshots], 'Missing store submission/assets')
      unless existing
        identifiers = $calls.assoc(:match).last.fetch(:app_identifier)
        expected = app == 'pitchperfect' ? ['depollsoft.pitchperfect', 'depollsoft.pitchperfect.widget'] : ['depollsoft.tagmaster']
        assert(identifiers == expected, 'Signed wrong app or missed widget')
        assert($calls.assoc(:build_ios).last[:scheme] == app, 'Built wrong scheme')
      end
      assert(File.read(File.join(project, 'project.pbxproj')) == 'original project', 'Signing changes leaked')
      $calls = []
      $lanes.fetch([:android, :deploy_production]).call(app: app)
      assert($play_query == ["depollsoft.#{app}", 'test-only'], 'Wrong Play review query')
      if existing
        assert(!$calls.assoc(:build_android) && !$calls.assoc(:upload_android), 'Re-uploaded active Play build')
      else
        build = $calls.assoc(:build_android).last
        assert(build[:task] == ":#{app == 'tagmaster' ? 'TagMaster' : 'PitchPerfect'}:bundleRelease", 'Built wrong Gradle module')
        assert(build[:properties]['DEPOLLSOFT_RELEASE_APP'] == (app == 'tagmaster' ? 'TagMaster' : 'PitchPerfect'), 'Version override targets wrong app')
        assert(build[:properties]['DEPOLLSOFT_RELEASE_VERSION'] == '5.2.9', 'Android reused iOS version')
        upload = $calls.assoc(:upload_android).last
        assert(upload[:package_name] == "depollsoft.#{app}" && upload[:track] == 'production', 'Wrong Play target')
        assert(!upload[:skip_upload_metadata] && !upload[:skip_upload_screenshots], 'Skipped store assets')
      end
    end
    # Store accepted submission, then GitHub release-record publication failed.
    # A retry must return without uploading assets or submitting a second review.
    %w[WAITING_FOR_REVIEW IN_REVIEW ACCEPTED PENDING_APPLE_RELEASE PENDING_DEVELOPER_RELEASE
       PROCESSING_FOR_DISTRIBUTION READY_FOR_DISTRIBUTION].each do |state|
      $store_version = Struct.new(:app_version_state, :build).new(state, Struct.new(:version).new('1800000000'))
      $calls = []
      $lanes.fetch([:ios, :deploy_production]).call(app: app)
      assert($calls.all? { |call| call.first == :validate }, "Repeated iOS submission in #{state}")
    end
    $store_version.build.version = '1799999999'
    $calls = []
    begin
      $lanes.fetch([:ios, :deploy_production]).call(app: app)
      raise 'Accepted a different submitted build'
    rescue RuntimeError => error
      raise unless error.message.include?('submitted with a different build')
    end
    assert($calls.all? { |call| call.first == :validate }, 'Changed a different submitted build')
    $store_version.app_version_state = 'PREPARE_FOR_SUBMISSION'
    $calls = []
    $lanes.fetch([:ios, :deploy_production]).call(app: app)
    assert($calls.assoc(:upload_ios), 'Skipped an unsubmitted iOS build')

    %w[IN_REVIEW APPROVED_NOT_PUBLISHED PUBLISHED].each do |state|
      $play_releases = [play_release(state)]
      $calls = []
      $lanes.fetch([:android, :deploy_production]).call(app: app)
      assert($calls.all? { |call| call.first == :validate }, "Repeated Play submission in #{state}")
    end
    %w[DRAFT NOT_SENT_FOR_REVIEW NOT_APPROVED UNSPECIFIED].each do |state|
      $play_releases = [play_release(state)]
      $calls = []
      begin
        $lanes.fetch([:android, :deploy_production]).call(app: app)
        raise "Accepted unsubmitted Play build in #{state}"
      rescue RuntimeError => error
        raise unless error.message.include?('requires attention')
      end
      assert($calls.all? { |call| call.first == :validate }, 'Re-uploaded a rejected or unsubmitted Play build')
    end
    $play_releases = [play_release('PUBLISHED', 1799999999), play_release('IN_REVIEW', 1800000000, 'internal')]
    $calls = []
    $lanes.fetch([:android, :deploy_production]).call(app: app)
    assert($calls.assoc(:upload_android), 'Wrong track/build suppressed a production submission')
    $reject_capture = true
    $calls = []
    begin
      $lanes.fetch([:ios, :deploy_production]).call(app: app)
      raise 'Accepted invalid capture'
    rescue RuntimeError => error
      raise unless error.message == 'Rejected capture'
    end
    assert($calls.all? { |call| call.first == :validate }, 'Store changed before capture validation')
  end
end
puts 'PASS: isolated production targets, widget signing, store assets, retries, and capture gate'
