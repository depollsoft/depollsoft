# Read authenticated store history without uploading or committing a Play edit.
require 'json'
require 'base64'
require 'stringio'
require 'time'
require 'spaceship'
require 'google/apis/androidpublisher_v3'
require 'googleauth'

root = File.expand_path('../..', __dir__)
configs = JSON.parse(File.read(File.join(root, 'scripts/release/apps.json')))
apps = ARGV
raise 'Select at least one app' if apps.empty?
raise 'Unknown app' unless (apps - configs.keys).empty?
Spaceship::ConnectAPI.token = Spaceship::ConnectAPI::Token.create(
  key_id: ENV.fetch('APP_STORE_CONNECT_API_KEY_ID'),
  issuer_id: ENV.fetch('APP_STORE_CONNECT_API_ISSUER_ID'),
  key: ENV.fetch('APP_STORE_CONNECT_API_KEY_CONTENT'),
)
raw = ENV.fetch('PLAY_SERVICE_ACCOUNT_JSON')
raw = Base64.strict_decode64(raw.gsub(/\s/, '')) unless raw.lstrip.start_with?('{')
play = Google::Apis::AndroidpublisherV3::AndroidPublisherService.new
play.authorization = Google::Auth::ServiceAccountCredentials.make_creds(
  json_key_io: StringIO.new(raw), scope: 'https://www.googleapis.com/auth/androidpublisher',
)
report = {checked_at: Time.now.utc.iso8601, apps: {}}
errors = []
apps.each do |app|
  package = configs.fetch(app).fetch('bundle_id')
  plan = JSON.parse(File.read(File.join(root, 'releases', app, 'release.json')))
  result = {package: package}
  report[:apps][app] = result
  if plan.fetch('platforms').key?('ios')
    store_app = Spaceship::ConnectAPI::App.find(package)
    raise "App Store Connect app missing: #{package}" unless store_app
    versions = store_app.get_app_store_versions(filter: {platform: 'IOS'}, includes: 'build')
    builds = store_app.get_builds(includes: 'preReleaseVersion', sort: '-uploadedDate')
    result[:ios] = {
      versions: versions.map { |v| {version: v.version_string, state: v.app_version_state,
        legacy_state: v.app_store_state, build: v.build&.version,
        created_at: v.created_date, earliest_release_date: v.earliest_release_date} },
      uploads: builds.map { |b| {version: b.app_version, build: b.version,
        uploaded_at: b.uploaded_date, state: b.processing_state} },
    }
    target = plan.fetch('platforms').fetch('ios')
    highest = builds.map { |b| Gem::Version.new(b.version) }.max
    if highest && Gem::Version.new(target.fetch('build').to_s) <= highest
      errors << "#{app} iOS planned build must exceed uploaded build #{highest}"
    end
  end
  if plan.fetch('platforms').key?('android')
    edit = play.insert_edit(package)
    begin
      tracks = play.list_edit_tracks(package, edit.id).tracks || []
      bundles = play.list_edit_bundles(package, edit.id).bundles || []
      apks = play.list_edit_apks(package, edit.id).apks || []
      lifecycle = play.list_application_track_releases("applications/#{package}/tracks/production")
      codes = (bundles + apks).map(&:version_code) + tracks.flat_map { |t|
        (t.releases || []).flat_map { |r| r.version_codes || [] }
      }
      result[:android] = {
        tracks: tracks.map(&:to_h), production_releases: lifecycle.to_h,
        bundle_version_codes: bundles.map(&:version_code), apk_version_codes: apks.map(&:version_code),
        highest_uploaded_build: codes.map(&:to_i).max,
      }
      target = plan.fetch('platforms').fetch('android')
      if codes.any? && target.fetch('build') <= codes.map(&:to_i).max
        errors << "#{app} Android planned build must exceed uploaded build #{codes.map(&:to_i).max}"
      end
    ensure
      # Never validate, modify tracks, or commit this inspection-only edit.
      play.delete_edit(package, edit.id)
    end
  end
end
report[:errors] = errors
path = ENV.fetch('STORE_REPORT', File.join(root, 'build/release/store-history.json'))
require 'fileutils'
FileUtils.mkdir_p(File.dirname(path))
File.write(path, JSON.pretty_generate(report) + "\n")
puts JSON.pretty_generate(report)
abort(errors.join("\n")) unless errors.empty?
