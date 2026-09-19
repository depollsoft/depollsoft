require 'json'
require 'fileutils'

# Fastlane wraps Google API exceptions and omits their structured error details.
# Keep only error fields; never log request headers, credentials, or request bodies.
def with_play_submission_diagnostics(path)
  yield
rescue StandardError => error
  begin
    causes = []
    cause = error
    while cause && !causes.include?(cause)
      causes << cause
      cause = cause.cause
    end
    api_error = causes.find { |item| item.respond_to?(:body) && item.body }
    if api_error
      payload = JSON.parse(api_error.body).fetch('error', {})
      report = payload.slice('code', 'status', 'message')
      report['details'] = Array(payload['details']).map do |detail|
        detail.slice('@type', 'reason', 'domain', 'fieldViolations', 'violations')
      end
      FileUtils.mkdir_p(File.dirname(path))
      File.write(path, JSON.pretty_generate(report) + "\n")
      warn("Google Play submission error details: #{JSON.generate(report)}")
    end
  rescue StandardError
    warn('Unable to extract Google Play error details; preserving the original failure.')
  end
  raise
end
