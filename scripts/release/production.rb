# Shared Fastlane production lanes. The caller selects one app and one platform.
require 'json'
require 'fileutils'
require 'tmpdir'

RELEASE_ROOT = File.expand_path('../..', __dir__)

def production_plan(options, platform)
  app = options[:app].to_s
  configs = JSON.parse(File.read(File.join(RELEASE_ROOT, 'scripts/release/apps.json')))
  UI.user_error!("Unknown release app: #{app}") unless configs.key?(app)
  plan = JSON.parse(File.read(File.join(RELEASE_ROOT, 'releases', app, 'release.json')))
  version = plan.fetch('platforms').fetch(platform)
  assets = File.expand_path(ENV.fetch('RELEASE_ASSETS'))
  sh(ENV.fetch('RELEASE_PYTHON', 'python3'), File.join(RELEASE_ROOT, 'scripts/release/release.py'), 'validate')
  sh(ENV.fetch('RELEASE_PYTHON', 'python3'), File.join(RELEASE_ROOT, 'scripts/release/capture.py'),
     '--app', app, '--platform', platform, '--output', assets, '--validate-only')
  sh(ENV.fetch('RELEASE_PYTHON', 'python3'), File.join(RELEASE_ROOT, 'scripts/release/release.py'),
     'export', '--app', app, '--platform', platform, '--output', assets)
  [app, configs.fetch(app), version, assets]
end

platform :ios do
  desc 'Create or repair production App Store profiles for one app'
  lane :provision_production do |options|
    app = options[:app].to_s
    configs = JSON.parse(File.read(File.join(RELEASE_ROOT, 'scripts/release/apps.json')))
    UI.user_error!("Unknown release app: #{app}") unless configs.key?(app)
    identifier = configs.fetch(app).fetch('bundle_id')
    identifiers = [identifier]
    identifiers << "#{identifier}.widget" if app == 'pitchperfect'
    api_key = app_store_connect_api_key(
      key_id: ENV.fetch('APP_STORE_CONNECT_API_KEY_ID'),
      issuer_id: ENV.fetch('APP_STORE_CONNECT_API_ISSUER_ID'),
      key_content: ENV.fetch('APP_STORE_CONNECT_API_KEY_CONTENT'),
      is_key_content_base64: false,
    )
    match(type: 'appstore', readonly: false, app_identifier: identifiers, api_key: api_key)
  end

  desc 'Submit one production app with its reviewed metadata and live screenshots'
  lane :deploy_production do |options|
    app, config, version, assets = production_plan(options, 'ios')
    api_key = app_store_connect_api_key(
      key_id: ENV.fetch('APP_STORE_CONNECT_API_KEY_ID'),
      issuer_id: ENV.fetch('APP_STORE_CONNECT_API_ISSUER_ID'),
      key_content: ENV.fetch('APP_STORE_CONNECT_API_KEY_CONTENT'),
      is_key_content_base64: false,
    )
    identifier = config.fetch('bundle_id')
    store_app = Spaceship::ConnectAPI::App.find(identifier)
    UI.user_error!("App Store Connect app missing: #{identifier}") unless store_app
    existing_build = store_app.get_builds(filter: {version: version.fetch('build').to_s}, includes: 'preReleaseVersion').first
    if existing_build && existing_build.app_version != version.fetch('version')
      UI.user_error!('This build number belongs to another App Store version; choose a new build number')
    end
    ipa = nil
    unless existing_build
      identifiers = [identifier]
      identifiers << "#{identifier}.widget" if app == 'pitchperfect'
      match(type: 'appstore', readonly: true, app_identifier: identifiers, api_key: api_key)
      profiles = lane_context[SharedValues::MATCH_PROVISIONING_PROFILE_MAPPING]
      project = File.join(RELEASE_ROOT, 'iOS', app, "#{app}.xcodeproj")
      original = File.binread(File.join(project, 'project.pbxproj'))
      begin
        Dir.mktmpdir('production-signing-') do |directory|
          targets = [[app, identifier, File.join(RELEASE_ROOT, 'iOS', app, app, "#{app}.entitlements")]]
          if app == 'pitchperfect'
            targets << ['PitchPerfectWidget', "#{identifier}.widget",
                        File.join(RELEASE_ROOT, 'iOS/pitchperfect/PitchPerfectWidget/PitchPerfectWidget.entitlements')]
          end
          targets.each do |target, bundle, source|
            entitlements = File.join(directory, "#{target}.entitlements")
            File.write(entitlements, File.read(source).sub('<string>development</string>', '<string>production</string>'))
            update_code_signing_settings(
              path: project, use_automatic_signing: false, team_id: '43AWMS3YJX',
              targets: [target], build_configurations: ['Release'],
              code_sign_identity: 'Apple Distribution', profile_name: profiles.fetch(bundle),
              bundle_identifier: bundle, entitlements_file_path: entitlements,
            )
          end
          ipa = build_app(
            workspace: File.join(RELEASE_ROOT, 'iOS/iOS.xcworkspace'), scheme: app,
            export_method: 'app-store', export_team_id: '43AWMS3YJX',
            export_options: {provisioningProfiles: profiles.slice(*identifiers), manageAppVersionAndBuildNumber: false},
            xcargs: "MARKETING_VERSION=#{version.fetch('version')} CURRENT_PROJECT_VERSION=#{version.fetch('build')}",
            cloned_source_packages_path: File.join(RELEASE_ROOT, 'build/release/SourcePackages'),
            output_directory: File.join(RELEASE_ROOT, 'build/release/binaries', app), clean: true,
          )
        end
      ensure
        File.binwrite(File.join(project, 'project.pbxproj'), original)
      end
    end
    upload_to_app_store(
      api_key: api_key, app_identifier: identifier, app_version: version.fetch('version'),
      build_number: version.fetch('build').to_s, ipa: ipa,
      skip_binary_upload: !existing_build.nil?,
      metadata_path: File.join(assets, 'metadata'), screenshots_path: File.join(assets, 'screenshots'),
      overwrite_screenshots: true, force: true, run_precheck_before_submit: false,
      submit_for_review: true, automatic_release: true,
    )
  end
end

platform :android do
  desc 'Publish one production app with its reviewed metadata and live screenshots'
  lane :deploy_production do |options|
    app, config, version, assets = production_plan(options, 'android')
    package = config.fetch('bundle_id')
    key = ENV.fetch('PLAY_SERVICE_ACCOUNT_JSON_PATH')
    # A successful Play edit commits the binary and listing together.
    codes = google_play_track_version_codes(package_name: package, track: 'production', json_key: key)
    if codes.map(&:to_i).include?(version.fetch('build'))
      UI.success("#{app} build #{version.fetch('build')} is already on production")
      next
    end
    %w[ANDROID_UPLOAD_KEYSTORE_PATH ANDROID_UPLOAD_KEYSTORE_PASSWORD ANDROID_UPLOAD_KEY_ALIAS ANDROID_UPLOAD_KEY_PASSWORD].each do |name|
      UI.user_error!("Missing #{name}") if ENV[name].to_s.empty?
    end
    gradle(task: ":#{config.fetch('module')}:bundleRelease", project_dir: '.', properties: {
      'DEPOLLSOFT_RELEASE_APP' => config.fetch('module'),
      'DEPOLLSOFT_RELEASE_VERSION' => version.fetch('version'),
      'DEPOLLSOFT_RELEASE_BUILD' => version.fetch('build'),
    })
    upload_to_play_store(
      package_name: package, json_key: key, track: 'production', release_status: 'completed',
      aab: File.join(RELEASE_ROOT, 'Android', config.fetch('module'),
                     'build/outputs/bundle/release', "#{config.fetch('module')}-release.aab"),
      metadata_path: File.join(assets, 'metadata'), skip_upload_apk: true,
      skip_upload_metadata: false, skip_upload_images: false, skip_upload_screenshots: false,
      changes_not_sent_for_review: false, rescue_changes_not_sent_for_review: false,
    )
  end
end
