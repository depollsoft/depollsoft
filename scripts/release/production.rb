# Shared Fastlane production lanes. The caller selects one app and one platform.
require 'json'
require 'fileutils'
require 'tmpdir'

RELEASE_ROOT = File.expand_path('../..', __dir__)

def production_play_releases(package, key)
  require 'supply'
  Supply::Client.make_from_config(params: {json_key: key, timeout: 300})
    .list_track_release_summaries(package, 'production')
end

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
    store_version = store_app.get_app_store_versions(
      filter: {versionString: version.fetch('version'), platform: 'IOS'}, includes: 'build',
    ).first
    submitted_states = %w[WAITING_FOR_REVIEW IN_REVIEW ACCEPTED PENDING_APPLE_RELEASE
                          PENDING_DEVELOPER_RELEASE PROCESSING_FOR_DISTRIBUTION READY_FOR_DISTRIBUTION]
    if store_version && submitted_states.include?(store_version.app_version_state)
      unless store_version.build&.version.to_s == version.fetch('build').to_s
        UI.user_error!('This App Store version was submitted with a different build; choose a new version')
      end
      UI.success("#{app} version #{version.fetch('version')} build #{version.fetch('build')} is already submitted (#{store_version.app_version_state})")
      next
    end
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
      **(existing_build ? {build_number: version.fetch('build').to_s} : {ipa: ipa}),
      skip_binary_upload: !existing_build.nil?,
      metadata_path: File.join(assets, 'metadata'), screenshots_path: File.join(assets, 'screenshots'),
      overwrite_screenshots: true, force: true, run_precheck_before_submit: false,
      submission_information: {export_compliance_uses_encryption: false},
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
    # Release summaries include pending reviews, which are not yet live.
    releases = production_play_releases(package, key)
    release = releases.find do |item|
      item.track == 'production' && Array(item.active_artifacts).any? do |artifact|
        artifact.version_code.to_i == version.fetch('build')
      end
    end
    # A phone-only upload replaces the track's artifact list. Keep the existing
    # Wear build while it is still published; never reactivate a retired build.
    published_codes = releases.select do |item|
      item.track == 'production' && item.release_lifecycle_state == 'RELEASE_LIFECYCLE_STATE_PUBLISHED'
    end.flat_map { |item| Array(item.active_artifacts).map { |artifact| artifact.version_code.to_i } }
    retained_codes = Array(config['play_retained_version_codes']) & published_codes
    if release
      accepted = %w[IN_REVIEW APPROVED_NOT_PUBLISHED PUBLISHED].map { |state| "RELEASE_LIFECYCLE_STATE_#{state}" }
      unless accepted.include?(release.release_lifecycle_state)
        UI.user_error!("Play build #{version.fetch('build')} requires attention: #{release.release_lifecycle_state}")
      end
      UI.success("#{app} build #{version.fetch('build')} is already submitted (#{release.release_lifecycle_state})")
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
      version_codes_to_retain: retained_codes,
      aab: File.join(RELEASE_ROOT, 'Android', config.fetch('module'),
                     'build/outputs/bundle/release', "#{config.fetch('module')}-release.aab"),
      metadata_path: File.join(assets, 'metadata'), skip_upload_apk: true,
      skip_upload_metadata: false, skip_upload_images: false, skip_upload_screenshots: false,
      changes_not_sent_for_review: false, rescue_changes_not_sent_for_review: false,
    )
  end
end
