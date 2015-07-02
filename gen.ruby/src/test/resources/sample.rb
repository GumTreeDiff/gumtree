#
# This file is part of GumTree.
#
# GumTree is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# GumTree is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
#
# Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
# Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
#
require 'thor'
require 'fileutils'

require_relative 'diggit_core'

module Diggit

	module Utils

		DONE = '[done]'
		WARNING = '[warning]'
		ERROR = '[error]'
		INFO = '[info]'

		def diggit
			@diggit = Diggit.new if @diggit.nil?
			return @diggit
		end

		def dump_error(e)
			{ name: e.class.name, message: e.to_s, backtrace: e.backtrace }
		end

		def plugin_ok?(name, type)
			obj = Object::const_get(name)
			return obj < type
		rescue NameError
			return false
		end

		def source_color(source_hash)
			if source_hash[:log][:error].empty?
				return :blue
			else
				return :red
			end
		end

		def status_color(status)
			if status == DONE
				return :green
			else
				return :red
			end
		end

		def say_error(msg)
			say_status(ERROR, msg, :red)
		end

		def say_info(msg)
			say_status(INFO, msg, :blue)
		end

		def say_done(msg)
			say_status(DONE, msg, :green)
		end

	end

	module Cli

		class SourcesCli < Thor
			include Thor::Actions
			include Utils

			desc 'list', "Display the list of sources."
			def list
				idx = 1
				diggit.sources.hashes.each do |s|
					say_status("[#{s[:log][:state]}]", "#{idx}: #{s[:url]}", source_color(s))
					idx += 1
				end
				errors = diggit.sources.get_all(nil, {error: true}).size
				status = (errors== 0 && DONE) || ERROR
				say_status(status, "listed #{diggit.sources.size} sources including #{errors} errors", status_color(status))
			end

			desc 'info [SOURCE_DEF]', "Display informations on the provided source definition (either source URL or id)."
			def info(source_def)
				s = diggit.sources.get(source_def)
				say_status("[#{s[:log][:state]}]", "#{s[:url]}", source_color(s))
				say_status('[folder]', "#{s[:folder]}", :blue)
				unless s[:log][:error].empty?
					say_error("#{s[:log][:error][:name]}")
					say_status('[message]', "#{s[:log][:error][:message]}", :red)
					say_status('[backtrace]', "", :red)
					say(s[:log][:error][:backtrace].join("\n"))
				end
			end

			desc "errors", "Display informations on all source that have encountered an error."
			def errors
				diggit.sources.get_all(nil, {error: true}).each{|s| info s[:url] }
			end

			desc 'import [FILE]', "Import a list of sources from a file (one URL per line)."
			def import(urls_file)
				IO.readlines(urls_file).each{ |line| diggit.sources.add(line.strip) }
			end

			desc "add [URL*]", "Add the provided urls to the list of sources."
			def add(*urls)
				urls.each{ |u| diggit.sources.add(u) }
			end

			desc "rem [SOURCE_DEF*]", "Remove the sources correspondign to the provided source definitions (id or URL) from the list of sources."
			def rem(*sources_defs)
				sources_defs.each { |s| diggit.sources.rem(s) }
			end
		end

		class AddonsCli < Thor
			include Thor::Actions
			include Utils

			desc "add [ADDON*]", "Add the provided addons to the list of active addons."
			def add(*addons)
				addons.each do |a|
					if plugin_ok?(a, Addon)
						diggit.config.add_addon(a)
					else
						say_error("error loading addon #{a}")
					end
				end
			end

			desc "rem [ADDON*]", "Remove the provided addons from the list of active addons."
			def rem(*addons)
				addons.each{ |a| diggit.config.rem_addon(a) }
			end

		end

		class JoinsCli < Thor
			include Thor::Actions
			include Utils

			desc "add [JOIN*]", "Add the provided joins to the list of active joins."
			def add(*joins)
				joins.each do |j|
					if plugin_ok?(j, Join)
						diggit.config.add_join(j)
					else
						say_error("error loading join #{j}")
					end
				end
			end

			desc "rem [JOIN*]", "Remove the provided joins from the list of active joins."
			def rem(*joins)
				joins.each{ |j| diggit.config.rem_join(j) }
			end

		end

		class AnalysesCli < Thor
			include Thor::Actions
			include Utils

			desc "add [ANALYSIS*]", "Add the provided analyses to the list of active analyses."
			def add(*analyses)
				analyses.each do |a|
					if plugin_ok?(a, Analysis)
						diggit.config.add_analysis(a)
					else
						say_error("error loading analysis #{a}")
					end
				end
			end

			desc "rem [ANALYSIS*]", "Remove the provided analyses from the list of active analyses."
			def rem(*analyses)
				analyses.each{ |a| diggit.config.rem_analysis(a) }
			end

		end

		class PerformCli < Thor
			include Thor::Actions
			include Utils

			desc "clones [SOURCE_DEFS*]", "Clone the sources corresponding to the provided source definitions (id or URL). Clone all sources if no source definitions are provided."
			def clones(*source_defs)
				diggit.sources.get_all(source_defs, {state: :new}).each do |s|
					begin
						if File.exist?(s[:folder])
							Rugged::Repository::new(s[:folder])
						else
							Rugged::Repository::clone_at(s[:url], s[:folder])
						end
					rescue => e
						s[:log][:error] = dump_error(e)
						say_error("error cloning #{s[:url]}")
					else
						s[:log][:state] = :cloned
						s[:log][:error] = {}
						say_done("#{s[:url]} cloned")
					ensure
						diggit.sources.update(s)
					end
				end
			end

			desc "analyses [SOURCE_DEFS*]", "Perform the configured analyses to the sources corresponding to the provided source definitions (id or URL). Analyze all sources if no source definitions are provided."
			def analyses(*source_defs)
				addons = diggit.config.load_addons
				diggit.sources.get_all(source_defs, {state: :cloned}).each do |s|
					FileUtils.cd(s[:folder])
					globs = {}
					performed_analyses = []
					begin
						repo = Rugged::Repository.new('.')
						diggit.config.load_analyses(s[:url], repo, addons, globs).each do |a|
							performed_analyses << a.class.to_s
							a.run
						end
					rescue => e
						s[:log][:error] = dump_error(e)
						s[:log][:analyses] = performed_analyses[1..-2]
						say_error("error performing #{performed_analyses.last} on #{s[:url]}")
					else
						s[:log][:analyses] = performed_analyses
						s[:log][:state] = :finished
						s[:log][:error] = {}
						say_done("source #{s[:url]} analyzed")
					ensure
						FileUtils.cd(diggit.root)
						diggit.sources.update(s)
					end
				end
			end

			desc "joins", "Perform the configured joins."
			def joins
				addons = diggit.config.load_addons
				globs = {}
				diggit.config.load_joins(diggit.sources.get_all([], {state: :finished, error: false}), addons, globs).each{ |j| j.run }
				say_done("joins performed")
			end

		end

		class CleanCli < Thor
			include Thor::Actions
			include Utils

			desc "analyses", "Clean the configured analyzes on the provided source definitions (id or URL). Clean all sources if no source definitions are provided."
			def analyses(*source_defs)
				addons = diggit.config.load_addons
				diggit.sources.get_all(source_defs, {state: :finished}).each do |s|
					globs = {}
					diggit.config.load_analyses(s[:url], nil, addons, globs).each{ |a| a.clean}
					s[:log][:state] = :cloned
					s[:log][:analyses] = []
					s[:log][:error] = {}
					diggit.sources.update(s)
					say_done("cleaned analyses on #{s[:url]}")
				end
			end

			desc "joins", "Clean the configured joins."
			def joins
				addons = diggit.config.load_addons
				globs = {}
				diggit.config.load_joins(diggit.sources.get_all([], {state: :finished, error: false}), addons, globs).each{ |j| j.clean }
			end

		end

		class DiggitCli < Thor
			include Thor::Actions
			include Utils

			def initialize(*args)
				super
				cmd = args[2][:current_command].name
				unless 'init'.eql?(cmd) || 'help'.eql?(cmd)
					unless File.exist?(DIGGIT_RC)
						say_error("this is not a diggit directory")
					else
						diggit
					end
				end
			end

			desc "init", "Initialize the current folder as a diggit folder."
			def init
				FileUtils.touch(DIGGIT_SOURCES)
				Oj.to_file(DIGGIT_LOG, {})
				Oj.to_file(DIGGIT_RC, { addons: [], analyses: [], joins: [], options: {} })
				say_done("Diggit folder successfully initialized")
			end

			desc 'status', "Display the status of the current diggit folder."
			def status
				color = (diggit.sources.get_all(nil, {error: true}).size > 0 && :red) || :blue
				say_status('[sources]', "#{diggit.sources.get_all([], {state: :new}).size} new (#{diggit.sources.get_all([], {state: :new, error: true}).size} errors), #{diggit.sources.get_all([], {state: :cloned}).size} cloned (#{diggit.sources.get_all([], {state: :cloned, error: true}).size} errors), #{diggit.sources.get_all([], {state: :finished}).size} finished", color)
				say_status('[addons]', "#{diggit.config.addons.join(', ')}", :blue)
				say_status('[analyses]', "#{diggit.config.analyses.join(', ')}", :blue)
				say_status('[joins]', "#{diggit.config.joins.join(', ')}", :blue)
				say_status('[options]', "#{diggit.config.options}", :blue)
			end

			desc "sources SUBCOMMAND ...ARGS", "manage sources for the current diggit folder."
			subcommand "sources", SourcesCli

			desc "joins SUBCOMMAND ...ARGS", "manage joins for the current diggit folder."
			subcommand "joins", JoinsCli

			desc "analyses SUBCOMMAND ...ARGS", "manage analyses for the current diggit folder."
			subcommand "analyses", AnalysesCli

			desc "addons SUBCOMMAND ...ARGS", "manage addons for the current diggit folder."
			subcommand "addons", AddonsCli

			desc "perform SUBCOMMAND ...ARGS", "perform actions in the current diggit folder."
			subcommand "perform", PerformCli

			desc "clean SUBCOMMAND ...ARGS", "clean the current diggit folder."
			subcommand "clean", CleanCli

		end

	end

end