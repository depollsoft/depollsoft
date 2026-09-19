# Shared Fastlane production lanes. The caller selects one app and one platform.
require 'json'
require 'fileutils'
require 'tmpdir'
require_relative 'play_diagnostics'

RELEASE_ROOT = File.expand_path('../..', __dir__)

def production_play_releases(package, key, track = 'production')
  require 'supply'
  Supply::Client.make_from_config(params: {json_key: key, timeout: 300})
    .list_track_release_summaries(package, track)
end

def production_play_tracks(package, key)
  require 'supply'
  client = Supply::Client.make_from_config(params: {json_key: key, timeout: 300})
  begin
    client.begin_edit(package_name: package)
    client.tracks.map(&:track)
  ensure
    client.abort_current_edit if client.current_edit
  end
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
    targets = [{module: config.fetch('module'), track: 'production', build: version.fetch('build')}]
    if (wear = config['wear'])
      unless production_play_tracks(package, key).include?(wear.fetch('track'))
        UI.user_error!("Enable Pitch Perfect's dedicated Wear OS release track in Play Console before releasing")
      end
      # Submit the replacement watch build before removing the obsolete watch
      # bundle from the phone track. A retry checks each track independently.
      targets.unshift(module: wear.fetch('module'), track: wear.fetch('track'), build: version.fetch('build') + 1)
    end
    pending = targets.reject do |target|
      releases = production_play_releases(package, key, target[:track])
      release = releases.find do |item|
        item.track == target[:track] && Array(item.active_artifacts).any? do |artifact|
          artifact.version_code.to_i == target[:build]
        end
      end
      if release
        accepted = %w[IN_REVIEW APPROVED_NOT_PUBLISHED PUBLISHED].map { |state| "RELEASE_LIFECYCLE_STATE_#{state}" }
        unless accepted.include?(release.release_lifecycle_state)
          UI.user_error!("Play #{target[:track]} build #{target[:build]} requires attention: #{release.release_lifecycle_state}")
        end
        UI.success("#{app} #{target[:track]} build #{target[:build]} is already submitted (#{release.release_lifecycle_state})")
        true
      else
        false
      end
    end
    pending.each do |target|
      %w[ANDROID_UPLOAD_KEYSTORE_PATH ANDROID_UPLOAD_KEYSTORE_PASSWORD ANDROID_UPLOAD_KEY_ALIAS ANDROID_UPLOAD_KEY_PASSWORD].each do |name|
        UI.user_error!("Missing #{name}") if ENV[name].to_s.empty?
      end
      gradle(task: ":#{target[:module]}:bundleRelease", project_dir: '.', properties: {
        'DEPOLLSOFT_RELEASE_APP' => target[:module],
        'DEPOLLSOFT_RELEASE_VERSION' => version.fetch('version'),
        'DEPOLLSOFT_RELEASE_BUILD' => target[:build],
      })
      with_play_submission_diagnostics(File.join(RELEASE_ROOT, 'build/release/play-upload-error.json')) do
        upload_to_play_store(
          package_name: package, json_key: key, track: target[:track], release_status: 'completed',
          version_codes_to_retain: [],
          aab: File.join(RELEASE_ROOT, 'Android', target[:module],
                         'build/outputs/bundle/release', "#{target[:module]}-release.aab"),
          metadata_path: File.join(assets, 'metadata'), skip_upload_apk: true,
          skip_upload_metadata: false, skip_upload_images: false, skip_upload_screenshots: false,
          changes_not_sent_for_review: false, rescue_changes_not_sent_for_review: false,
        )
      end
    end
  end
end
