#!/usr/bin/env ruby
# frozen_string_literal: true

# add_uitest_target.rb
# Adds a UI Testing target to an Xcode project
# Usage: ruby add_uitest_target.rb <project_path> <target_name> <host_target> [--sources <path>]

require 'xcodeproj'
require 'optparse'

def main
  options = { sources: nil }
  
  parser = OptionParser.new do |opts|
    opts.banner = "Usage: ruby #{File.basename($0)} <project_path> <target_name> <host_target> [options]"
    opts.on("--sources PATH", "Path to source files for the test target") { |v| options[:sources] = v }
    opts.on("-h", "--help", "Show this help") do
      puts opts
      exit
    end
  end
  parser.parse!
  
  if ARGV.length < 3
    puts parser
    exit 1
  end
  
  project_path, target_name, host_target_name = ARGV[0..2]
  
  unless File.exist?(project_path)
    abort "Project not found: #{project_path}"
  end
  
  project = Xcodeproj::Project.open(project_path)
  
  # Check if target already exists
  if project.targets.find { |t| t.name == target_name }
    puts "Target '#{target_name}' already exists, skipping creation"
    exit 0
  end
  
  # Find host target
  host_target = project.targets.find { |t| t.name == host_target_name }
  unless host_target
    abort "Host target '#{host_target_name}' not found. Available targets: #{project.targets.map(&:name).join(', ')}"
  end
  
  # Create UI Testing target
  puts "Creating UI Testing target: #{target_name}"
  
  ui_test_target = project.new_target(:ui_testing_bundle, target_name, :ios, '15.0')
  
  # Set test host
  ui_test_target.build_configurations.each do |config|
    config.build_settings['TEST_TARGET_NAME'] = host_target_name
    config.build_settings['BUNDLE_LOADER'] = ''
    config.build_settings['INFOPLIST_FILE'] = "#{target_name}/Info.plist"
    config.build_settings['PRODUCT_BUNDLE_IDENTIFIER'] = "com.depollsoft.#{target_name}"
    config.build_settings['CODE_SIGN_STYLE'] = 'Automatic'
    config.build_settings['SWIFT_VERSION'] = '5.0'
    config.build_settings['TARGETED_DEVICE_FAMILY'] = '1,2'
    config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'] = '15.0'
  end
  
  # Add test target dependency
  ui_test_target.add_dependency(host_target)
  
  # Create group for test files
  test_group = project.main_group.find_subpath(target_name, true)
  test_group.set_source_tree('SOURCE_ROOT')
  test_group.set_path(target_name)
  
  # Add source files if path specified
  if options[:sources] && Dir.exist?(options[:sources])
    Dir.glob(File.join(options[:sources], '*.swift')).each do |file|
      file_ref = test_group.new_file(File.basename(file))
      ui_test_target.add_file_references([file_ref])
    end
    
    # Add Info.plist if it exists
    info_plist = File.join(options[:sources], 'Info.plist')
    if File.exist?(info_plist)
      test_group.new_file('Info.plist')
    end
  end
  
  # Save project
  project.save
  
  puts "Successfully created UI Testing target: #{target_name}"
  puts "Host target: #{host_target_name}"
  puts "Remember to add the target to your scheme for testing."
end

main if __FILE__ == $0
