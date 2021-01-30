.EXPORT_ALL_VARIABLES:
.DEFAULT_GOAL := help
.PHONY: help test response

SHELL = bash

include .env



####
## Helpers
####

HEADER_CHAR=λ
HEADER_COLOR=\033[36m
TEXT_COLOR=\033[0m

define header
	@echo -e '$(HEADER_COLOR)${HEADER_CHAR}'  $(1) '$(TEXT_COLOR)'
endef



####
## Global variables
####

PROJECT_ROOT=$(shell pwd)
CLJ_CONFIG=${PROJECT_ROOT}



####
## ninja.platform
####

ARGS = $(filter-out $@,$(MAKECMDGOALS))
%:
	@:

help: ## Show help
	$(call header, "[${PROJECT_NAME}] Show help")
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "$(HEADER_COLOR)%-30s$(TEXT_COLOR) %s\n", $$1, $$2}' $(MAKEFILE_LIST)

deps: ## Check for deps updates
	$(call header, "[${PROJECT_NAME}] Check for deps updates")
	$(if $(strip $(ARGS)), cd $(ARGS) && clojure -M:project.default/deps:project.updates/deps:project.updates/run, clojure -M:project.default/deps:project.updates/deps:project.updates/run)

tree: ## Show deps tree
	$(call header, "[${PROJECT_NAME}] Show deps tree")
	$(if $(strip $(ARGS)), cd $(ARGS) && clojure -A:project.default/deps -Stree, clojure -A:project.default/deps -Stree)

clean: ## Clean
	$(call header, "[${PROJECT_NAME}] Clean")
	$(if $(strip $(ARGS)), @make clean-$(ARGS), @make clean-response)

repl: ## Run REPL
	$(call header, "[${PROJECT_NAME}] Run REPL")
	$(if $(strip $(ARGS)), @make repl-$(ARGS), $(error "Module name is not specified. Example, `$ make repl response`"))

lint: ## Run linter
	$(call header, "[${PROJECT_NAME}] Run linter")
	$(if $(strip $(ARGS)), @make lint-$(ARGS), @make lint-response)

format: ## Run formatter
	$(call header, "[${PROJECT_NAME}] Run formatter")
	$(if $(strip $(ARGS)), @make format-$(ARGS), @make format-response)

test: ## Run tests
	$(call header, "[${PROJECT_NAME}] Run tests")
	$(if $(strip $(ARGS)), @make test-$(ARGS), @make test-response)

pom: ## Generate pom
	$(call header, "[${PROJECT_NAME}] Generate pom")
	$(if $(strip $(ARGS)), @make pom-$(ARGS), @make pom-response)

jar: ## Build jar
	$(call header, "[${PROJECT_NAME}] Build jar")
	$(if $(strip $(ARGS)), @make jar-$(ARGS), @make jar-response)

install: ## Install jar
	$(call header, "[${PROJECT_NAME}] Install jar")
	$(if $(strip $(ARGS)), @make install-$(ARGS), @make install-response)

deploy: ## Deploy jar
	$(call header, "[${PROJECT_NAME}] Deploy jar")
	$(if $(strip $(ARGS)), @make deploy-$(ARGS), @make deploy-response)



####
## ninja.platform/response
####

clean-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Clean")
	cd response && rm -rf target

repl-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Run REPL")
	cd response && clojure -M:project.default/deps:project.bench/deps::project.bench/opts:project.dev/deps:module.dev/paths:project.test.clj/deps:project.test.cljs/deps:module.test/paths --main nrepl.cmdline --middleware '[cider.piggieback/wrap-cljs-repl]'

lint-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Run linter")
	cd response && clj-kondo --lint src/dev/clojure:src/main/clojure:src/test/clojure
	cljstyle check response/src

format-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Run formatter")
	cljstyle fix response/src

test-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Run tests")
	cd response && clojure -M:project.default/deps:project.dev/deps:module.dev/paths:project.test.clj/deps:module.test/paths --main kaocha.runner
	cd response && clojure -M:project.default/deps:project.dev/deps:module.dev/paths:project.test.cljs/deps:module.test/paths --main cljs-test-runner.main --dir src/test/clojure --out target/test-cljs

pom-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Generate pom")
	cd response && clojure -X:project.default/deps:deps mvn-pom

jar-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Build jar")
	cd response && clojure -X:project.default/deps:project.build/deps jar :group-id ${MODULE_NINJA_RESPONSE_GROUP_ID} :artifact-id ${MODULE_NINJA_RESPONSE_ARTIFACT_ID} :version '"${MODULE_NINJA_RESPONSE_VERSION}"' :sync-pom true :jar ${MODULE_NINJA_RESPONSE_JAR}

install-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Install jar")
	cd response && clojure -X:project.default/deps:project.deploy/deps :installer :local :sign-releases? true :artifact '"${MODULE_NINJA_RESPONSE_JAR}"'

deploy-response:
	$(call header, "[${MODULE_NINJA_RESPONSE}] Deploy jar")
	cd response && clojure -X:project.default/deps:project.deploy/deps :installer :remote :sign-releases? true :artifact '"${MODULE_NINJA_RESPONSE_JAR}"'
